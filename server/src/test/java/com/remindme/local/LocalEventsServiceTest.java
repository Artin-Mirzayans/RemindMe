package com.remindme.local;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LocalEventsServiceTest {

    private static final Instant DAY_ZERO = Instant.parse("2030-01-01T08:00:00Z");
    private final GeoLocation la = new GeoLocation("Los Angeles", "California", "United States", 34.0, -118.2, null);
    private final GeoLocation chicago = new GeoLocation("Chicago", "Illinois", "United States", 41.8, -87.6, null);

    private static class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advanceTo(Instant instant) {
            this.instant = instant;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    private LocalEvent event(String title, String startsAt) {
        return new LocalEvent(title, "s", "Music", 5, startsAt, "Venue", "$40+", title, "2030-01-05T00:00:00Z",
                "https://x");
    }

    private LocalEventsWindow windowWith(LocalEvent... events) {
        return new LocalEventsWindow("Los Angeles, California", "2030-01-03", "2030-01-17", List.of(events), null);
    }

    @Test
    @DisplayName("caches per location and reuses within the 3-day interval")
    void cachesPerLocationWithinTheInterval() {
        AtomicInteger calls = new AtomicInteger();
        MutableClock clock = new MutableClock(DAY_ZERO);
        LocalEventsService service = new LocalEventsService((loc, s, e) -> {
            calls.incrementAndGet();
            return windowWith(event("Show", "2030-01-10T00:00:00Z"));
        }, clock);

        service.forLocation(la);
        service.forLocation(la);
        clock.advanceTo(DAY_ZERO.plus(2, ChronoUnit.DAYS));
        service.forLocation(la);

        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("keeps a separate cache per location")
    void separatesByLocation() {
        AtomicInteger calls = new AtomicInteger();
        LocalEventsService service = new LocalEventsService((loc, s, e) -> {
            calls.incrementAndGet();
            return windowWith(event("Show", "2030-01-10T00:00:00Z"));
        }, Clock.fixed(DAY_ZERO, ZoneOffset.UTC));

        service.forLocation(la);
        service.forLocation(chicago);
        service.forLocation(la);

        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("past the interval, a normal read serves the cached copy but an explicit refresh regenerates")
    void refreshIsUserDrivenPastTheInterval() {
        AtomicInteger calls = new AtomicInteger();
        MutableClock clock = new MutableClock(DAY_ZERO);
        LocalEventsService service = new LocalEventsService((loc, s, e) -> {
            calls.incrementAndGet();
            return windowWith(event("Show", "2030-01-10T00:00:00Z"));
        }, clock);

        service.forLocation(la);
        clock.advanceTo(DAY_ZERO.plus(5, ChronoUnit.DAYS));
        service.forLocation(la);            // normal read - still cached
        assertThat(calls.get()).isEqualTo(1);

        service.forLocation(la, true);      // user hit Refresh
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("auto-regenerates once the whole coverage window has elapsed")
    void autoRegeneratesWhenWindowHasFullyElapsed() {
        AtomicInteger calls = new AtomicInteger();
        MutableClock clock = new MutableClock(DAY_ZERO);
        LocalEventsService service = new LocalEventsService((loc, s, e) -> {
            calls.incrementAndGet();
            return windowWith(event("Show", "2030-01-10T00:00:00Z"));
        }, clock);

        service.forLocation(la); // window is 2030-01-03..2030-01-17
        clock.advanceTo(Instant.parse("2030-01-20T08:00:00Z"));
        service.forLocation(la);

        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("does not regenerate on the last day before the refresh is due")
    void doesNotRegenerateJustBeforeTheInterval() {
        AtomicInteger calls = new AtomicInteger();
        MutableClock clock = new MutableClock(DAY_ZERO);
        LocalEventsService service = new LocalEventsService((loc, s, e) -> {
            calls.incrementAndGet();
            return windowWith(event("Show", "2030-01-20T00:00:00Z"));
        }, clock);

        service.forLocation(la);
        clock.advanceTo(DAY_ZERO.plus(2, ChronoUnit.DAYS));
        service.forLocation(la);

        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("reports when a location may next be refreshed - 3 days out from generation")
    void reportsNextRefreshDate() {
        LocalEventsService service = new LocalEventsService(
                (loc, s, e) -> windowWith(event("Show", "2030-01-10T00:00:00Z")),
                Clock.fixed(DAY_ZERO, ZoneOffset.UTC));

        assertThat(service.forLocation(la).nextRefreshAt()).isEqualTo("2030-01-04");
    }

    @Test
    @DisplayName("generates once per location even when many requests arrive at the same moment")
    void generatesOncePerLocationUnderConcurrentLoad() throws Exception {
        int threads = 16;
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);

        LocalEventsService service = new LocalEventsService((loc, s, e) -> {
            calls.incrementAndGet();
            return windowWith(event("Show", "2030-01-10T00:00:00Z"));
        }, Clock.fixed(DAY_ZERO, ZoneOffset.UTC));

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    start.await();
                    return service.forLocation(la);
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
    @DisplayName("the generation lock is per location, so different cities still generate in parallel")
    void lockIsScopedPerLocation() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        // both generations need to be in flight at once for the barrier to trip - a global lock
        // would hold the second caller out until the first returned
        CyclicBarrier bothInFlight = new CyclicBarrier(2);

        LocalEventsService service = new LocalEventsService((loc, s, e) -> {
            calls.incrementAndGet();
            try {
                bothInFlight.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException | BrokenBarrierException | java.util.concurrent.TimeoutException ex) {
                throw new RuntimeException("generations were serialized across locations", ex);
            }
            return windowWith(event("Show", "2030-01-10T00:00:00Z"));
        }, Clock.fixed(DAY_ZERO, ZoneOffset.UTC));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            var laCall = pool.submit(() -> service.forLocation(la));
            var chiCall = pool.submit(() -> service.forLocation(chicago));
            laCall.get(6, TimeUnit.SECONDS);
            chiCall.get(6, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("a generator failure is not cached - the next request tries again")
    void doesNotCacheGeneratorFailure() {
        AtomicInteger calls = new AtomicInteger();
        LocalEventsService service = new LocalEventsService((loc, s, e) -> {
            if (calls.incrementAndGet() == 1) {
                throw new IllegalStateException("transient");
            }
            return windowWith(event("Show", "2030-01-10T00:00:00Z"));
        }, Clock.fixed(DAY_ZERO, ZoneOffset.UTC));

        assertThat(service.forLocation(la).isEmpty()).isTrue();
        assertThat(service.forLocation(la).isEmpty()).isFalse();
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("an empty window is not cached - the next request tries again")
    void doesNotCacheEmptyWindow() {
        AtomicInteger calls = new AtomicInteger();
        LocalEventsService service = new LocalEventsService((loc, s, e) -> {
            calls.incrementAndGet();
            return LocalEventsWindow.empty("Los Angeles, California", "2030-01-03", "2030-01-17");
        }, Clock.fixed(DAY_ZERO, ZoneOffset.UTC));

        service.forLocation(la);
        service.forLocation(la);

        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("requests a 14-day window starting 2 days out, clear of the daily digest")
    void requestsWindowClearOfTheDigest() {
        LocalEventsService service = new LocalEventsService((loc, start, end) -> {
            assertThat(start).isEqualTo(java.time.LocalDate.parse("2030-01-03"));
            assertThat(end).isEqualTo(java.time.LocalDate.parse("2030-01-17"));
            return windowWith(event("Show", "2030-01-10T00:00:00Z"));
        }, Clock.fixed(DAY_ZERO, ZoneOffset.UTC));

        service.forLocation(la);
    }

    @Test
    @DisplayName("drops malformed and already-started events on read")
    void filtersEvents() {
        LocalEventsService service = new LocalEventsService(
                (loc, s, e) -> windowWith(
                        event("Bad time", "not-a-date"),
                        event("Already on", "2030-01-01T06:00:00Z"),
                        event("Still ahead", "2030-01-10T00:00:00Z")),
                Clock.fixed(DAY_ZERO, ZoneOffset.UTC));

        List<String> titles = service.forLocation(la).events().stream().map(LocalEvent::title).toList();

        assertThat(titles).containsExactly("Still ahead");
    }
}
