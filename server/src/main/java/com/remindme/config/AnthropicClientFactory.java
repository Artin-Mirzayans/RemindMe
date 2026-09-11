package com.remindme.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;

public final class AnthropicClientFactory {

    private AnthropicClientFactory() {
    }

    public static AnthropicClient createIfConfigured(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }

        return AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }
}
