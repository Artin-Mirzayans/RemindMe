export interface DailyPoint {
    day: string;
    requests: number;
    generations: number;
    costUsd: number;
}

export interface EvalsSummary {
    feature: string;
    windowDays: number;
    totalRequests: number;
    totalGenerations: number;
    cacheHitRate: number;
    successRate: number;
    emptyRate: number;
    failureRate: number;
    totalCostUsd: number;
    avgCostPerGeneration: number;
    avgLatencyMs: number;
    totalInputTokens: number;
    totalOutputTokens: number;
    daily: DailyPoint[];
}
