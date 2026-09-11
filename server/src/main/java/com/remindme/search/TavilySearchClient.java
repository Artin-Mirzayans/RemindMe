package com.remindme.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TavilySearchClient {

    private static final Logger log = LoggerFactory.getLogger(TavilySearchClient.class);

    private static final String SEARCH_URL = "https://api.tavily.com/search";
    private static final int MAX_RESULTS = 4;

    private final String apiKey;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TavilySearchClient(String apiKey) {
        this.apiKey = apiKey;
    }

    public TavilySearchResult search(String query) {
        return search(query, "general");
    }

    public TavilySearchResult search(String query, String topic) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("query", query);
            body.put("topic", topic);
            body.put("search_depth", "basic");
            body.put("max_results", MAX_RESULTS);
            body.put("include_answer", false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            ResponseEntity<String> response = restTemplate.postForEntity(SEARCH_URL,
                    new HttpEntity<>(body, headers), String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String answer = root.hasNonNull("answer") ? root.get("answer").asText() : null;

            List<TavilyResult> results = new ArrayList<>();
            if (root.has("results")) {
                for (JsonNode item : root.get("results")) {
                    results.add(new TavilyResult(
                            item.path("title").asText(""),
                            item.path("url").asText(""),
                            item.path("content").asText("")));
                }
            }

            return new TavilySearchResult(query, answer, results);
        } catch (Exception e) {
            log.warn("Tavily search failed for query '{}': {}", query, e.getMessage());
            return new TavilySearchResult(query, null, List.of());
        }
    }
}
