package com.remindme.watchlist;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WatchlistRankerTest {

    private final WatchlistRanker ranker = new WatchlistRanker();

    private WatchlistEvent event(String title, String category, int rating, String startsAt) {
        return new WatchlistEvent(title, "summary", category, rating, startsAt, startsAt == null ? "TBD" : null,
                null, "Watch this", startsAt == null ? null : "2030-01-01T00:00:00Z", null);
    }

    @Test
    @DisplayName("cold start (no interest history) sorts by rating then start time")
    void coldStartSortsByRatingThenDate() {
        List<WatchlistEvent> events = List.of(
                event("3-star game", "Basketball", 3, "2030-01-10T00:00:00Z"),
                event("5-star final", "Chess", 5, "2030-01-05T00:00:00Z"),
                event("4-star match", "Tennis", 4, "2030-01-01T00:00:00Z"));

        List<String> titles = ranker.rank(events, Map.of()).stream().map(WatchlistEvent::title).toList();

        assertThat(titles).containsExactly("5-star final", "4-star match", "3-star game");
    }

    @Test
    @DisplayName("heavy clicks on a category move same-rating events in that category above others")
    void clicksReorderWithinARating() {
        List<WatchlistEvent> events = List.of(
                event("Basketball game", "Basketball", 4, "2030-01-01T00:00:00Z"),
                event("Tennis match", "Tennis", 4, "2030-01-02T00:00:00Z"));

        List<String> titles = ranker.rank(events, Map.of("Basketball", 20)).stream().map(WatchlistEvent::title)
                .toList();

        assertThat(titles).containsExactly("Basketball game", "Tennis match");
    }

    @Test
    @DisplayName("no amount of clicks lets a lower rating outrank a higher one")
    void clicksNeverCrossARatingBoundary() {
        List<WatchlistEvent> events = List.of(
                event("3-star favorite", "Basketball", 3, "2030-01-01T00:00:00Z"),
                event("4-star unrelated", "Tennis", 4, "2030-01-02T00:00:00Z"));

        List<String> titles = ranker.rank(events, Map.of("Basketball", 1_000_000)).stream()
                .map(WatchlistEvent::title).toList();

        assertThat(titles).containsExactly("4-star unrelated", "3-star favorite");
    }
}
