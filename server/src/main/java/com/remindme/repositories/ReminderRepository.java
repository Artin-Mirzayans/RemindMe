package com.remindme.repositories;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;
import software.amazon.awssdk.utils.ImmutableMap;

import com.remindme.models.Reminder;

@Repository
public class ReminderRepository {

    private static final Logger log = LoggerFactory.getLogger(ReminderRepository.class);

    private final DynamoDbClient dynamoDbClient;
    private static final String tableName = "Reminders";

    public ReminderRepository(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    // the key is (UserId, DateTime) only, no ContactMethod - without this check, a Text and an
    // Email reminder at the same instant would silently overwrite each other
    public boolean saveReminder(Reminder reminder, String userId) {
        try {
            PutItemRequest request = PutItemRequest.builder()
                    .tableName(tableName)
                    .item(reminder.toItem(userId))
                    .conditionExpression("attribute_not_exists(UserId)")
                    .build();

            PutItemResponse response = dynamoDbClient.putItem(request);
            return response.sdkHttpResponse().isSuccessful();

        } catch (ConditionalCheckFailedException e) {
            log.warn("Reminder already exists for user={} at={}, refusing to overwrite it", userId,
                    reminder.getDateTime());
            return false;
        } catch (DynamoDbException e) {
            log.error("DynamoDB error", e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected repository error", e);
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
            log.error("DynamoDB error", e);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Unexpected repository error", e);
            return Collections.emptyList();
        }
    }

    public boolean deleteReminder(String userId, String dateTime) {
        try {
            DeleteItemRequest request = DeleteItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of(
                            "UserId", AttributeValue.builder().s(userId).build(),
                            "DateTime", AttributeValue.builder().s(dateTime).build()))
                    .build();

            DeleteItemResponse response = dynamoDbClient.deleteItem(request);

            return response.sdkHttpResponse().isSuccessful();
        } catch (DynamoDbException e) {
            log.error("DynamoDB error", e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected repository error", e);
            return false;
        }
    }

    // used when editing a reminder in place, since the schedule doesn't carry the description
    // and the condition stops an edit from resurrecting a reminder that's already gone
    public boolean updateDescription(String userId, String dateTime, String description) {
        try {
            UpdateItemRequest request = UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of(
                            "UserId", AttributeValue.builder().s(userId).build(),
                            "DateTime", AttributeValue.builder().s(dateTime).build()))
                    .updateExpression("SET Description = :d")
                    .conditionExpression("attribute_exists(UserId)")
                    .expressionAttributeValues(Map.of(
                            ":d", AttributeValue.builder().s(description).build()))
                    .build();

            UpdateItemResponse response = dynamoDbClient.updateItem(request);
            return response.sdkHttpResponse().isSuccessful();
        } catch (ConditionalCheckFailedException e) {
            log.info("Update skipped, reminder no longer exists for user={} at={}", userId, dateTime);
            return false;
        } catch (DynamoDbException e) {
            log.error("DynamoDB error", e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected repository error", e);
            return false;
        }
    }

    private Reminder mapToReminder(Map<String, AttributeValue> item) {
        String userId = item.get("UserId").s();
        String dateTime = item.get("DateTime").s();
        String contactMethod = item.get("ContactMethod").s();
        String description = item.get("Description").s();

        Reminder reminder = new Reminder(userId, dateTime, contactMethod, description);
        if (item.containsKey("SeriesId")) {
            reminder.setSeriesId(item.get("SeriesId").s());
        }
        return reminder;
    }
}
