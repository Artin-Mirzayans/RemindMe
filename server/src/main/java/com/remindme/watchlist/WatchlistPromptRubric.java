package com.remindme.watchlist;

// shared editorial rubric for every WatchlistGenerator, so a change like "add a domain" or
// "adjust the rating bar" only has to happen in one place
final class WatchlistPromptRubric {

    static final String TEXT = """
            Include only things that are, during this window, the highest level of competition, \
            achievement, or anticipation in their domain, anywhere in the world - and that anyone \
            can experience digitally (broadcast, streamed, or otherwise available online) with no \
            barrier except knowing it's happening. This is about real-world happenings worth tuning \
            into, not gaming - video games and esports are out of scope regardless of scale. Domains \
            include sports, chess and other mind-sports, film releases, television, music, comedy \
            (a major stand-up special dropping, or a milestone taping of a huge live comedy show or \
            podcast - think Kill Tony-caliber, not a routine local show), wrestling, awards shows, \
            space exploration, or anything else that fits the bar. Skip anything that only matters to \
            a live, in-person crowd, or that requires physical attendance to experience the moment it \
            happens.

            Rate each event's significance on a 1-5 scale, but only ever use 5, 4, or 3:
            - 5 (rare, well under 10 across the whole window): the single biggest thing of its kind \
              happening that week - a championship or title genuinely being decided, a once-a-year or \
              once-a-generation cultural moment, the debut of something enormous and long-anticipated.
            - 4: a clear step below 5 but still a big deal in its domain - a semifinal or elimination \
              stage, a top-ranked matchup with real stakes, a major but not tentpole release or premiere.
            - 3 (use sparingly, only to fill otherwise-quiet stretches, never to pad the count): solid \
              mainstream interest, but routine for its domain.

            Never use 1 or 2 - these ratings are about the bar above, not a checklist of named events; \
            judge anything against that bar, whatever domain it comes from, and leave it out entirely \
            if it doesn't reach 3. This is a highlights list, not a schedule.

            For each event, assign a short, reusable category label for its domain (e.g. "Basketball", \
            "Chess", "Film", "Television", "Comedy", "Wrestling", "Awards", "Space") - keep the same \
            label for the same domain across events rather than inventing new phrasing each time.

            The startsAt field must be EITHER a single valid UTC timestamp in exactly the format \
            yyyy-MM-ddTHH:mm:ssZ (e.g. 2026-09-10T18:00:00Z) OR null - never a partial string, never \
            the word "null" as text, never anything else. Set it only when there's a specific \
            confirmed start time. When there's no confirmed time yet, leave startsAt null and set \
            expectedWindow to a short human-readable estimate (e.g. "Early October" or "Date TBD, \
            pending playoff seeding") instead. Do not guess a precise time.

            For each event also provide:
            - recommendedDescription: a punchy reminder label (at most 40 characters); include the \
              local start time when known, e.g. "El Clasico 3pm ET" or "Dune 3 premiere".
            - recommendedReminderAt: a UTC timestamp (yyyy-MM-ddTHH:mm:ssZ) for when to remind the \
              user. When startsAt is known, set it 15-60 minutes before for a live event, or a day \
              or more before for a premiere or release they need to plan around. When startsAt is \
              null, set it to a sensible date on or just before the expectedWindow. It must be in \
              the future - never before the current date.
            - howToWatch: a few words on where to actually watch or experience it, e.g. "ESPN, \
              fuboTV", "Netflix", "NASA livestream on YouTube", "Theaters nationwide". Null if you \
              can't find this.

            Keep each title under 80 characters and each summary to one or two plain sentences \
            explaining why the event clears the bar.""";

    private WatchlistPromptRubric() {
    }
}
