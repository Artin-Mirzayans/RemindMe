export interface FeedHealth {
    feature: string;
    windowDays: number;
    successRate: number | null;
    avgLatencyMs: number | null;
}
