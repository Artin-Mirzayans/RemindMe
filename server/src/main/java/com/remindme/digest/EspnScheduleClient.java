package com.remindme.digest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

// pulls exact fixtures (matchup, UTC time, broadcaster) from ESPN's public scoreboard API, so
// the model isn't guessing at opponents or timezones. undocumented but stable, and ESPN's edge
// 403s browser-like user agents but allows curl, hence the UA below
public class EspnScheduleClient {

    private static final Logger log = LoggerFactory.getLogger(EspnScheduleClient.class);

    private static final String BASE = "https://site.api.espn.com/apis/site/v2/sports/";
    private static final String USER_AGENT = "curl/8.5.0";
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    // sport/league path segments worth scanning for marquee, nationally-relevant events
    private static final List<String> LEAGUES = List.of(
            "soccer/uefa.champions", "soccer/uefa.europa", "soccer/eng.1", "soccer/esp.1",
            "soccer/ita.1", "soccer/ger.1", "soccer/usa.1", "football/nfl", "football/college-football",
            "basketball/nba", "basketball/wnba", "hockey/nhl", "baseball/mlb", "racing/f1",
            "mma/ufc", "golf/pga");

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // e.g. "Bodo/Glimt at Bayern Munich | 2026-09-10T19:00:00Z | UEFA Champions League | Paramount+"
    record Fixture(String matchup, String startsAt, String competition, String watch) {
        String line() {
            return matchup + " | " + startsAt + " | " + competition
                    + (watch == null || watch.isBlank() ? "" : " | " + watch);
        }
    }

    public List<Fixture> upcoming(LocalDate today, LocalDate tomorrow) {
        List<String> dates = List.of(today.format(YMD), tomorrow.format(YMD));
        ExecutorService pool = Executors.newFixedThreadPool(16);
        try {
            List<CompletableFuture<List<Fixture>>> futures = new ArrayList<>();
            for (String league : LEAGUES) {
                for (String date : dates) {
                    futures.add(CompletableFuture.supplyAsync(() -> fetch(league, date), pool));
                }
            }
            return futures.stream().flatMap(f -> f.join().stream()).distinct().toList();
        } finally {
            pool.shutdown();
        }
    }

    private List<Fixture> fetch(String league, String date) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE + league + "/scoreboard?dates=" + date))
                    .header("User-Agent", USER_AGENT)
                    .timeout(Duration.ofSeconds(8))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("ESPN {} {} returned {}", league, date, response.statusCode());
                return List.of();
            }
            return parseScoreboard(objectMapper.readTree(response.body()), league);
        } catch (Exception e) {
            log.warn("ESPN fetch failed for {} {}: {}", league, date, e.getMessage());
            return List.of();
        }
    }

    List<Fixture> parseScoreboard(JsonNode root, String league) {
        String competition = root.path("leagues").path(0).path("name").asText(league);
        boolean nationalOnly = "baseball/mlb".equals(league);

        List<Fixture> fixtures = new ArrayList<>();
        for (JsonNode event : root.path("events")) {
            if (!"pre".equals(event.path("status").path("type").path("state").asText())) {
                continue;
            }
            String watch = broadcast(event, nationalOnly);
            if (nationalOnly && watch == null) {
                continue;
            }
            fixtures.add(new Fixture(
                    event.path("name").asText(),
                    normalizeInstant(event.path("date").asText()),
                    competition,
                    watch));
        }
        return fixtures;
    }

    private static final List<String> NATIONAL_NETWORKS = List.of(
            "ESPN", "FOX", "FS1", "TBS", "TNT", "MLB Network", "Apple TV", "Roku", "NBC", "Peacock",
            "ABC", "CBS", "Amazon", "Prime", "Netflix");

    private String broadcast(JsonNode event, boolean nationalOnly) {
        List<String> names = new ArrayList<>();
        for (JsonNode b : event.path("competitions").path(0).path("broadcasts")) {
            for (JsonNode n : b.path("names")) {
                names.add(n.asText());
            }
        }
        if (nationalOnly && names.stream().noneMatch(
                n -> NATIONAL_NETWORKS.stream().anyMatch(net -> n.toLowerCase().contains(net.toLowerCase())))) {
            return null;
        }
        return names.isEmpty() ? null : String.join(", ", names);
    }

    // ESPN returns e.g. "2026-09-10T19:00Z" - pad it to "2026-09-10T19:00:00Z"
    private String normalizeInstant(String raw) {
        if (raw != null && raw.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}Z")) {
            return raw.substring(0, 16) + ":00Z";
        }
        return raw;
    }
}
