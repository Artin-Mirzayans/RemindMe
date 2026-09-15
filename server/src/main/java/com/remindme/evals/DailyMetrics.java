package com.remindme.evals;

// one feature's rolled-up counters for a single UTC day
public record DailyMetrics(
        String feature,
        String day,
        long requests,
        long generations,
        long successes,
        long empties,
        long failures,
        double costUsd,
        long inputTokens,
        long outputTokens,
        long latencyMsTotal) {

    static DailyMetrics empty(String feature, String day) {
        return new DailyMetrics(feature, day, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
