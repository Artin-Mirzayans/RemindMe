package com.remindme.evals;

// what the public status page gets: how reliable and how fast, never how many or how much.
// both are null when the feed hasn't generated anything in the window
public record FeedHealth(String feature, int windowDays, Double successRate, Double avgLatencyMs) {
}
