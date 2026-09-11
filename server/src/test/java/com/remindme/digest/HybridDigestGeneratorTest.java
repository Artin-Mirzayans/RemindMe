package com.remindme.digest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.anthropic.client.AnthropicClient;
import com.remindme.search.TavilySearchClient;
import com.remindme.search.TavilySearchResult;

class HybridDigestGeneratorTest {

    private final HybridDigestGenerator generator = new HybridDigestGenerator(null, null, null);
    private final LocalDate today = LocalDate.parse("2026-09-10");

    @Test
    @DisplayName("skips the model call entirely when ESPN and the web searches all come back empty")
    void skipsModelCallWhenNoResearch() {
        AnthropicClient anthropic = mock(AnthropicClient.class);
        EspnScheduleClient espn = mock(EspnScheduleClient.class);
        TavilySearchClient tavily = mock(TavilySearchClient.class);

        when(espn.upcoming(any(), any())).thenReturn(List.of());
        when(tavily.search(anyString())).thenReturn(new TavilySearchResult("q", null, List.of()));

        HybridDigestGenerator gen = new HybridDigestGenerator(anthropic, tavily, espn);
        DailyDigest digest = gen.generate(Instant.parse("2026-09-10T08:00:00Z"));

        assertThat(digest.isEmpty()).isTrue();
        verifyNoInteractions(anthropic);
    }

    @Test
    @DisplayName("parses a clean JSON object into a digest")
    void parsesCleanJson() {
        String json = """
                {"date":"2026-09-10","events":[
                  {"title":"Bayern vs Bodo","summary":"Big game.","category":"Soccer","rating":4,
                   "startsAt":"2026-09-10T19:00:00Z","howToWatch":"CBS","recommendedDescription":"Bayern vs Bodo",
                   "recommendedReminderAt":"2026-09-10T18:30:00Z","source":"https://x.com"}]}
                """;

        DailyDigest digest = generator.parse(json, today);

        assertThat(digest.date()).isEqualTo("2026-09-10");
        assertThat(digest.events()).hasSize(1);
        assertThat(digest.events().get(0).title()).isEqualTo("Bayern vs Bodo");
        assertThat(digest.events().get(0).rating()).isEqualTo(4);
        assertThat(digest.events().get(0).recommendedReminderAt()).isEqualTo("2026-09-10T18:30:00Z");
    }

    @Test
    @DisplayName("pulls the JSON out of a fenced / prose-wrapped reply")
    void parsesFencedJson() {
        String reply = "Here is the digest:\n```json\n{\"date\":\"x\",\"events\":[]}\n```\nHope that helps.";

        DailyDigest digest = generator.parse(reply, today);

        assertThat(digest.date()).isEqualTo("2026-09-10");
        assertThat(digest.events()).isEmpty();
    }

    @Test
    @DisplayName("returns an empty digest when there's no JSON at all")
    void handlesNoJson() {
        DailyDigest digest = generator.parse("I could not find enough events.", today);

        assertThat(digest.isEmpty()).isTrue();
        assertThat(digest.date()).isEqualTo("2026-09-10");
    }

    @Test
    @DisplayName("returns an empty digest when the JSON is truncated / broken")
    void handlesBrokenJson() {
        DailyDigest digest = generator.parse("{\"date\":\"x\",\"events\":[{\"title\":\"cut off", today);

        assertThat(digest.isEmpty()).isTrue();
    }
}
