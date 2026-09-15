package com.remindme.watchlist;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.ThinkingConfigDisabled;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.remindme.config.UsageLogging;
import com.remindme.evals.EvalsService;
import com.remindme.evals.GenerationOutcome;
import com.remindme.search.TavilySearchClient;
import com.remindme.search.TavilySearchResult;

// gathers research via a handful of Tavily queries, then one plain Claude call curates it into
// JSON (parsed by hand). thinking stays off - with it on, Sonnet burns the whole budget
// reasoning and never actually emits the answer
public class TavilyWatchlistGenerator implements WatchlistGenerator {

    private static final Logger log = LoggerFactory.getLogger(TavilyWatchlistGenerator.class);

    private static final String MODEL = "claude-sonnet-5";
    private static final long MAX_TOKENS = 12000L;

    private static final List<String> QUERY_TEMPLATES = List.of(
            "biggest sports games, playoff games, and title matches between %s and %s",
            "major boxing or MMA fight cards between %s and %s",
            "chess or major mind-sport tournament schedule between %s and %s",
            "movie premieres and major theatrical releases between %s and %s",
            "television show premieres and finales between %s and %s",
            "major stand-up comedy specials or live comedy show events between %s and %s",
            "WWE or AEW wrestling premium live event schedule between %s and %s",
            "major awards show schedule between %s and %s",
            "rocket launch schedule NASA SpaceX between %s and %s");

    private final AnthropicClient client;
    private final TavilySearchClient tavilyClient;
    private final EvalsService evalsService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public TavilyWatchlistGenerator(AnthropicClient client, TavilySearchClient tavilyClient,
            EvalsService evalsService) {
        this.client = client;
        this.tavilyClient = tavilyClient;
        this.evalsService = evalsService;
    }

    @Override
    public WatchlistWindow generate(LocalDate windowStart, LocalDate windowEnd) {
        long start = System.currentTimeMillis();
        List<TavilySearchResult> results = QUERY_TEMPLATES.stream()
                .map(template -> template.formatted(windowStart, windowEnd))
                .map(query -> CompletableFuture.supplyAsync(() -> tavilyClient.search(query)))
                .toList()
                .stream()
                .map(CompletableFuture::join)
                .toList();

        // every query came back empty (Tavily down, or no key) - nothing to curate
        if (results.stream().noneMatch(TavilySearchResult::hasResults)) {
            log.warn("No web results for the {}..{} watchlist - skipping the model call", windowStart, windowEnd);
            evalsService.recordGeneration(EvalsService.WATCHLIST, GenerationOutcome.EMPTY, 0, 0, 0,
                    System.currentTimeMillis() - start);
            return WatchlistWindow.empty(windowStart.toString(), windowEnd.toString());
        }

        String research = results.stream()
                .map(TavilySearchResult::formatForPrompt)
                .collect(Collectors.joining("\n"));

        MessageCreateParams params = MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(MAX_TOKENS)
                .thinking(ThinkingConfigDisabled.builder().build())
                .addUserMessage(prompt(windowStart, windowEnd, research))
                .build();

        log.info("Requesting watchlist for {}..{} (Tavily-backed)", windowStart, windowEnd);

        try {
            var response = client.messages().create(params);
            double cost = UsageLogging.log(log, "Watchlist", MODEL, response.usage());

            String text = response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(block -> block.text())
                    .collect(Collectors.joining());

            WatchlistWindow window = parse(text, windowStart, windowEnd);
            evalsService.recordGeneration(EvalsService.WATCHLIST,
                    window.isEmpty() ? GenerationOutcome.EMPTY : GenerationOutcome.SUCCESS, cost,
                    response.usage().inputTokens(), response.usage().outputTokens(),
                    System.currentTimeMillis() - start);

            return window;
        } catch (RuntimeException e) {
            evalsService.recordGeneration(EvalsService.WATCHLIST, GenerationOutcome.FAILED, 0, 0, 0,
                    System.currentTimeMillis() - start);
            throw e;
        }
    }

    WatchlistWindow parse(String text, LocalDate windowStart, LocalDate windowEnd) {
        String json = extractJson(text);
        if (json == null) {
            log.warn("Watchlist response had no JSON object: {}",
                    text != null && text.length() > 500 ? text.substring(0, 500) + "..." : text);
            return WatchlistWindow.empty(windowStart.toString(), windowEnd.toString());
        }

        try {
            WatchlistWindow parsed = objectMapper.readValue(json, WatchlistWindow.class);
            List<WatchlistEvent> events = parsed.events() == null ? List.of() : parsed.events();
            // use the requested dates, not whatever the model echoed back - WatchlistService's
            // cache-refresh math depends on these being exact
            return new WatchlistWindow(windowStart.toString(), windowEnd.toString(), events, null);
        } catch (Exception e) {
            log.warn("Watchlist response was not parseable JSON: {}", e.getMessage());
            return WatchlistWindow.empty(windowStart.toString(), windowEnd.toString());
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

    private String prompt(LocalDate windowStart, LocalDate windowEnd, String research) {
        return """
                Below is research about what's happening between %s and %s. Pick 8 to 20 events \
                worth putting on a global watchlist.

                Research:
                %s

                Treat the research as your primary source - don't invent specific events or dates \
                it doesn't support. You may use general knowledge for context (e.g. a league's \
                usual broadcaster), but when the research doesn't confirm a date or time, use \
                expectedWindow rather than guessing.

                """.formatted(windowStart, windowEnd, research)
                + WatchlistPromptRubric.TEXT
                + """


                        Respond with ONLY this JSON object, nothing before or after it:
                        {"windowStart":"%s","windowEnd":"%s","events":[{"title":"...","summary":"...",\
                        "category":"...","rating":5,"startsAt":"yyyy-MM-ddTHH:mm:ssZ or null",\
                        "expectedWindow":"... or null","source":"... or null","recommendedDescription":"...",\
                        "recommendedReminderAt":"yyyy-MM-ddTHH:mm:ssZ","howToWatch":"... or null"}]}
                        """.formatted(windowStart, windowEnd);
    }
}
