package com.remindme.local;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.remindme.config.FeedCacheStore;

// keyed by location instead of one global cache like digest/watchlist, but same rolling window
// and refresh rules, and drops already-started events on every read
@Service
public class LocalEventsService {

    private static final Logger log = LoggerFactory.getLogger(LocalEventsService.class);

    private static final String CACHE_NAME = "local-events";
    private static final Period WINDOW_START_OFFSET = Period.ofDays(2);
    private static final Period WINDOW_LENGTH = Period.ofDays(14);
    // after this the user can refresh; a normal read keeps serving the cache either way
    private static final Period REFRESH_INTERVAL = Period.ofDays(3);
    private static final Pattern ISO_UTC = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");

    private final LocalEventsGenerator generator;
    private final Clock clock;
    private final FeedCacheStore cacheStore;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    private record Cached(LocalDate generatedAt, LocalEventsWindow window) {
    }

    record Snapshot(String generatedAt, LocalEventsWindow window) {
    }

    @Autowired
    public LocalEventsService(LocalEventsGenerator generator, FeedCacheStore cacheStore) {
        this(generator, Clock.system(ZoneOffset.UTC), cacheStore);
    }

    LocalEventsService(LocalEventsGenerator generator, Clock clock) {
        this(generator, clock, FeedCacheStore.disabled());
    }

    LocalEventsService(LocalEventsGenerator generator, Clock clock, FeedCacheStore cacheStore) {
        this.generator = generator;
        this.clock = clock;
        this.cacheStore = cacheStore;
        cacheStore.load(CACHE_NAME, new TypeReference<Map<String, Snapshot>>() {
        }).ifPresent(saved -> {
            saved.forEach((key, snap) -> cache.put(key,
                    new Cached(LocalDate.parse(snap.generatedAt()), snap.window())));
            if (!cache.isEmpty()) {
                log.info("Loaded {} persisted local-events location(s)", cache.size());
            }
        });
    }

    public LocalEventsWindow forLocation(GeoLocation location) {
        return forLocation(location, false);
    }

    public LocalEventsWindow forLocation(GeoLocation location, boolean force) {
        LocalDate today = LocalDate.now(clock);
        String key = location.label() == null ? "unknown" : location.label().toLowerCase();

        Cached hit = cache.get(key);
        if (withinInterval(hit, today)) {
            return present(hit);
        }

        LocalDate windowStart = today.plus(WINDOW_START_OFFSET);
        LocalDate windowEnd = windowStart.plus(WINDOW_LENGTH);

        // one generation per location at a time, so a burst of clicks on a new city doesn't
        // fan out into a burst of paid Claude calls
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            hit = cache.get(key);
            if (withinInterval(hit, today)) {
                return present(hit);
            }

            // past the cooldown but still useful and nobody asked for a refresh - keep serving it
            if (hit != null && !completelyOutdated(hit, today) && !force) {
                return present(hit);
            }

            try {
                LocalEventsWindow fresh = sanitize(generator.generate(location, windowStart, windowEnd));
                if (fresh == null || fresh.isEmpty()) {
                    return hit != null ? present(hit)
                            : LocalEventsWindow.empty(location.label(), windowStart.toString(), windowEnd.toString());
                }
                Cached entry = new Cached(today, fresh);
                cache.put(key, entry);
                persist();
                log.info("Local events cached for {} with {} events", key, fresh.events().size());
                return present(entry);
            } catch (Exception e) {
                log.error("Local events generation failed for {}", key, e);
                return hit != null ? present(hit)
                        : LocalEventsWindow.empty(location.label(), windowStart.toString(), windowEnd.toString());
            }
        } finally {
            lock.unlock();
        }
    }

    private boolean withinInterval(Cached hit, LocalDate today) {
        return hit != null && today.isBefore(hit.generatedAt().plus(REFRESH_INTERVAL));
    }

    private boolean completelyOutdated(Cached hit, LocalDate today) {
        try {
            return !today.isBefore(LocalDate.parse(hit.window().windowEnd()));
        } catch (Exception e) {
            return true;
        }
    }

    private void persist() {
        Map<String, Snapshot> snapshot = new java.util.HashMap<>();
        cache.forEach((key, c) -> snapshot.put(key, new Snapshot(c.generatedAt().toString(), c.window())));
        cacheStore.save(CACHE_NAME, snapshot);
    }

    private LocalEventsWindow present(Cached hit) {
        return upcomingOnly(hit.window())
                .withNextRefreshAt(hit.generatedAt().plus(REFRESH_INTERVAL).toString());
    }

    private LocalEventsWindow sanitize(LocalEventsWindow window) {
        if (window == null || window.events() == null) {
            return window;
        }
        List<LocalEvent> kept = window.events().stream()
                .filter(e -> e.startsAt() != null && ISO_UTC.matcher(e.startsAt()).matches())
                .collect(Collectors.toList());
        return new LocalEventsWindow(window.location(), window.windowStart(), window.windowEnd(), kept,
                window.nextRefreshAt());
    }

    private LocalEventsWindow upcomingOnly(LocalEventsWindow window) {
        Instant now = clock.instant();
        List<LocalEvent> upcoming = window.events().stream()
                .filter(e -> {
                    try {
                        return Instant.parse(e.startsAt()).isAfter(now);
                    } catch (Exception ex) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
        return new LocalEventsWindow(window.location(), window.windowStart(), window.windowEnd(), upcoming,
                window.nextRefreshAt());
    }
}
