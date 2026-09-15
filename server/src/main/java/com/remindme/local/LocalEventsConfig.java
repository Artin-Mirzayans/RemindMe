package com.remindme.local;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.anthropic.client.AnthropicClient;
import com.remindme.config.AnthropicClientFactory;
import com.remindme.evals.EvalsService;

@Configuration
public class LocalEventsConfig {

    private static final Logger log = LoggerFactory.getLogger(LocalEventsConfig.class);

    @Bean
    LocalEventsGenerator localEventsGenerator(@Value("${anthropic.api_key:}") String anthropicKey,
            @Value("${ticketmaster.api_key:}") String ticketmasterKey, EvalsService evalsService) {
        AnthropicClient client = AnthropicClientFactory.createIfConfigured(anthropicKey);
        boolean hasTicketmaster = ticketmasterKey != null && !ticketmasterKey.isBlank();

        if (client == null || !hasTicketmaster) {
            log.info("Anthropic and/or Ticketmaster key missing, the Nearby feed will stay empty");
            return (GeoLocation location, LocalDate start, LocalDate end) -> LocalEventsWindow.empty(
                    location.label(), start.toString(), end.toString());
        }

        log.info("Ticketmaster key configured, using Ticketmaster-backed Nearby generation");
        return new TicketmasterLocalEventsGenerator(client, new TicketmasterClient(ticketmasterKey), evalsService);
    }
}
