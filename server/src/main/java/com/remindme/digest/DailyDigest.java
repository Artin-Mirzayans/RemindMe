package com.remindme.digest;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record DailyDigest(
        @JsonPropertyDescription("The date the digest covers, as yyyy-MM-dd") String date,

        @JsonPropertyDescription("Between 4 and 8 notable events happening on this date") List<DigestEvent> events) {

    public static DailyDigest empty(String date) {
        return new DailyDigest(date, List.of());
    }

    @JsonIgnore
    public boolean isEmpty() {
        return events == null || events.isEmpty();
    }
}
