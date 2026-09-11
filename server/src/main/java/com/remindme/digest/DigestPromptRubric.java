package com.remindme.digest;

// shared editorial rubric for every DigestGenerator. the scheduled-sports list is exact (real
// matchups, real UTC times); the TV/other lists come from web search and need more scrutiny
final class DigestPromptRubric {

    static final String TEXT = """
            Pick 4 to 8 things a person can watch live at a known time. Favor the highest level of \
            each thing: a Champions League or big-league fixture, a Grand Slam or marquee tennis \
            match, a playoff or title game, a championship fight, an F1 qualifying session or race \
            (skip ordinary practice), an awards show, a major premiere or finale. A regular-season \
            game counts only when both teams matter - a rivalry (Subway Series, Yankees-Red Sox, \
            El Clasico) or two contenders - not a marquee club hosting a cellar-dweller, and never \
            just a run-down of the day's slate. At most three from one sport.

            The SCHEDULED SPORTS list is exact - use its matchups, competitions and UTC times \
            verbatim; do not alter a time or invent a fixture. For the TV and OTHER lists (web \
            search), only include an item whose name, start time and today-or-tomorrow date you can \
            pin down; cross-check dates against what you know (the Emmys, for instance, are \
            mid-September), and skip anything vague, already started, or from an SEO / aggregator \
            page. It's fine to lean mostly on the sports list on a quiet day.

            All startsAt and recommendedReminderAt values are UTC (yyyy-MM-ddTHH:mm:ssZ). When you \
            do need to convert a TV time yourself: in September US Eastern is UTC-4, Central UTC-5, \
            Mountain UTC-6, Pacific UTC-7, UK UTC+1. recommendedReminderAt is 30 to 60 minutes \
            before startsAt.

            Give every event a rating of 3, 4 or 5: 5 = must-see, unmissable (a final, a title \
            fight, a genuine rivalry, a huge premiere); 4 = excellent, a real event most fans would \
            clear time for; 3 = solid, worth knowing about. Use the full range - not everything is \
            a 5.

            recommendedDescription is the reminder label, at most 40 characters, and must include \
            the event's local start time, e.g. "Bayern vs Bodo 3pm ET" or "Emmy Awards 8pm ET". \
            howToWatch is a few words on where to tune in (use the sports list's broadcaster when \
            given). Title under 80 characters, summary one plain sentence. Skip video games and \
            esports.""";

    private DigestPromptRubric() {
    }
}
