package com.remindme.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;

import com.remindme.digest.DigestService;
import com.remindme.watchlist.WatchlistService;

class CacheWarmupSchedulerTest {

    @Test
    @DisplayName("one warm-up pass touches each feed exactly once")
    void warmsEachFeedOnce() {
        DigestService digest = mock(DigestService.class);
        WatchlistService watchlist = mock(WatchlistService.class);

        new CacheWarmupScheduler(digest, watchlist).warmCaches();

        verify(digest).today();
        verify(watchlist).current();
        verifyNoMoreInteractions(digest, watchlist);
    }

    @Test
    @DisplayName("the warm-up is scheduled once a day, not more often")
    void runsDailyInUtc() throws Exception {
        Method warm = CacheWarmupScheduler.class.getMethod("warmCaches");
        Scheduled scheduled = warm.getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.zone()).isEqualTo("UTC");

        CronExpression cron = CronExpression.parse(scheduled.cron());
        var first = java.time.LocalDateTime.parse("2030-01-01T12:00:00");
        var next = cron.next(first);
        var afterThat = cron.next(next);

        // consecutive firings are 24h apart - a stricter schedule would regress cost
        assertThat(java.time.Duration.between(next, afterThat)).isEqualTo(java.time.Duration.ofDays(1));
    }
}
