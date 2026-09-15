package com.remindme.watchlist;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.anthropic.client.AnthropicClient;
import com.remindme.config.AnthropicClientFactory;
import com.remindme.evals.EvalsService;
import com.remindme.search.TavilySearchClient;

@Configuration
public class WatchlistConfig {

    private static final Logger log = LoggerFactory.getLogger(WatchlistConfig.class);

    @Bean
    WatchlistGenerator watchlistGenerator(@Value("${anthropic.api_key:}") String apiKey,
            @Value("${tavily.api_key:}") String tavilyApiKey, EvalsService evalsService) {
        AnthropicClient client = AnthropicClientFactory.createIfConfigured(apiKey);
        if (client == null) {
            log.info("No Anthropic API key configured, the watchlist will stay empty");
            return (LocalDate windowStart, LocalDate windowEnd) -> WatchlistWindow.empty(windowStart.toString(),
                    windowEnd.toString());
        }

        if (tavilyApiKey == null || tavilyApiKey.isBlank()) {
            log.warn("No Tavily API key configured, the watchlist will stay empty");
            return (LocalDate windowStart, LocalDate windowEnd) -> WatchlistWindow.empty(windowStart.toString(),
                    windowEnd.toString());
        }

        log.info("Tavily API key configured, using Tavily-backed watchlist generation");
        return new TavilyWatchlistGenerator(client, new TavilySearchClient(tavilyApiKey), evalsService);
    }
}
