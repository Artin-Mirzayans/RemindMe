package com.remindme.local;

// editorial rubric for the Nearby feed - the "go to it" counterpart to the watchlist's "watch it"
final class LocalEventsRubric {

    static final String TEXT = """
            Pick 8 to 15 events genuinely worth leaving the house for, biggest first. The list you're \
            given is already sorted by relevance, so the top entries are usually the real headliners \
            - arena and stadium acts, marquee pro or college games, big touring theatre and comedy, \
            major festivals. Recognize the act or team: a known artist at the Forum or a Chargers \
            game rates 5; a well-known act at a mid-size theatre rates 4; a solid regional show \
            rates 3. Leave out small club gigs, open mics, tribute nights, minor-league filler and \
            anything you don't recognize as a draw. At most four from one category, and spread \
            across music, sports, theatre and comedy rather than an all-concert list.

            Use the given start time (UTC) and venue verbatim - don't alter them. priceFrom is the \
            lowest listed price if there is one. recommendedReminderAt is a UTC timestamp one to \
            three days before startsAt, so there's still time to get tickets. recommendedDescription \
            is at most 40 characters and includes the date, e.g. "Robyn @ Kia Forum Sep 23".

            Title under 90 characters, summary one sentence.""";

    private LocalEventsRubric() {
    }
}
