package com.remindme.digest;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
import com.remindme.digest.EspnScheduleClient.Fixture;
import com.remindme.evals.EvalsService;
import com.remindme.evals.GenerationOutcome;
import com.remindme.search.TavilySearchClient;
import com.remindme.search.TavilySearchResult;

// today/tomorrow digest: exact fixtures from ESPN plus a couple of web searches for everything
// else, curated into JSON by one plain Claude call (thinking off, parsed by hand - the SDK's
// structured-output helper kept blowing the token limit on this schema)
public class HybridDigestGenerator implements DigestGenerator {

    private static final Logger log = LoggerFactory.getLogger(HybridDigestGenerator.class);

    private static final String MODEL = "claude-sonnet-5";
    private static final long MAX_TOKENS = 6000L;

    private final AnthropicClient client;
    private final TavilySearchClient tavilyClient;
    private final EspnScheduleClient espnClient;
    private final EvalsService evalsService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public HybridDigestGenerator(AnthropicClient client, TavilySearchClient tavilyClient,
            EspnScheduleClient espnClient, EvalsService evalsService) {
        this.client = client;
        this.tavilyClient = tavilyClient;
        this.espnClient = espnClient;
        this.evalsService = evalsService;
    }

    @Override
    public DailyDigest generate(Instant now) {
        long start = System.currentTimeMillis();
        LocalDate today = now.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate tomorrow = today.plusDays(1);

        var sports = CompletableFuture.supplyAsync(() -> espnClient.upcoming(today, tomorrow));
        var tv = CompletableFuture.supplyAsync(() -> tavilyClient.search(
                "big TV and streaming events on " + today + " and " + tomorrow
                        + " - awards shows, major season premieres and finales, live network specials - with air times"));
        var other = CompletableFuture.supplyAsync(() -> tavilyClient.search(
                "notable live event to watch online on " + today + " or " + tomorrow
                        + " - a concert livestream, a rocket launch, a big cultural broadcast"));

        List<Fixture> fixtures = sports.join();
        TavilySearchResult tvResult = tv.join();
        TavilySearchResult otherResult = other.join();

        // nothing to curate means no reason to pay for a call
        if (fixtures.isEmpty() && !tvResult.hasResults() && !otherResult.hasResults()) {
            log.warn("No fixtures and no web results for {} - skipping the model call", today);
            evalsService.recordGeneration(EvalsService.DIGEST, GenerationOutcome.EMPTY, 0, 0, 0,
                    System.currentTimeMillis() - start);
            return DailyDigest.empty(today.toString());
        }

        String research = """
                SCHEDULED SPORTS (exact data - matchup | UTC start | competition | how to watch):
                %s

                TV / STREAMING (web search - verify against your own knowledge):
                %s

                OTHER LIVE EVENTS (web search):
                %s
                """.formatted(
                fixtures.stream().map(Fixture::line).collect(Collectors.joining("\n")),
                tvResult.formatForPrompt(),
                otherResult.formatForPrompt());

        MessageCreateParams params = MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(MAX_TOKENS)
                .thinking(ThinkingConfigDisabled.builder().build())
                .addUserMessage(prompt(today, now, research))
                .build();

        log.info("Requesting digest for {} (hybrid, now={})", today, now);

        try {
            var response = client.messages().create(params);
            double cost = UsageLogging.log(log, "Digest", MODEL, response.usage());

            String text = response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(block -> block.text())
                    .collect(Collectors.joining());

            DailyDigest digest = parse(text, today);
            evalsService.recordGeneration(EvalsService.DIGEST,
                    digest.isEmpty() ? GenerationOutcome.EMPTY : GenerationOutcome.SUCCESS, cost,
                    response.usage().inputTokens(), response.usage().outputTokens(),
                    System.currentTimeMillis() - start);

            return digest;
        } catch (RuntimeException e) {
            evalsService.recordGeneration(EvalsService.DIGEST, GenerationOutcome.FAILED, 0, 0, 0,
                    System.currentTimeMillis() - start);
            throw e;
        }
    }

    DailyDigest parse(String text, LocalDate today) {
        String json = extractJson(text);
        if (json == null) {
            log.warn("Digest response for {} had no JSON object (stopped early?): {}", today,
                    text.length() > 500 ? text.substring(0, 500) + "..." : text);
            return DailyDigest.empty(today.toString());
        }

        try {
            DailyDigest digest = objectMapper.readValue(json, DailyDigest.class);
            List<DigestEvent> events = digest.events() == null ? List.of() : digest.events();
            return new DailyDigest(today.toString(), events);
        } catch (Exception e) {
            log.warn("Digest response for {} was not parseable JSON: {}", today, e.getMessage());
            return DailyDigest.empty(today.toString());
        }
    }

    // pulls the outermost {...} out of the reply, tolerating ```json fences or stray prose
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

    private String prompt(LocalDate today, Instant now, String research) {
        LocalDate tomorrow = today.plusDays(1);
        return """
                The current time is %s (UTC). Below is what's on for %s and %s (today and \
                tomorrow). Pick the things worth watching live over that window, starting from the \
                current time.

                %s
                """.formatted(now, today, tomorrow, research)
                + DigestPromptRubric.TEXT
                + """


                        Respond with ONLY this JSON object, nothing before or after it:
                        {"date":"%s","events":[{"title":"...","summary":"...","category":"...","rating":5,\
                        "startsAt":"yyyy-MM-ddTHH:mm:ssZ","howToWatch":"...","recommendedDescription":"...",\
                        "recommendedReminderAt":"yyyy-MM-ddTHH:mm:ssZ","source":"..."}]}
                        """.formatted(today);
    }
}
