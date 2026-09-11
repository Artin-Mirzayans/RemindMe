package com.remindme.local;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

// pulls ticketed events near a location from Ticketmaster's Discovery API, sorted by relevance
// so arena headliners and pro games come first rather than club shows
public class TicketmasterClient {

    private static final Logger log = LoggerFactory.getLogger(TicketmasterClient.class);

    private static final String BASE = "https://app.ticketmaster.com/discovery/v2/events.json";
    private static final String SEGMENTS = "Music,Sports,Arts & Theatre";
    private static final int SIZE = 100;
    private static final int RADIUS_MILES = 45;

    private final String apiKey;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TicketmasterClient(String apiKey) {
        this.apiKey = apiKey;
    }

    // e.g. "Robyn - The Sexistential Tour | 2026-09-23T02:00:00Z | Kia Forum, Inglewood | Music/Pop | $55+ | https://..."
    record Event(String name, String startsAt, String venue, String genre, String priceFrom, String url) {
        String line() {
            return String.join(" | ", name, startsAt, venue,
                    genre == null ? "" : genre,
                    priceFrom == null ? "" : priceFrom,
                    url == null ? "" : url);
        }
    }

    public List<Event> near(GeoLocation location, LocalDate windowStart, LocalDate windowEnd) {
        try {
            StringBuilder url = new StringBuilder(BASE)
                    .append("?apikey=").append(apiKey)
                    .append("&segmentName=").append(enc(SEGMENTS))
                    .append("&startDateTime=").append(windowStart).append("T00:00:00Z")
                    .append("&endDateTime=").append(windowEnd).append("T00:00:00Z")
                    .append("&sort=relevance,desc&size=").append(SIZE);

            if (location.lat() != 0 || location.lon() != 0) {
                url.append("&latlong=").append(location.lat()).append(",").append(location.lon())
                        .append("&radius=").append(RADIUS_MILES).append("&unit=miles");
            } else if (location.city() != null && !location.city().isBlank()) {
                url.append("&city=").append(enc(location.city()));
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url.toString()))
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Ticketmaster returned {} for {}", response.statusCode(), location.label());
                return List.of();
            }
            return parse(objectMapper.readTree(response.body()));
        } catch (Exception e) {
            log.warn("Ticketmaster fetch failed for {}: {}", location.label(), e.getMessage());
            return List.of();
        }
    }

    List<Event> parse(JsonNode root) {
        List<Event> events = new ArrayList<>();
        for (JsonNode e : root.path("_embedded").path("events")) {
            String startsAt = e.path("dates").path("start").path("dateTime").asText(null);
            if (startsAt == null) {
                continue;
            }
            JsonNode venue = e.path("_embedded").path("venues").path(0);
            String venueLabel = venue.path("name").asText("") + venueCity(venue);

            JsonNode classification = e.path("classifications").path(0);
            String segment = classification.path("segment").path("name").asText("");
            String genre = classification.path("genre").path("name").asText("");
            String genreLabel = genre.isBlank() || genre.equals("Undefined") ? segment : segment + "/" + genre;

            JsonNode price = e.path("priceRanges").path(0);
            String priceFrom = price.isMissingNode() ? null : "$" + Math.round(price.path("min").asDouble()) + "+";

            events.add(new Event(e.path("name").asText(), startsAt, venueLabel.trim(), genreLabel, priceFrom,
                    e.path("url").asText(null)));
        }
        return events;
    }

    private String venueCity(JsonNode venue) {
        String city = venue.path("city").path("name").asText("");
        return city.isBlank() ? "" : ", " + city;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
