package com.remindme.local;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.ThinkingConfigDisabled;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.remindme.config.UsageLogging;
import com.remindme.local.TicketmasterClient.Event;

// fetches ticketed events near the user from Ticketmaster, then one plain Claude call curates
// them into a ranked JSON list (thinking off, parsed by hand - same reasons as the digest)
public class TicketmasterLocalEventsGenerator implements LocalEventsGenerator {

    private static final Logger log = LoggerFactory.getLogger(TicketmasterLocalEventsGenerator.class);

    private static final String MODEL = "claude-haiku-4-5";
    private static final long MAX_TOKENS = 8000L;

    private final AnthropicClient client;
    private final TicketmasterClient ticketmasterClient;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public TicketmasterLocalEventsGenerator(AnthropicClient client, TicketmasterClient ticketmasterClient) {
        this.client = client;
        this.ticketmasterClient = ticketmasterClient;
    }

    @Override
    public LocalEventsWindow generate(GeoLocation location, LocalDate windowStart, LocalDate windowEnd) {
        List<Event> events = ticketmasterClient.near(location, windowStart, windowEnd);
        if (events.isEmpty()) {
            log.warn("Ticketmaster returned nothing for {}", location.label());
            return LocalEventsWindow.empty(location.label(), windowStart.toString(), windowEnd.toString());
        }

        String listing = events.stream().map(Event::line).collect(Collectors.joining("\n"));

        MessageCreateParams params = MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(MAX_TOKENS)
                .thinking(ThinkingConfigDisabled.builder().build())
                .addUserMessage(prompt(location, windowStart, windowEnd, listing))
                .build();

        log.info("Requesting local events for {} ({}..{})", location.label(), windowStart, windowEnd);

        var response = client.messages().create(params);
        UsageLogging.log(log, "LocalEvents", MODEL, response.usage());

        String text = response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(block -> block.text())
                .collect(Collectors.joining());

        return parse(text, location.label(), windowStart, windowEnd);
    }

    LocalEventsWindow parse(String text, String location, LocalDate windowStart, LocalDate windowEnd) {
        String json = extractJson(text);
        if (json == null) {
            log.warn("Local events response had no JSON object: {}",
                    text != null && text.length() > 500 ? text.substring(0, 500) + "..." : text);
            return LocalEventsWindow.empty(location, windowStart.toString(), windowEnd.toString());
        }
        try {
            LocalEventsWindow parsed = objectMapper.readValue(json, LocalEventsWindow.class);
            List<LocalEvent> events = parsed.events() == null ? List.of() : parsed.events();
            return new LocalEventsWindow(location, windowStart.toString(), windowEnd.toString(), events, null);
        } catch (Exception e) {
            log.warn("Local events response was not parseable JSON: {}", e.getMessage());
            return LocalEventsWindow.empty(location, windowStart.toString(), windowEnd.toString());
        }
    }

    private static String extractJson(String text) {
        if (text == null) {
            return null;
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        return text.substring(start, end + 1);
    }

    private String prompt(GeoLocation location, LocalDate windowStart, LocalDate windowEnd, String listing) {
        return """
                Below are ticketed events near %s between %s and %s, from Ticketmaster, sorted by \
                relevance (biggest first). Curate them.

                %s

                """.formatted(location.label(), windowStart, windowEnd, listing)
                + LocalEventsRubric.TEXT
                + """


                        Respond with ONLY this JSON object, nothing before or after it:
                        {"location":"%s","windowStart":"%s","windowEnd":"%s","events":[{"title":"...",\
                        "summary":"...","category":"...","rating":5,"startsAt":"yyyy-MM-ddTHH:mm:ssZ",\
                        "venue":"...","priceFrom":"$45+ or null","recommendedDescription":"...",\
                        "recommendedReminderAt":"yyyy-MM-ddTHH:mm:ssZ","ticketUrl":"..."}]}
                        """.formatted(location.label(), windowStart, windowEnd);
    }
}
