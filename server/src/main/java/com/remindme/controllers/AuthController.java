package com.remindme.controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private static final String CALLBACK_PATH = "/oauth2/callback";

    String url = "https://oauth2.googleapis.com/token";

    @Value("${oauth.client_id}")
    private String clientId;

    @Value("${oauth.client_secret}")
    private String clientSecret;

    @Value("${oauth.redirect_uri}")
    private String redirectUri;

    @Value("${cors.allowed_origins}")
    private String[] allowedOrigins;

    @PostMapping("/oauth2callback")
    public ResponseEntity<Map<String, String>> exchangeCode(@RequestParam("code") String authorizationCode,
            @RequestParam(value = "redirect_uri", required = false) String requestedRedirectUri) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", authorizationCode);
        body.add("grant_type", "authorization_code");
        body.add("redirect_uri", resolveRedirectUri(requestedRedirectUri));

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

        String refreshToken = extractValueFromJson(response.getBody(), "refresh_token");
        String accessToken = extractValueFromJson(response.getBody(), "access_token");

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);

        return ResponseEntity.ok(tokens);
    }

    // Google requires the redirect_uri on the token exchange to be byte-identical to the one
    // the authorization request used. Both frontend hostnames serve the same bundle, so the
    // client sends the origin it was served from; anything we don't recognise falls back to the
    // configured value rather than being forwarded to Google unchecked.
    private String resolveRedirectUri(String requested) {
        if (requested == null || requested.isBlank()) {
            return redirectUri;
        }
        for (String origin : allowedOrigins) {
            if (requested.equals(origin.trim() + CALLBACK_PATH)) {
                return requested;
            }
        }
        log.warn("Ignoring unrecognised redirect_uri on token exchange, using the configured one");
        return redirectUri;
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<String> refreshAccessToken(@RequestParam("refresh_token") String refreshToken) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("refresh_token", refreshToken);
        body.add("grant_type", "refresh_token");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

        String newAccessToken = extractValueFromJson(response.getBody(), "access_token");

        return ResponseEntity.ok(newAccessToken);
    }

    private String extractValueFromJson(String responseBody, String key) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            return jsonNode.get(key).asText();
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse value from JSON response", e);
        }
    }
}
