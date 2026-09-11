package com.remindme.local;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

// resolves a visitor's rough location from their IP via ip-api.com (keyless, ~45 req/min). for
// a loopback or private IP (local dev) it falls back to this server's own public IP instead
@Component
public class GeoLocationClient {

    private static final Logger log = LoggerFactory.getLogger(GeoLocationClient.class);

    private static final String ENDPOINT =
            "http://ip-api.com/json/%s?fields=status,country,regionName,city,lat,lon,timezone";

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Optional<GeoLocation> resolve(String ip) {
        String target = isPublic(ip) ? ip : "";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT.formatted(target)))
                    .timeout(Duration.ofSeconds(4))
                    .build();
            JsonNode body = objectMapper.readTree(
                    http.send(request, HttpResponse.BodyHandlers.ofString()).body());

            if (!"success".equals(body.path("status").asText())) {
                return Optional.empty();
            }
            return Optional.of(new GeoLocation(
                    body.path("city").asText(null),
                    body.path("regionName").asText(null),
                    body.path("country").asText(null),
                    body.path("lat").asDouble(),
                    body.path("lon").asDouble(),
                    body.path("timezone").asText(null)));
        } catch (Exception e) {
            log.warn("Geo lookup failed for '{}': {}", ip, e.getMessage());
            return Optional.empty();
        }
    }

    private boolean isPublic(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        return !ip.startsWith("127.") && !ip.startsWith("10.") && !ip.startsWith("192.168.")
                && !ip.startsWith("172.16.") && !ip.startsWith("::1") && !ip.equalsIgnoreCase("localhost")
                && !ip.startsWith("0:0:0:0");
    }
}
