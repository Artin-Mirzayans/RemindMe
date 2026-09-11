package com.remindme.digest;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record DigestEvent(
        @JsonPropertyDescription("Short headline, at most 80 characters") String title,

        @JsonPropertyDescription("One plain sentence on what it is and why it's worth watching") String summary,

        @JsonPropertyDescription("Short domain label, e.g. Soccer, Tennis, Formula 1, Film, Awards") String category,

        @JsonPropertyDescription("How big a deal this is: 5 = must-see, unmissable; 4 = excellent, a real event; "
                + "3 = solid, worth knowing about. Only 3, 4 or 5.") Integer rating,

        @JsonPropertyDescription("Confirmed start time as a UTC timestamp, format yyyy-MM-ddTHH:mm:ssZ") String startsAt,

        @JsonPropertyDescription("Where to watch it live, a few words, e.g. \"ESPN\" or \"Netflix\"") String howToWatch,

        @JsonPropertyDescription("Reminder label including the event's local start time, at most 40 characters, "
                + "e.g. \"Yankees-Dodgers 7pm ET\"") String recommendedDescription,

        @JsonPropertyDescription("When to remind the user, a UTC timestamp yyyy-MM-ddTHH:mm:ssZ, 30-60 minutes before startsAt") String recommendedReminderAt,

        @JsonPropertyDescription("Publicly reachable URL backing this event, or null") String source) {
}
