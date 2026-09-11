package com.remindme.watchlist;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record WatchlistEvent(
        @JsonPropertyDescription("Short headline for the event, at most 80 characters") String title,

        @JsonPropertyDescription("One or two sentences on why this event clears the bar for inclusion") String summary,

        @JsonPropertyDescription("A short domain label for this event, e.g. Basketball, Chess, Film, "
                + "Television, Comedy, Wrestling, Awards, Space. Not a fixed list - use whatever label "
                + "fits, and reuse the same label for the same domain across events instead of inventing "
                + "new phrasing each time") String category,

        @JsonPropertyDescription("Significance on a 1-5 scale, but only ever 5, 4, or 3: 5 is the single "
                + "biggest thing of its kind that week (rare), 4 is a clear step below 5 but still a big deal "
                + "in its domain, 3 is solid mainstream interest, routine for its domain, used sparingly to "
                + "fill otherwise-quiet stretches. Never use 1 or 2 - if it doesn't reach 3, leave it out") Integer rating,

        @JsonPropertyDescription("UTC start time as yyyy-MM-ddTHH:mm:ssZ when a specific time is confirmed, "
                + "otherwise null") String startsAt,

        @JsonPropertyDescription("When startsAt is null, a short human-readable window such as 'Early October' "
                + "or 'Date TBD, pending playoff seeding'. Null when startsAt is set") String expectedWindow,

        @JsonPropertyDescription("Publicly reachable URL backing this event, or null") String source,

        @JsonPropertyDescription("A punchy label for this event to use as a reminder's title, at most 40 "
                + "characters; include the local start time when known, e.g. 'El Clasico 3pm ET' or "
                + "'Dune 3 premiere'") String recommendedDescription,

        @JsonPropertyDescription("A UTC timestamp (yyyy-MM-ddTHH:mm:ssZ) for when to remind the user. When "
                + "startsAt is known, set it 15-60 minutes before for a live event, or a day or more before "
                + "for a premiere or release they need to plan around. When startsAt is null, set it to a "
                + "sensible date on or just before the expectedWindow so the user is nudged to check. Must be "
                + "in the future") String recommendedReminderAt,

        @JsonPropertyDescription("A few words on where or how to actually watch or experience this, e.g. "
                + "'ESPN, fuboTV', 'Netflix', 'NASA livestream on YouTube', 'Theaters nationwide'. Null if "
                + "not known") String howToWatch) {
}
