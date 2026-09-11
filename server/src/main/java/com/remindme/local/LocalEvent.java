package com.remindme.local;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record LocalEvent(
        @JsonPropertyDescription("Event name, at most 90 characters") String title,

        @JsonPropertyDescription("One sentence on who or what it is and why it's worth going to") String summary,

        @JsonPropertyDescription("Short label: Music, Sports, Theatre, Comedy, Family, or Festival") String category,

        @JsonPropertyDescription("Significance 1-5: 5 is a stadium/arena headliner or a marquee pro game, "
                + "4 is a well-known act at a mid-size venue, 3 is a solid show worth knowing about. "
                + "Never 1 or 2 - leave small club shows and open mics out") Integer rating,

        @JsonPropertyDescription("Start time as a UTC timestamp yyyy-MM-ddTHH:mm:ssZ") String startsAt,

        @JsonPropertyDescription("Venue and city, e.g. \"Kia Forum, Inglewood\"") String venue,

        @JsonPropertyDescription("Lowest ticket price as shown, e.g. \"$45+\", or null if unknown") String priceFrom,

        @JsonPropertyDescription("A reminder label with the date, at most 40 characters, e.g. \"Robyn @ Kia Forum Sep 23\"") String recommendedDescription,

        @JsonPropertyDescription("When to remind the user (UTC yyyy-MM-ddTHH:mm:ssZ) - a day or two before "
                + "startsAt so they can still get tickets and make plans") String recommendedReminderAt,

        @JsonPropertyDescription("The ticket / event URL") String ticketUrl) {
}
