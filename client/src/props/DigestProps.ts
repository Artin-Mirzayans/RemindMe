export interface DigestEventProps {
    title: string;
    summary: string;
    category: string;
    rating: 3 | 4 | 5;
    startsAt: string;
    howToWatch: string | null;
    recommendedDescription: string;
    recommendedReminderAt: string | null;
    source: string | null;
}

export interface DigestProps {
    date: string;
    events: DigestEventProps[];
}
