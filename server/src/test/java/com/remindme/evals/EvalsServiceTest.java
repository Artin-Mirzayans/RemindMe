package com.remindme.evals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EvalsServiceTest {

    private static final LocalDate TODAY = LocalDate.parse("2030-01-10");

    private EvalsService serviceWith(FeedMetricsRepository repository) {
        Clock clock = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
        return new EvalsService(repository, clock);
    }

    @Test
    @DisplayName("records a request against today's date")
    void recordsRequestForToday() {
        FeedMetricsRepository repository = mock(FeedMetricsRepository.class);
        serviceWith(repository).recordRequest(EvalsService.DIGEST);

        verify(repository).recordRequest(EvalsService.DIGEST, TODAY);
    }

    @Test
    @DisplayName("records a generation against today's date")
    void recordsGenerationForToday() {
        FeedMetricsRepository repository = mock(FeedMetricsRepository.class);
        serviceWith(repository).recordGeneration(EvalsService.WATCHLIST, GenerationOutcome.SUCCESS, 0.05, 100, 200,
                1500);

        verify(repository).recordGeneration(EvalsService.WATCHLIST, TODAY, GenerationOutcome.SUCCESS, 0.05, 100, 200,
                1500);
    }

    @Test
    @DisplayName("rolls up requests, generations and outcomes across the window")
    void aggregatesAcrossDays() {
        FeedMetricsRepository repository = mock(FeedMetricsRepository.class);
        when(repository.findRange(eq(EvalsService.DIGEST), any(), any())).thenReturn(List.of(
                new DailyMetrics(EvalsService.DIGEST, "2030-01-09", 10, 1, 1, 0, 0, 0.05, 500, 100, 800),
                new DailyMetrics(EvalsService.DIGEST, "2030-01-10", 6, 1, 0, 1, 0, 0.00, 300, 0, 400)));

        EvalsSummary summary = serviceWith(repository).summarize(EvalsService.DIGEST, 2);

        assertThat(summary.totalRequests()).isEqualTo(16);
        assertThat(summary.totalGenerations()).isEqualTo(2);
        // 14 of 16 requests were served without a real generation
        assertThat(summary.cacheHitRate()).isEqualTo(14.0 / 16);
        assertThat(summary.successRate()).isEqualTo(0.5);
        assertThat(summary.emptyRate()).isEqualTo(0.5);
        assertThat(summary.failureRate()).isEqualTo(0);
        assertThat(summary.totalCostUsd()).isEqualTo(0.05);
        assertThat(summary.avgCostPerGeneration()).isEqualTo(0.025);
        assertThat(summary.avgLatencyMs()).isEqualTo(600);
        assertThat(summary.totalInputTokens()).isEqualTo(800);
        assertThat(summary.totalOutputTokens()).isEqualTo(100);
        assertThat(summary.daily()).hasSize(2);
    }

    @Test
    @DisplayName("fills in a zeroed day when nothing happened rather than skipping it")
    void fillsGapDays() {
        FeedMetricsRepository repository = mock(FeedMetricsRepository.class);
        when(repository.findRange(eq(EvalsService.LOCAL_EVENTS), any(), any())).thenReturn(List.of());

        EvalsSummary summary = serviceWith(repository).summarize(EvalsService.LOCAL_EVENTS, 3);

        assertThat(summary.daily()).hasSize(3);
        assertThat(summary.daily()).allMatch(point -> point.requests() == 0 && point.generations() == 0);
        assertThat(summary.totalRequests()).isZero();
        assertThat(summary.cacheHitRate()).isZero();
    }

    @Test
    @DisplayName("a disabled instance never touches a repository and reports empty summaries")
    void disabledIsInert() {
        EvalsService disabled = EvalsService.disabled();

        disabled.recordRequest(EvalsService.DIGEST);
        disabled.recordGeneration(EvalsService.DIGEST, GenerationOutcome.SUCCESS, 1, 1, 1, 1);
        EvalsSummary summary = disabled.summarize(EvalsService.DIGEST, 7);

        assertThat(summary.totalRequests()).isZero();
        assertThat(summary.totalGenerations()).isZero();
    }
}
