package com.remindme.repositories;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import org.springframework.stereotype.Repository;
import com.remindme.models.User;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Repository
public class UserRepository {

    private final DynamoDbClient dynamoDbClient;
    private static final String TABLE_NAME = "Users";
    private static final int MAX_REQUESTS = 10;
    private static final long COOLDOWN_PERIOD_MS = 30 * 1000;

    public UserRepository(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    public User getUser(String userId) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("Email", AttributeValue.builder().s(userId).build());

        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(TABLE_NAME)
                .keyConditionExpression("Email = :email")
                .expressionAttributeValues(Map.of(":email", AttributeValue.builder().s(userId).build()))
                .build();

        QueryResponse response = dynamoDbClient.query(queryRequest);

        if (response.hasItems() && !response.items().isEmpty()) {
            Map<String, AttributeValue> item = response.items().get(0);
            return new User(item);
        }
        return null;
    }

    public boolean createOrUpdateUser(String phoneNumber, String otpCode, String userId) {
        User user = getUser(userId);

        long currentTime = Instant.now().toEpochMilli();

        if (user != null) {
            if (user.isVerified()) {
                return false;
            }

            if (user.getSmsRequestCount() >= MAX_REQUESTS) {
                return false;
            }

            if (user.getLastOtpSentTimestamp() + COOLDOWN_PERIOD_MS > currentTime) {
                return false;
            }
        } else {
            user = new User();
        }

        Map<String, AttributeValue> key = new HashMap<>();
        key.put("Email", AttributeValue.builder().s(userId).build());

        Map<String, AttributeValue> updateValues = new HashMap<>();
        updateValues.put(":phone", AttributeValue.builder().s(phoneNumber).build());
        updateValues.put(":otp", AttributeValue.builder().s(otpCode).build());
        updateValues.put(":inc", AttributeValue.builder().n("1").build());
        updateValues.put(":verified", AttributeValue.builder().bool(false).build());
        updateValues.put(":lastSent", AttributeValue.builder().n(String.valueOf(currentTime)).build());
        updateValues.put(":zero", AttributeValue.builder().n("0").build());

        UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .updateExpression(
                        "SET PhoneNumber = :phone, OtpCode = :otp, SmsRequestCount = if_not_exists(SmsRequestCount, :zero) + :inc, IsVerified = :verified, LastOtpSentTimestamp = :lastSent")
                .expressionAttributeValues(updateValues)
                .build();

        try {
            dynamoDbClient.updateItem(updateItemRequest);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateVerificationStatus(String userId, boolean isVerified) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("Email", AttributeValue.builder().s(userId).build());

        Map<String, AttributeValue> updateValues = new HashMap<>();
        updateValues.put(":verified", AttributeValue.builder().bool(isVerified).build());

        UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .updateExpression("SET IsVerified = :verified")
                .expressionAttributeValues(Map.of(":verified", AttributeValue.builder().bool(isVerified).build()))
                .build();

        try {
            dynamoDbClient.updateItem(updateItemRequest);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
