package com.remindme.evals;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// records what the three AI-curated feeds actually do (a request came in, a real generation
// ran and what it cost) and aggregates it back out for the /evals endpoint. every write here
// is fire-and-forget from the caller's point of view - the repository swallows its own errors,
// so a metrics hiccup never takes down an actual feed
@Service
public class EvalsService {

    public static final String DIGEST = "Digest";
    public static final String WATCHLIST = "Watchlist";
    public static final String LOCAL_EVENTS = "LocalEvents";

    private static final List<String> FEATURES = List.of(DIGEST, WATCHLIST, LOCAL_EVENTS);

    private final FeedMetricsRepository repository;
    private final Clock clock;

    @Autowired
    public EvalsService(FeedMetricsRepository repository) {
        this(repository, Clock.systemUTC());
    }

    EvalsService(FeedMetricsRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    // a no-op instance for tests that exercise a generator/service but don't care about
    // metrics - mirrors FeedCacheStore.disabled()
    public static EvalsService disabled() {
        return new EvalsService(null, Clock.systemUTC());
    }

    public void recordRequest(String feature) {
        if (repository == null) {
            return;
        }
        repository.recordRequest(feature, today());
    }

    public void recordGeneration(String feature, GenerationOutcome outcome, double costUsd, long inputTokens,
            long outputTokens, long latencyMs) {
        if (repository == null) {
            return;
        }
        repository.recordGeneration(feature, today(), outcome, costUsd, inputTokens, outputTokens, latencyMs);
    }

    public List<EvalsSummary> summarizeAll(int days) {
        return FEATURES.stream().map(feature -> summarize(feature, days)).toList();
    }

    public EvalsSummary summarize(String feature, int days) {
        LocalDate today = today();
        LocalDate from = today.minusDays(days - 1L);

        List<DailyMetrics> found = repository == null ? List.of() : repository.findRange(feature, from, today);
        Map<String, DailyMetrics> byDay = found.stream()
                .collect(Collectors.toMap(DailyMetrics::day, m -> m));

        List<DailyMetrics> series = from.datesUntil(today.plusDays(1))
                .map(day -> byDay.getOrDefault(day.toString(), DailyMetrics.empty(feature, day.toString())))
                .toList();

        long requests = series.stream().mapToLong(DailyMetrics::requests).sum();
        long generations = series.stream().mapToLong(DailyMetrics::generations).sum();
        long successes = series.stream().mapToLong(DailyMetrics::successes).sum();
        long empties = series.stream().mapToLong(DailyMetrics::empties).sum();
        long failures = series.stream().mapToLong(DailyMetrics::failures).sum();
        double totalCost = series.stream().mapToDouble(DailyMetrics::costUsd).sum();
        long inputTokens = series.stream().mapToLong(DailyMetrics::inputTokens).sum();
        long outputTokens = series.stream().mapToLong(DailyMetrics::outputTokens).sum();
        long latencyTotal = series.stream().mapToLong(DailyMetrics::latencyMsTotal).sum();

        List<EvalsSummary.DailyPoint> daily = series.stream()
                .map(m -> new EvalsSummary.DailyPoint(m.day(), m.requests(), m.generations(), m.costUsd()))
                .toList();

        return new EvalsSummary(
                feature,
                days,
                requests,
                generations,
                rate(requests - generations, requests),
                rate(successes, generations),
                rate(empties, generations),
                rate(failures, generations),
                totalCost,
                generations == 0 ? 0 : totalCost / generations,
                generations == 0 ? 0 : (double) latencyTotal / generations,
                inputTokens,
                outputTokens,
                daily);
    }

    private static double rate(long part, long whole) {
        return whole == 0 ? 0 : (double) part / whole;
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(ZoneOffset.UTC));
    }
}
