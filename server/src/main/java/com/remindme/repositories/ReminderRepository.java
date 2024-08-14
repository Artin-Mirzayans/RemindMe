package com.remindme.repositories;

import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.utils.ImmutableMap;

import com.remindme.models.Reminder;

@Repository
public class ReminderRepository {

    private final DynamoDbClient dynamoDbClient;
    private static final String tableName = "Reminders";

    public ReminderRepository(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    public boolean saveReminder(Reminder reminder) {
        try {
            PutItemRequest request = PutItemRequest.builder()
                    .tableName(tableName)
                    .item(reminder.toItem())
                    .build();

            PutItemResponse response = dynamoDbClient.putItem(request);
            return response.sdkHttpResponse().isSuccessful();

        } catch (DynamoDbException e) {
            System.err.println("DynamoDB Error: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            return false;
        }

    }

    public List<Reminder> findAll(String UserId) {
        final long currentTime = System.currentTimeMillis() / 1000;

        final String keyConditionExpression = "#pk = :pk";

        final String filterExpression = "#ttl > :currentTime";

        final Map<String, String> expressionAttributeNames = ImmutableMap.of(
                "#pk", "UserId",
                "#ttl", "TTL");

        final Map<String, AttributeValue> expressionAttributeValues = ImmutableMap.of(
                ":pk", AttributeValue.builder().s(UserId).build(),
                ":currentTime", AttributeValue.builder().n(String.valueOf(currentTime)).build());

        final QueryRequest request = QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression(keyConditionExpression)
                .filterExpression(filterExpression)
                .expressionAttributeNames(expressionAttributeNames)
                .expressionAttributeValues(expressionAttributeValues)
                .scanIndexForward(true)
                .build();

        try {
            QueryResponse response = dynamoDbClient.query(request);
            return response.items().stream()
                    .map(this::mapToReminder)
                    .collect(Collectors.toList());
        } catch (DynamoDbException e) {
            System.err.println("DynamoDB Error: " + e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private Reminder mapToReminder(Map<String, AttributeValue> item) {
        String userId = item.get("UserId").s();
        String dateTime = item.get("DateTime").s();
        String contactMethod = item.get("ContactMethod").s();
        String description = item.get("Description").s();

        return new Reminder(userId, dateTime, contactMethod, description);
    }
}
