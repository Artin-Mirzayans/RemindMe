package com.remindme.watchlist;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.remindme.config.FeedCacheStore;

class WatchlistServiceTest {

    private static final Instant DAY_ZERO = Instant.parse("2030-01-01T08:00:00Z");

    private WatchlistWindow windowFor(LocalDate start, LocalDate end) {
        return new WatchlistWindow(start.toString(), end.toString(),
                List.of(event("Something big", null)), null);
    }

    private WatchlistEvent event(String title, String startsAt) {
        return new WatchlistEvent(title, "It clears the bar.", "Sports", 5, startsAt,
                startsAt == null ? "TBD" : null, null, "Watch this",
                startsAt == null ? null : "2030-01-14T00:00:00Z", null);
    }

    // a clock whose instant can be advanced mid-test, to exercise the weekly refresh rule
    private static class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone;

        MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        void advanceTo(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    @Test
    @DisplayName("generates the window once and serves the cached copy afterwards")
    void generatesOnceWithinWindow() {
        AtomicInteger calls = new AtomicInteger();
        WatchlistService service = new WatchlistService((start, end) -> {
            calls.incrementAndGet();
            return windowFor(start, end);
        }, Clock.fixed(DAY_ZERO, ZoneOffset.UTC));

        service.current();
        service.current();
        service.current();

        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("does not regenerate within the 3-day refresh interval")
    void doesNotRegenerateWithinTheInterval() {
        AtomicInteger calls = new AtomicInteger();
        MutableClock clock = new MutableClock(DAY_ZERO, ZoneOffset.UTC);
        WatchlistService service = new WatchlistService((start, end) -> {
            calls.incrementAndGet();
            return windowFor(start, end);
        }, clock);

        service.current();
        clock.advanceTo(DAY_ZERO.plus(2, java.time.temporal.ChronoUnit.DAYS));
        service.current();

        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("past the interval, a normal read still serves the cached copy (user must proc a refresh)")
    void doesNotAutoRegenerateJustPastTheInterval() {
        AtomicInteger calls = new AtomicInteger();
        MutableClock clock = new MutableClock(DAY_ZERO, ZoneOffset.UTC);
        WatchlistService service = new WatchlistService((start, end) -> {
            calls.incrementAndGet();
            return windowFor(start, end);
        }, clock);

        service.current();
        clock.advanceTo(DAY_ZERO.plus(5, java.time.temporal.ChronoUnit.DAYS));
        service.current();
        service.current();

        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("past the interval, an explicit refresh regenerates")
    void regeneratesWhenForcedPastTheInterval() {
        AtomicInteger calls = new AtomicInteger();
        MutableClock clock = new MutableClock(DAY_ZERO, ZoneOffset.UTC);
        WatchlistService service = new WatchlistService((start, end) -> {
            calls.incrementAndGet();
            return windowFor(start, end);
        }, clock);

        service.current();
        clock.advanceTo(DAY_ZERO.plus(5, java.time.temporal.ChronoUnit.DAYS));
        service.current(true);

        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("within the interval, an explicit refresh is ignored - the cooldown holds")
    void forcedRefreshWithinTheIntervalIsIgnored() {
        AtomicInteger calls = new AtomicInteger();
        MutableClock clock = new MutableClock(DAY_ZERO, ZoneOffset.UTC);
        WatchlistService service = new WatchlistService((start, end) -> {
            calls.incrementAndGet();
            return windowFor(start, end);
        }, clock);

        service.current();
        clock.advanceTo(DAY_ZERO.plus(1, java.time.temporal.ChronoUnit.DAYS));
        service.current(true);

        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("auto-regenerates once the whole coverage window has elapsed, no refresh needed")
    void autoRegeneratesWhenWindowHasFullyElapsed() {
        AtomicInteger calls = new AtomicInteger();
        MutableClock clock = new MutableClock(DAY_ZERO, ZoneOffset.UTC);
        WatchlistService service = new WatchlistService((start, end) -> {
            calls.incrementAndGet();
            return windowFor(start, end);
        }, clock);

        service.current(); // window is 2030-01-03..2030-01-17
        clock.advanceTo(Instant.parse("2030-01-20T08:00:00Z"));
        service.current();

        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("reports when the feed may next be refreshed - 3 days out from generation")
    void reportsNextRefreshDate() {
        WatchlistService service = new WatchlistService((start, end) -> windowFor(start, end),
                Clock.fixed(DAY_ZERO, ZoneOffset.UTC));

        assertThat(service.current().nextRefreshAt()).isEqualTo("2030-01-04");
    }

    @Test
    @DisplayName("serves a watchlist from the persisted store on startup instead of regenerating it")
    void loadsPersistedWatchlistOnStartup() {
        FeedCacheStore store = FeedCacheStore.inMemory();
        store.save("watchlist", new WatchlistService.Snapshot("2030-01-01",
                new WatchlistWindow("2030-01-03", "2030-01-17",
                        List.of(event("From disk", "2030-01-15T00:00:00Z")), null)));

        AtomicInteger calls = new AtomicInteger();
        WatchlistService service = new WatchlistService((start, end) -> {
            calls.incrementAndGet();
            return windowFor(start, end);
        }, Clock.fixed(DAY_ZERO, ZoneOffset.UTC), store);

        List<String> titles = service.current().events().stream().map(WatchlistEvent::title).toList();

        assertThat(titles).containsExactly("From disk");
        assertThat(calls.get()).isZero();
    }

    @Test
    @DisplayName("requests a 14-day window starting 2 days out, clear of the daily digest's today/tomorrow")
    void requestsWindowClearOfTheDigest() {
        WatchlistService service = new WatchlistService((start, end) -> {
            assertThat(start).isEqualTo(LocalDate.parse("2030-01-03"));
            assertThat(end).isEqualTo(LocalDate.parse("2030-01-17"));
            return windowFor(start, end);
        }, Clock.fixed(DAY_ZERO, ZoneOffset.UTC));

        service.current();
    }

    @Test
    @DisplayName("returns an empty window rather than propagating a generator failure")
    void swallowsGeneratorFailure() {
        WatchlistService service = new WatchlistService((start, end) -> {
            throw new IllegalStateException("the model is having a day");
        }, Clock.fixed(DAY_ZERO, ZoneOffset.UTC));

        WatchlistWindow window = service.current();

        assertThat(window.isEmpty()).isTrue();
        assertThat(window.windowStart()).isEqualTo("2030-01-03");
    }

    @Test
    @DisplayName("retries after a failure instead of caching the empty result")
    void doesNotCacheFailures() {
        AtomicInteger calls = new AtomicInteger();
        WatchlistService service = new WatchlistService((start, end) -> {
            if (calls.incrementAndGet() == 1) {
                throw new IllegalStateException("transient");
            }
            return windowFor(start, end);
        }, Clock.fixed(DAY_ZERO, ZoneOffset.UTC));

        assertThat(service.current().isEmpty()).isTrue();
        assertThat(service.current().isEmpty()).isFalse();
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("does not cache an empty window")
    void doesNotCacheEmptyWindows() {
        AtomicInteger calls = new AtomicInteger();
        WatchlistService service = new WatchlistService((start, end) -> {
            calls.incrementAndGet();
            return WatchlistWindow.empty(start.toString(), end.toString());
        }, Clock.fixed(DAY_ZERO, ZoneOffset.UTC));

        service.current();
        service.current();

        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("generates once even when many requests arrive at the same moment")
    void generatesOnceUnderConcurrentLoad() throws Exception {
        int threads = 16;
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);

        WatchlistService service = new WatchlistService((windowStart, windowEnd) -> {
            calls.incrementAndGet();
            return windowFor(windowStart, windowEnd);
        }, Clock.fixed(DAY_ZERO, ZoneOffset.UTC));

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    start.await();
                    return service.current();
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

    @Test
    @DisplayName("nulls out a malformed startsAt rather than passing it through")
    void sanitizesMalformedStartsAt() {
        WatchlistWindow window = new WatchlistWindow("2030-01-03", "2030-01-17", List.of(
                new WatchlistEvent("Bad time", "s", "Sports", 5, "2Championship026-09-10T00:00:00Z", null, null,
                        "d", "2030-01-14T00:00:00Z", null),
                new WatchlistEvent("Good time", "s", "Sports", 5, "2030-01-15T00:00:00Z", null, null, "d",
                        "2030-01-14T00:00:00Z", null)),
                null);

        WatchlistService service = new WatchlistService((start, end) -> window,
                Clock.fixed(DAY_ZERO, ZoneOffset.UTC));

        List<WatchlistEvent> events = service.current().events();

        assertThat(events.get(0).startsAt()).isNull();
        assertThat(events.get(1).startsAt()).isEqualTo("2030-01-15T00:00:00Z");
    }

    @Test
    @DisplayName("filters out events whose confirmed start time has already passed")
    void filtersPastEvents() {
        WatchlistWindow window = new WatchlistWindow(
                LocalDate.parse("2030-01-03").toString(),
                LocalDate.parse("2030-01-17").toString(),
                List.of(
                        event("Already happened", "2029-12-31T00:00:00Z"),
                        event("Still upcoming", "2030-01-15T00:00:00Z"),
                        event("Date TBD", null)),
                null);

        WatchlistService service = new WatchlistService((start, end) -> window, Clock.fixed(DAY_ZERO, ZoneOffset.UTC));

        List<String> titles = service.current().events().stream().map(WatchlistEvent::title).toList();

        assertThat(titles).containsExactlyInAnyOrder("Still upcoming", "Date TBD");
    }
}
