package com.remindme.digest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
import com.remindme.evals.EvalsService;

@Service
public class DigestService {

    private static final Logger log = LoggerFactory.getLogger(DigestService.class);

    private static final Pattern ISO_UTC = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");
    private static final String CACHE_NAME = "digest";

    private final DigestGenerator generator;
    private final Clock clock;
    private final FeedCacheStore cacheStore;
    private final EvalsService evalsService;

    private final ReentrantLock generationLock = new ReentrantLock();
    private volatile Cached cached;

    private record Cached(LocalDate date, DailyDigest digest) {
    }

    // on-disk shape: the ISO date the digest was generated for, plus the digest itself
    record Snapshot(String date, DailyDigest digest) {
    }

    @Autowired
    public DigestService(DigestGenerator generator, FeedCacheStore cacheStore, EvalsService evalsService) {
        this(generator, Clock.system(ZoneOffset.UTC), cacheStore, evalsService);
    }

    DigestService(DigestGenerator generator, Clock clock) {
        this(generator, clock, FeedCacheStore.disabled(), EvalsService.disabled());
    }

    DigestService(DigestGenerator generator, Clock clock, FeedCacheStore cacheStore) {
        this(generator, clock, cacheStore, EvalsService.disabled());
    }

    DigestService(DigestGenerator generator, Clock clock, FeedCacheStore cacheStore, EvalsService evalsService) {
        this.generator = generator;
        this.clock = clock;
        this.cacheStore = cacheStore;
        this.evalsService = evalsService;
        this.cached = cacheStore.load(CACHE_NAME, Snapshot.class)
                .map(s -> new Cached(LocalDate.parse(s.date()), s.digest()))
                .orElse(null);
        if (cached != null) {
            log.info("Loaded persisted digest for {}", cached.date());
        }
    }

    public DailyDigest today() {
        evalsService.recordRequest(EvalsService.DIGEST);
        LocalDate today = LocalDate.now(clock);

        DailyDigest hit = cachedFor(today);
        if (hit != null) {
            return upcomingOnly(hit);
        }

        generationLock.lock();
        try {
            hit = cachedFor(today);
            if (hit != null) {
                return upcomingOnly(hit);
            }

            DailyDigest fresh = timestamped(generator.generate(clock.instant()));

            if (fresh == null || fresh.isEmpty()) {
                log.warn("Digest generation for {} produced nothing, not caching", today);
                return DailyDigest.empty(today.toString());
            }

            cached = new Cached(today, fresh);
            cacheStore.save(CACHE_NAME, new Snapshot(today.toString(), fresh));
            log.info("Digest generated for {} with {} events", today, fresh.events().size());
            return upcomingOnly(fresh);
        } catch (Exception e) {
            log.error("Digest generation failed for {}", today, e);
            return DailyDigest.empty(today.toString());
        } finally {
            generationLock.unlock();
        }
    }

    private DailyDigest cachedFor(LocalDate date) {
        Cached snapshot = cached;
        return snapshot != null && snapshot.date().equals(date) ? snapshot.digest() : null;
    }

    // drops anything without a clean UTC startsAt (every digest event needs a real start time)
    // and clamps ratings into 3..5 so the client's filter always has a valid bucket - runs once,
    // at generation
    private DailyDigest timestamped(DailyDigest digest) {
        if (digest == null || digest.events() == null) {
            return digest;
        }

        List<DigestEvent> kept = digest.events().stream()
                .filter(event -> event.startsAt() != null && ISO_UTC.matcher(event.startsAt()).matches())
                .map(DigestService::withNormalRating)
                .collect(Collectors.toList());

        return new DailyDigest(digest.date(), kept);
    }

    private static DigestEvent withNormalRating(DigestEvent e) {
        int normal = e.rating() == null ? 3 : Math.max(3, Math.min(5, e.rating()));
        if (e.rating() != null && e.rating() == normal) {
            return e;
        }
        return new DigestEvent(e.title(), e.summary(), e.category(), normal, e.startsAt(), e.howToWatch(),
                e.recommendedDescription(), e.recommendedReminderAt(), e.source());
    }

    // drops events that already started, so a late request naturally surfaces only what's still
    // ahead - runs on every read
    private DailyDigest upcomingOnly(DailyDigest digest) {
        Instant now = clock.instant();

        List<DigestEvent> upcoming = digest.events().stream()
                .filter(event -> {
                    try {
                        return Instant.parse(event.startsAt()).isAfter(now);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());

        return new DailyDigest(digest.date(), upcoming);
    }
}
