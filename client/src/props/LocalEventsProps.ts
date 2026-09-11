export interface LocalEventProps {
    title: string;
    summary: string;
    category: string;
    rating: 3 | 4 | 5;
    startsAt: string;
    venue: string;
    priceFrom: string | null;
    recommendedDescription: string;
    recommendedReminderAt: string | null;
    ticketUrl: string | null;
}

export interface LocalEventsProps {
    location: string;
    windowStart: string;
    windowEnd: string;
    nextRefreshAt: string | null;
    events: LocalEventProps[];
}
