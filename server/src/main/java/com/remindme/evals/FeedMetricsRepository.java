package com.remindme.evals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

// one row per (feature, day): request count, generation count, outcome breakdown, cost and
// token totals, and summed latency - cheap to query a month of, no need to scan raw events
@Repository
public class FeedMetricsRepository {

    private static final Logger log = LoggerFactory.getLogger(FeedMetricsRepository.class);
    private static final String TABLE_NAME = "FeedMetrics";
    // keeps the table from growing forever without anyone having to remember to prune it
    private static final long RETENTION_DAYS = 180;

    private final DynamoDbClient dynamoDbClient;

    public FeedMetricsRepository(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    public void recordRequest(String feature, LocalDate day) {
        add(feature, day, Map.of("Requests", "1"));
    }

    public void recordGeneration(String feature, LocalDate day, GenerationOutcome outcome, double costUsd,
            long inputTokens, long outputTokens, long latencyMs) {
        String outcomeField = switch (outcome) {
            case SUCCESS -> "Successes";
            case EMPTY -> "Empties";
            case FAILED -> "Failures";
        };

        add(feature, day, Map.of(
                "Generations", "1",
                outcomeField, "1",
                "CostUsd", BigDecimal.valueOf(costUsd).toPlainString(),
                "InputTokens", String.valueOf(inputTokens),
                "OutputTokens", String.valueOf(outputTokens),
                "LatencyMsTotal", String.valueOf(latencyMs)));
    }

    private void add(String feature, LocalDate day, Map<String, String> increments) {
        try {
            StringBuilder updateExpression = new StringBuilder("ADD ");
            Map<String, AttributeValue> values = new java.util.HashMap<>();

            int i = 0;
            for (var entry : increments.entrySet()) {
                String placeholder = ":v" + i++;
                updateExpression.append(entry.getKey()).append(" ").append(placeholder).append(", ");
                values.put(placeholder, AttributeValue.builder().n(entry.getValue()).build());
            }
            updateExpression.setLength(updateExpression.length() - 2);
            updateExpression.append(" SET #ttl = :ttl");
            values.put(":ttl", AttributeValue.builder().n(String.valueOf(ttlFor(day))).build());

            dynamoDbClient.updateItem(UpdateItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .key(Map.of(
                            "Feature", AttributeValue.builder().s(feature).build(),
                            "Day", AttributeValue.builder().s(day.toString()).build()))
                    .updateExpression(updateExpression.toString())
                    .expressionAttributeNames(Map.of("#ttl", "TTL"))
                    .expressionAttributeValues(values)
                    .build());
        } catch (DynamoDbException e) {
            log.warn("Could not record feed metrics for {}/{}: {}", feature, day, e.getMessage());
        } catch (Exception e) {
            log.warn("Unexpected error recording feed metrics for {}/{}: {}", feature, day, e.getMessage());
        }
    }

    // every day in [from, to], oldest first - callers fill the gaps for days with no activity
    public List<DailyMetrics> findRange(String feature, LocalDate from, LocalDate to) {
        try {
            QueryResponse response = dynamoDbClient.query(QueryRequest.builder()
                    .tableName(TABLE_NAME)
                    .keyConditionExpression("Feature = :f AND #day BETWEEN :from AND :to")
                    .expressionAttributeNames(Map.of("#day", "Day"))
                    .expressionAttributeValues(Map.of(
                            ":f", AttributeValue.builder().s(feature).build(),
                            ":from", AttributeValue.builder().s(from.toString()).build(),
                            ":to", AttributeValue.builder().s(to.toString()).build()))
                    .build());

            return response.items().stream().map(this::toMetrics).toList();
        } catch (Exception e) {
            log.warn("Could not read feed metrics for {}: {}", feature, e.getMessage());
            return List.of();
        }
    }

    private DailyMetrics toMetrics(Map<String, AttributeValue> item) {
        return new DailyMetrics(
                item.get("Feature").s(),
                item.get("Day").s(),
                longAttr(item, "Requests"),
                longAttr(item, "Generations"),
                longAttr(item, "Successes"),
                longAttr(item, "Empties"),
                longAttr(item, "Failures"),
                doubleAttr(item, "CostUsd"),
                longAttr(item, "InputTokens"),
                longAttr(item, "OutputTokens"),
                longAttr(item, "LatencyMsTotal"));
    }

    private long longAttr(Map<String, AttributeValue> item, String key) {
        return (long) doubleAttr(item, key);
    }

    private double doubleAttr(Map<String, AttributeValue> item, String key) {
        AttributeValue value = item.get(key);
        return value == null || value.n() == null ? 0 : Double.parseDouble(value.n());
    }

    private static long ttlFor(LocalDate day) {
        return day.plusDays(RETENTION_DAYS).atStartOfDay(ZoneOffset.UTC).toEpochSecond();
    }
}
