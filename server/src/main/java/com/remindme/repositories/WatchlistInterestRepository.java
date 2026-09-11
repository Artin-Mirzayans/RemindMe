package com.remindme.repositories;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

@Repository
public class WatchlistInterestRepository {

    private static final Logger log = LoggerFactory.getLogger(WatchlistInterestRepository.class);

    private static final String TABLE_NAME = "WatchlistInterests";

    private final DynamoDbClient dynamoDbClient;

    public WatchlistInterestRepository(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    public boolean recordInterest(String userId, String category) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("UserId", AttributeValue.builder().s(userId).build());
        key.put("Category", AttributeValue.builder().s(category).build());

        Map<String, AttributeValue> updateValues = new HashMap<>();
        updateValues.put(":inc", AttributeValue.builder().n("1").build());
        updateValues.put(":zero", AttributeValue.builder().n("0").build());

        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .updateExpression("SET ClickCount = if_not_exists(ClickCount, :zero) + :inc")
                .expressionAttributeValues(updateValues)
                .build();

        try {
            dynamoDbClient.updateItem(request);
            return true;
        } catch (DynamoDbException e) {
            log.error("DynamoDB error recording watchlist interest", e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected repository error recording watchlist interest", e);
            return false;
        }
    }

    public Map<String, Integer> getInterestCounts(String userId) {
        QueryRequest request = QueryRequest.builder()
                .tableName(TABLE_NAME)
                .keyConditionExpression("UserId = :userId")
                .expressionAttributeValues(Map.of(":userId", AttributeValue.builder().s(userId).build()))
                .build();

        try {
            QueryResponse response = dynamoDbClient.query(request);
            return response.items().stream()
                    .collect(Collectors.toMap(
                            item -> item.get("Category").s(),
                            item -> Integer.parseInt(item.get("ClickCount").n())));
        } catch (DynamoDbException e) {
            log.error("DynamoDB error reading watchlist interests", e);
            return Map.of();
        } catch (Exception e) {
            log.error("Unexpected repository error reading watchlist interests", e);
            return Map.of();
        }
    }
}
