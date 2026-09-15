package com.remindme.evals;

import java.util.List;

// the public shape: aggregated over the window, plus a daily series to chart
public record EvalsSummary(
        String feature,
        int windowDays,
        long totalRequests,
        long totalGenerations,
        double cacheHitRate,
        double successRate,
        double emptyRate,
        double failureRate,
        double totalCostUsd,
        double avgCostPerGeneration,
        double avgLatencyMs,
        long totalInputTokens,
        long totalOutputTokens,
        List<DailyPoint> daily) {

    public record DailyPoint(String day, long requests, long generations, double costUsd) {
    }
}
