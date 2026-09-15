package com.remindme.digest;

import java.time.Instant;
import java.time.ZoneOffset;

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
public class DigestConfig {

    private static final Logger log = LoggerFactory.getLogger(DigestConfig.class);

    @Bean
    DigestGenerator digestGenerator(@Value("${anthropic.api_key:}") String apiKey,
            @Value("${tavily.api_key:}") String tavilyApiKey, EvalsService evalsService) {
        AnthropicClient client = AnthropicClientFactory.createIfConfigured(apiKey);
        if (client == null) {
            log.info("No Anthropic API key configured, the daily digest will stay empty");
            return (Instant now) -> DailyDigest.empty(now.atZone(ZoneOffset.UTC).toLocalDate().toString());
        }

        if (tavilyApiKey == null || tavilyApiKey.isBlank()) {
            log.warn("No Tavily API key configured, the daily digest will stay empty");
            return (Instant now) -> DailyDigest.empty(now.atZone(ZoneOffset.UTC).toLocalDate().toString());
        }

        log.info("Tavily API key configured, using ESPN + Tavily hybrid digest generation");
        return new HybridDigestGenerator(client, new TavilySearchClient(tavilyApiKey), new EspnScheduleClient(),
                evalsService);
    }
}
