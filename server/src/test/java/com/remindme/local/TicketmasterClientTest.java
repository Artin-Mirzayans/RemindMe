package com.remindme.local;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class TicketmasterClientTest {

    private final TicketmasterClient client = new TicketmasterClient("key");
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String RESPONSE = """
            {"_embedded":{"events":[
              {"name":"Robyn - The Sexistential Tour","url":"https://tm.com/robyn",
               "dates":{"start":{"dateTime":"2026-09-23T02:00:00Z"}},
               "classifications":[{"segment":{"name":"Music"},"genre":{"name":"Pop"}}],
               "priceRanges":[{"min":55.0,"max":250.0,"currency":"USD"}],
               "_embedded":{"venues":[{"name":"Kia Forum","city":{"name":"Inglewood"}}]}},
              {"name":"TBA Showcase","url":"https://tm.com/tba",
               "dates":{"start":{"dateTBA":true}},
               "_embedded":{"venues":[{"name":"Some Club","city":{"name":"LA"}}]}}
            ]}}""";

    @Test
    @DisplayName("parses events, skips ones with no start time, and renders a research line")
    void parsesEvents() throws Exception {
        List<TicketmasterClient.Event> events = client.parse(mapper.readTree(RESPONSE));

        assertThat(events).hasSize(1);
        TicketmasterClient.Event e = events.get(0);
        assertThat(e.name()).isEqualTo("Robyn - The Sexistential Tour");
        assertThat(e.startsAt()).isEqualTo("2026-09-23T02:00:00Z");
        assertThat(e.venue()).isEqualTo("Kia Forum, Inglewood");
        assertThat(e.genre()).isEqualTo("Music/Pop");
        assertThat(e.priceFrom()).isEqualTo("$55+");
        assertThat(e.line()).startsWith(
                "Robyn - The Sexistential Tour | 2026-09-23T02:00:00Z | Kia Forum, Inglewood | Music/Pop | $55+ |");
    }
}
