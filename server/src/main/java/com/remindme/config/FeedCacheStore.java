package com.remindme.config;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

// best-effort persistence for the generated feeds (digest, watchlist, local events) so a
// restart doesn't throw away a cache and force a re-pay to regenerate it. one item per feed
// in DynamoDB, since App Runner has no local disk. everything here is swallow-and-log - if
// DynamoDB is down the feeds just regenerate like before this existed
@Component
public class FeedCacheStore {

    private static final Logger log = LoggerFactory.getLogger(FeedCacheStore.class);
    private static final String TABLE_NAME = "FeedCache";

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final DynamoDbClient dynamoDbClient;
    private final Map<String, String> memory;
    private final boolean enabled;

    @Autowired
    public FeedCacheStore(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
        this.memory = null;
        this.enabled = true;
    }

    private FeedCacheStore(Map<String, String> memory, boolean enabled) {
        this.dynamoDbClient = null;
        this.memory = memory;
        this.enabled = enabled;
    }

    // never persists or reads anything
    public static FeedCacheStore disabled() {
        return new FeedCacheStore(null, false);
    }

    // backed by an in-process map, for tests that exercise the load-on-startup path
    public static FeedCacheStore inMemory() {
        return new FeedCacheStore(new ConcurrentHashMap<>(), true);
    }

    public <T> Optional<T> load(String name, Class<T> type) {
        return read(name).flatMap(json -> parse(name, json, j -> mapper.readValue(j, type)));
    }

    public <T> Optional<T> load(String name, TypeReference<T> type) {
        return read(name).flatMap(json -> parse(name, json, j -> mapper.readValue(j, type)));
    }

    public void save(String name, Object value) {
        if (!enabled) {
            return;
        }
        String json;
        try {
            json = mapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Could not serialize feed cache {}: {}", name, e.getMessage());
            return;
        }

        if (memory != null) {
            memory.put(name, json);
            return;
        }
        try {
            dynamoDbClient.putItem(PutItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .item(Map.of(
                            "Name", AttributeValue.fromS(name),
                            "Payload", AttributeValue.fromS(json),
                            "UpdatedAt", AttributeValue.fromS(Instant.now().toString())))
                    .build());
        } catch (Exception e) {
            log.warn("Could not persist feed cache {}: {}", name, e.getMessage());
        }
    }

    private Optional<String> read(String name) {
        if (!enabled) {
            return Optional.empty();
        }
        if (memory != null) {
            return Optional.ofNullable(memory.get(name));
        }
        try {
            Map<String, AttributeValue> item = dynamoDbClient.getItem(GetItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .key(Map.of("Name", AttributeValue.fromS(name)))
                    .build()).item();
            if (item == null || item.isEmpty() || item.get("Payload") == null) {
                return Optional.empty();
            }
            return Optional.of(item.get("Payload").s());
        } catch (Exception e) {
            log.warn("Could not read feed cache {}: {}", name, e.getMessage());
            return Optional.empty();
        }
    }

    private interface Parser<T> {
        T apply(String json) throws Exception;
    }

    private <T> Optional<T> parse(String name, String json, Parser<T> parser) {
        try {
            return Optional.ofNullable(parser.apply(json));
        } catch (Exception e) {
            log.warn("Ignoring unparseable feed cache {}: {}", name, e.getMessage());
            return Optional.empty();
        }
    }
}
