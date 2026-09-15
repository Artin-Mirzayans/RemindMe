package com.remindme.watchlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.anthropic.client.AnthropicClient;
import com.remindme.evals.EvalsService;
import com.remindme.search.TavilySearchClient;
import com.remindme.search.TavilySearchResult;

class TavilyWatchlistGeneratorTest {

    private final TavilyWatchlistGenerator generator = new TavilyWatchlistGenerator(null, null,
            EvalsService.disabled());
    private final LocalDate start = LocalDate.parse("2026-09-12");
    private final LocalDate end = LocalDate.parse("2026-09-26");

    @Test
    @DisplayName("skips the model call entirely when every Tavily query comes back empty")
    void skipsModelCallWhenNoResearch() {
        AnthropicClient anthropic = mock(AnthropicClient.class);
        TavilySearchClient tavily = mock(TavilySearchClient.class);
        when(tavily.search(anyString())).thenReturn(new TavilySearchResult("q", null, List.of()));

        TavilyWatchlistGenerator gen = new TavilyWatchlistGenerator(anthropic, tavily, EvalsService.disabled());
        WatchlistWindow window = gen.generate(start, end);

        assertThat(window.isEmpty()).isTrue();
        assertThat(window.windowStart()).isEqualTo("2026-09-12");
        verifyNoInteractions(anthropic);
    }

    @Test
    @DisplayName("parses a clean JSON object and forces the requested window dates")
    void parsesCleanJson() {
        String json = """
                {"windowStart":"whatever","windowEnd":"whatever","events":[
                  {"title":"UCL Final","summary":"The final.","category":"Soccer","rating":5,
                   "startsAt":"2026-09-20T19:00:00Z","expectedWindow":null,"source":"uefa.com",
                   "recommendedDescription":"UCL Final","recommendedReminderAt":"2026-09-20T18:30:00Z",
                   "howToWatch":"CBS"}]}
                """;

        WatchlistWindow window = generator.parse(json, start, end);

        assertThat(window.windowStart()).isEqualTo("2026-09-12");
        assertThat(window.windowEnd()).isEqualTo("2026-09-26");
        assertThat(window.events()).hasSize(1);
        assertThat(window.events().get(0).rating()).isEqualTo(5);
        assertThat(window.events().get(0).recommendedReminderAt()).isEqualTo("2026-09-20T18:30:00Z");
    }

    @Test
    @DisplayName("returns an empty window on no JSON or broken JSON")
    void handlesBadInput() {
        assertThat(generator.parse("no json here", start, end).isEmpty()).isTrue();
        assertThat(generator.parse("{\"events\":[{\"title\":\"cut", start, end).isEmpty()).isTrue();
    }
}
