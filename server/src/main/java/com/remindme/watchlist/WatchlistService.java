package com.remindme.watchlist;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.remindme.config.FeedCacheStore;

@Service
public class WatchlistService {

    private static final Logger log = LoggerFactory.getLogger(WatchlistService.class);

    private static final Pattern ISO_UTC = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");
    private static final String CACHE_NAME = "watchlist";

    // starts 2 days out so it never overlaps the daily digest
    private static final Period WINDOW_START_OFFSET = Period.ofDays(2);
    private static final Period WINDOW_LENGTH = Period.ofDays(14);
    // after this the user can hit refresh; a normal page load keeps the cached copy either way
    private static final Period REFRESH_INTERVAL = Period.ofDays(3);

    private final WatchlistGenerator generator;
    private final Clock clock;
    private final FeedCacheStore cacheStore;

    private final ReentrantLock generationLock = new ReentrantLock();
    private volatile Cached cached;

    private record Cached(LocalDate generatedAt, WatchlistWindow window) {
    }

    // what gets persisted: when it was generated, plus the window itself
    record Snapshot(String generatedAt, WatchlistWindow window) {
    }

    @Autowired
    public WatchlistService(WatchlistGenerator generator, FeedCacheStore cacheStore) {
        this(generator, Clock.system(ZoneOffset.UTC), cacheStore);
    }

    WatchlistService(WatchlistGenerator generator, Clock clock) {
        this(generator, clock, FeedCacheStore.disabled());
    }

    WatchlistService(WatchlistGenerator generator, Clock clock, FeedCacheStore cacheStore) {
        this.generator = generator;
        this.clock = clock;
        this.cacheStore = cacheStore;
        this.cached = cacheStore.load(CACHE_NAME, Snapshot.class)
                .map(s -> new Cached(LocalDate.parse(s.generatedAt()), s.window()))
                .orElse(null);
        if (cached != null) {
            log.info("Loaded persisted watchlist generated {}", cached.generatedAt());
        }
    }

    public WatchlistWindow current() {
        return current(false);
    }

    // force = true means the user hit refresh, so ignore the cooldown if we're past it
    public WatchlistWindow current(boolean force) {
        LocalDate today = LocalDate.now(clock);

        Cached hit = cached;
        if (withinInterval(hit, today)) {
            return present(hit);
        }

        generationLock.lock();
        try {
            hit = cached;
            if (withinInterval(hit, today)) {
                return present(hit);
            }

            // past the cooldown but still useful and nobody asked for a refresh - keep serving it
            if (hit != null && !completelyOutdated(hit, today) && !force) {
                return present(hit);
            }

            LocalDate windowStart = today.plus(WINDOW_START_OFFSET);
            LocalDate windowEnd = windowStart.plus(WINDOW_LENGTH);
            try {
                WatchlistWindow fresh = sanitize(generator.generate(windowStart, windowEnd));

                if (fresh == null || fresh.isEmpty()) {
                    log.warn("Watchlist generation for {}..{} produced nothing", windowStart, windowEnd);
                    return hit != null ? present(hit)
                            : WatchlistWindow.empty(windowStart.toString(), windowEnd.toString());
                }

                Cached entry = new Cached(today, fresh);
                cached = entry;
                cacheStore.save(CACHE_NAME, new Snapshot(today.toString(), fresh));
                log.info("Watchlist generated for {}..{} with {} events", windowStart, windowEnd,
                        fresh.events().size());
                return present(entry);
            } catch (Exception e) {
                log.error("Watchlist generation failed for {}..{}", windowStart, windowEnd, e);
                return hit != null ? present(hit)
                        : WatchlistWindow.empty(windowStart.toString(), windowEnd.toString());
            }
        } finally {
            generationLock.unlock();
        }
    }

    private boolean withinInterval(Cached hit, LocalDate today) {
        return hit != null && today.isBefore(hit.generatedAt().plus(REFRESH_INTERVAL));
    }

    // true once the whole window has passed and everything in it is stale either way
    private boolean completelyOutdated(Cached hit, LocalDate today) {
        try {
            return !today.isBefore(LocalDate.parse(hit.window().windowEnd()));
        } catch (Exception e) {
            return true;
        }
    }

    private WatchlistWindow present(Cached hit) {
        return applyPastFilter(hit.window())
                .withNextRefreshAt(hit.generatedAt().plus(REFRESH_INTERVAL).toString());
    }

    private WatchlistWindow applyPastFilter(WatchlistWindow window) {
        Instant now = clock.instant();

        List<WatchlistEvent> upcoming = window.events().stream()
                .filter(event -> !hasPassed(event, now))
                .collect(Collectors.toList());

        return new WatchlistWindow(window.windowStart(), window.windowEnd(), upcoming, window.nextRefreshAt());
    }

    private boolean hasPassed(WatchlistEvent event, Instant now) {
        if (event.startsAt() == null) {
            return false;
        }

        try {
            return Instant.parse(event.startsAt()).isBefore(now);
        } catch (Exception e) {
            return false;
        }
    }

    // clears out any startsAt the model returned that isn't a clean UTC timestamp
    private WatchlistWindow sanitize(WatchlistWindow window) {
        if (window == null || window.events() == null) {
            return window;
        }

        List<WatchlistEvent> cleaned = window.events().stream()
                .map(event -> {
                    String startsAt = event.startsAt();
                    if (startsAt == null || ISO_UTC.matcher(startsAt).matches()) {
                        return event;
                    }
                    return new WatchlistEvent(event.title(), event.summary(), event.category(), event.rating(),
                            null, event.expectedWindow(), event.source(), event.recommendedDescription(),
                            event.recommendedReminderAt(), event.howToWatch());
                })
                .collect(Collectors.toList());

        return new WatchlistWindow(window.windowStart(), window.windowEnd(), cleaned, window.nextRefreshAt());
    }
}
