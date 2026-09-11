package com.remindme.local;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record LocalEventsWindow(
        @JsonPropertyDescription("The area these events are near, e.g. \"Los Angeles, California\"") String location,

        @JsonPropertyDescription("Start of the coverage window, yyyy-MM-dd") String windowStart,

        @JsonPropertyDescription("End of the coverage window, yyyy-MM-dd") String windowEnd,

        @JsonPropertyDescription("8 to 15 events worth going out for, biggest first") List<LocalEvent> events,

        // date (yyyy-MM-dd) this location becomes eligible to refresh again, stamped by the
        // service - anything the model sets here gets overwritten
        String nextRefreshAt) {

    public static LocalEventsWindow empty(String location, String windowStart, String windowEnd) {
        return new LocalEventsWindow(location, windowStart, windowEnd, List.of(), null);
    }

    public LocalEventsWindow withNextRefreshAt(String nextRefreshAt) {
        return new LocalEventsWindow(location, windowStart, windowEnd, events, nextRefreshAt);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return events == null || events.isEmpty();
    }
}
