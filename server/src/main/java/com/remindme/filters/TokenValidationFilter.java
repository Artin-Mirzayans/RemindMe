package com.remindme.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;

public class TokenValidationFilter extends OncePerRequestFilter {

    private final Environment environment;

    public TokenValidationFilter(Environment environment) {
        this.environment = environment;
    }

    private static final String TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo?access_token=";

    @Value("${oauth.client_id}")
    private String clientId;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = request.getHeader("Authorization");

        if (token == null || !token.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Unauthorized: Missing token");
            return;
        }

        String actualToken = token.substring(7);

        if (!isValidToken(actualToken, request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Unauthorized: Invalid token");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isValidToken(String token, HttpServletRequest request) {
        try {
            String url = TOKEN_INFO_URL + token;
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, null, String.class);
            String responseBody = response.getBody();
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode tokenInfo = objectMapper.readTree(responseBody);

            String clientId = environment.getProperty("oauth.client_id");
            String aud = tokenInfo.get("aud").asText();
            long exp = tokenInfo.get("exp").asLong();

            if (clientId != null && !clientId.equals(aud))
                throw new RuntimeException("Invalid clientId");

            if (Instant.now().getEpochSecond() > exp)
                throw new RuntimeException("Token has expired");

            String email = tokenInfo.get("email").asText();
            request.setAttribute("userId", email);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}