package com.remindme.search;

import java.util.List;

public record TavilySearchResult(String query, String answer, List<TavilyResult> results) {

    // true if the search actually returned something to feed the model
    public boolean hasResults() {
        return results != null && !results.isEmpty();
    }

    // renders this as plain text suitable for embedding in a Claude prompt
    public String formatForPrompt() {
        StringBuilder text = new StringBuilder();
        text.append("Query: ").append(query).append("\n");

        if (answer != null && !answer.isBlank()) {
            text.append("Summary: ").append(answer).append("\n");
        }

        for (TavilyResult result : results) {
            text.append("- ").append(result.title()).append(" (").append(result.url()).append("): ")
                    .append(result.content()).append("\n");
        }

        return text.toString();
    }
}
