package com.remindme.watchlist;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class WatchlistRanker {

    private static final int MIN_RATING = 3;
    private static final int MAX_RATING = 5;
    private static final double RATING_WEIGHT = 10.0;

    // Kept below the 10-point gap between ratings so personalization can only reorder
    // events within a rating, never promote one across a rating boundary.
    private static final double AFFINITY_MAX = 8.0;
    private static final double SMOOTHING_K = 5.0;

    public List<WatchlistEvent> rank(List<WatchlistEvent> events, Map<String, Integer> interestCounts) {
        return events.stream()
                .sorted(Comparator
                        .comparingDouble((WatchlistEvent event) -> -score(event, interestCounts))
                        .thenComparing(event -> event.startsAt() == null ? "9999" : event.startsAt())
                        .thenComparing(WatchlistEvent::title))
                .collect(Collectors.toList());
    }

    private double score(WatchlistEvent event, Map<String, Integer> interestCounts) {
        int rating = clampRating(event.rating());
        double ratingBase = rating * RATING_WEIGHT;

        int clicks = interestCounts.getOrDefault(event.category(), 0);
        double affinity = AFFINITY_MAX * (clicks / (clicks + SMOOTHING_K));

        return ratingBase + affinity;
    }

    private int clampRating(Integer rating) {
        if (rating == null) {
            return MIN_RATING;
        }
        return Math.max(MIN_RATING, Math.min(MAX_RATING, rating));
    }
}
