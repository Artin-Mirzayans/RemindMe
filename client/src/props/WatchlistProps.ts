export interface WatchlistEventProps {
    title: string;
    summary: string;
    category: string;
    rating: 3 | 4 | 5;
    startsAt: string | null;
    expectedWindow: string | null;
    source: string | null;
    recommendedDescription: string;
    recommendedReminderAt: string | null;
    howToWatch: string | null;
}

export interface WatchlistProps {
    windowStart: string;
    windowEnd: string;
    nextRefreshAt: string | null;
    events: WatchlistEventProps[];
}
