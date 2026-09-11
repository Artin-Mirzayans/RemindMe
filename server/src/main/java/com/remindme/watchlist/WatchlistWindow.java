package com.remindme.watchlist;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record WatchlistWindow(
        @JsonPropertyDescription("Start of the coverage window, as yyyy-MM-dd") String windowStart,

        @JsonPropertyDescription("End of the coverage window, as yyyy-MM-dd") String windowEnd,

        @JsonPropertyDescription("8 to 20 notable events across the window, weighted toward the higher end of the rating scale") List<WatchlistEvent> events,

        // date (yyyy-MM-dd) this feed becomes eligible to refresh again, stamped by the service -
        // anything the model sets here gets overwritten
        String nextRefreshAt) {

    public static WatchlistWindow empty(String windowStart, String windowEnd) {
        return new WatchlistWindow(windowStart, windowEnd, List.of(), null);
    }

    public WatchlistWindow withNextRefreshAt(String nextRefreshAt) {
        return new WatchlistWindow(windowStart, windowEnd, events, nextRefreshAt);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return events == null || events.isEmpty();
    }
}
