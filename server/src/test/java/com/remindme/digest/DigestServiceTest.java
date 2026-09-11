package com.remindme.digest;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.remindme.config.FeedCacheStore;

class DigestServiceTest {

    private static final Instant MORNING = Instant.parse("2030-01-01T08:00:00Z");

    private DailyDigest digestFor(Instant now) {
        String startsAt = now.plus(6, ChronoUnit.HOURS).toString().replace(".000Z", "Z");
        return new DailyDigest("2030-01-01", List.of(new DigestEvent("Something to watch", "It clears the bar.",
                "Soccer", 4, startsAt, "ESPN", "Something to watch", null, null)));
    }

    private DigestEvent event(String title, String startsAt) {
        return event(title, startsAt, 4);
    }

    private DigestEvent event(String title, String startsAt, Integer rating) {
        return new DigestEvent(title, "summary", "Soccer", rating, startsAt, "ESPN", title, null, null);
    }

    private Clock fixedAt(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    @Test
    @DisplayName("generates the digest once and serves the cached copy afterwards")
    void generatesOncePerDay() {
        AtomicInteger calls = new AtomicInteger();
        DigestService service = new DigestService(now -> {
            calls.incrementAndGet();
            return digestFor(now);
        }, fixedAt(MORNING));

        service.today();
        service.today();
        service.today();

        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("regenerates once the date rolls over")
    void regeneratesOnNewDay() {
        AtomicInteger calls = new AtomicInteger();
        DigestGenerator generator = now -> {
            calls.incrementAndGet();
            return digestFor(now);
        };

        new DigestService(generator, fixedAt(MORNING)).today();
        new DigestService(generator, fixedAt(MORNING.plus(1, ChronoUnit.DAYS))).today();

        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("returns an empty digest rather than propagating a generator failure")
    void swallowsGeneratorFailure() {
        DigestService service = new DigestService(now -> {
            throw new IllegalStateException("the model is having a day");
        }, fixedAt(MORNING));

        DailyDigest digest = service.today();

        assertThat(digest.isEmpty()).isTrue();
        assertThat(digest.date()).isEqualTo("2030-01-01");
    }

    @Test
    @DisplayName("retries after a failure instead of caching the empty result")
    void doesNotCacheFailures() {
        AtomicInteger calls = new AtomicInteger();
        DigestService service = new DigestService(now -> {
            if (calls.incrementAndGet() == 1) {
                throw new IllegalStateException("transient");
            }
            return digestFor(now);
        }, fixedAt(MORNING));

        assertThat(service.today().isEmpty()).isTrue();
        assertThat(service.today().isEmpty()).isFalse();
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("drops events without a valid, future timestamp")
    void dropsEventsWithoutAValidTimestamp() {
        DigestService service = new DigestService(now -> new DailyDigest("2030-01-01", List.of(
                event("Malformed", "2Championship026-09-10T00:00:00Z"),
                event("Literal null text", ": null,"),
                event("No time", null),
                event("Already started", "2030-01-01T06:00:00Z"),
                event("Still ahead", "2030-01-01T18:00:00Z"))),
                fixedAt(MORNING));

        List<String> titles = service.today().events().stream().map(DigestEvent::title).toList();

        assertThat(titles).containsExactly("Still ahead");
    }

    @Test
    @DisplayName("pins every rating into the 3..5 range the client filter expects")
    void normalizesRatings() {
        DigestService service = new DigestService(now -> new DailyDigest("2030-01-01", List.of(
                event("No rating", "2030-01-01T18:00:00Z", null),
                event("Too low", "2030-01-01T19:00:00Z", 1),
                event("Too high", "2030-01-01T20:00:00Z", 9),
                event("In range", "2030-01-01T21:00:00Z", 4))),
                fixedAt(MORNING));

        List<Integer> ratings = service.today().events().stream().map(DigestEvent::rating).toList();

        assertThat(ratings).containsExactly(3, 3, 5, 4);
    }

    @Test
    @DisplayName("late in the day, only events still ahead are served from the same cache")
    void filtersPastEventsPerRequestTime() {
        DailyDigest generated = new DailyDigest("2030-01-01", List.of(
                event("Noon match", "2030-01-01T12:00:00Z"),
                event("Late match", "2030-01-01T23:00:00Z")));

        DigestService morning = new DigestService(now -> generated, fixedAt(MORNING));
        assertThat(morning.today().events()).hasSize(2);

        DigestService evening = new DigestService(now -> generated,
                fixedAt(Instant.parse("2030-01-01T20:00:00Z")));
        List<String> eveningTitles = evening.today().events().stream().map(DigestEvent::title).toList();
        assertThat(eveningTitles).containsExactly("Late match");
    }

    @Test
    @DisplayName("does not cache an empty digest")
    void doesNotCacheEmptyDigests() {
        AtomicInteger calls = new AtomicInteger();
        DigestService service = new DigestService(now -> {
            calls.incrementAndGet();
            return DailyDigest.empty("2030-01-01");
        }, fixedAt(MORNING));

        service.today();
        service.today();

        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("serves a digest from the persisted store on startup instead of regenerating it")
    void loadsPersistedDigestOnStartup() {
        FeedCacheStore store = FeedCacheStore.inMemory();
        store.save("digest", new DigestService.Snapshot("2030-01-01",
                new DailyDigest("2030-01-01", List.of(event("From disk", "2030-01-01T18:00:00Z")))));

        AtomicInteger calls = new AtomicInteger();
        DigestService service = new DigestService(now -> {
            calls.incrementAndGet();
            return digestFor(now);
        }, fixedAt(MORNING), store);

        List<String> titles = service.today().events().stream().map(DigestEvent::title).toList();

        assertThat(titles).containsExactly("From disk");
        assertThat(calls.get()).isZero();
    }

    @Test
    @DisplayName("a stale persisted digest from a previous day is ignored")
    void ignoresPersistedDigestFromAnotherDay() {
        FeedCacheStore store = FeedCacheStore.inMemory();
        store.save("digest", new DigestService.Snapshot("2029-12-31",
                new DailyDigest("2029-12-31", List.of(event("Yesterday", "2029-12-31T18:00:00Z")))));

        AtomicInteger calls = new AtomicInteger();
        DigestService service = new DigestService(now -> {
            calls.incrementAndGet();
            return digestFor(now);
        }, fixedAt(MORNING), store);

        service.today();

        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("generates once even when many requests arrive at the same moment")
    void generatesOnceUnderConcurrentLoad() throws Exception {
        int threads = 16;
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);

        DigestService service = new DigestService(now -> {
            calls.incrementAndGet();
            return digestFor(now);
        }, fixedAt(MORNING));

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    start.await();
                    return service.today();
                });
            }
            start.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        assertThat(calls.get()).isEqualTo(1);
    }
}
