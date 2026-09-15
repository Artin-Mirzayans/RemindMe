package com.remindme.config;

import org.slf4j.Logger;

import com.anthropic.models.messages.Usage;

// logs what a Claude call cost - tokens and an estimated dollar figure - so spend stays visible
public final class UsageLogging {

    private UsageLogging() {
    }

    // published $/million-token rates: {input, output}. cache reads bill at 10% of input, writes at 125%
    private static double[] rates(String model) {
        String m = model == null ? "" : model.toLowerCase();
        if (m.contains("haiku")) {
            return new double[] { 0.80, 4.00 };
        }
        if (m.contains("opus")) {
            return new double[] { 15.00, 75.00 };
        }
        return new double[] { 3.00, 15.00 }; // sonnet / default
    }

    // returns the estimated cost so callers can feed it into their own tracking without
    // redoing this math themselves
    public static double log(Logger log, String label, String model, Usage usage) {
        long input = usage.inputTokens();
        long output = usage.outputTokens();
        long cacheRead = usage.cacheReadInputTokens().orElse(0L);
        long cacheWrite = usage.cacheCreationInputTokens().orElse(0L);

        double[] r = rates(model);
        double cost = (input * r[0] + cacheRead * r[0] * 0.10 + cacheWrite * r[0] * 1.25 + output * r[1]) / 1_000_000d;

        log.info("{} usage [{}]: input={} output={} cacheRead={} cacheWrite={} -> est ${}",
                label, model, input, output, cacheRead, cacheWrite, String.format("%.4f", cost));

        return cost;
    }
}
