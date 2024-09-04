package com.remindme.models;

import java.util.HashMap;
import java.util.Map;
import java.time.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import com.exception.FutureDateTime;

public class Reminder {

    @NotNull(message = "UserId cannot be null")
    private String userId;

    @NotNull(message = "DateTime cannot be null")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$", message = "DateTime must be a valid UTC formatted time")
    @FutureDateTime(message = "DateTime must be a valid UTC formatted time and in the future")
    private String dateTime;

    @NotNull(message = "Description cannot be null")
    @Size(min = 3, message = "Description must be at least 3 characters long")
    @Size(max = 20, message = "Description must be at most 20 characters long")
    private String description;

    @NotNull(message = "ContactMethod cannot be null")
    @Pattern(regexp = "^(Email|Text)$", message = "ContactMethod must be 'Email' or 'Text'")
    private String contactMethod;

    public Reminder(String userId, String dateTime, String contactMethod, String description) {
        this.userId = userId;
        this.dateTime = dateTime;
        this.description = description;
        this.contactMethod = contactMethod;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContactMethod() {
        return contactMethod;
    }

    public void setContactMethod(String contactMethod) {
        this.contactMethod = contactMethod;
    }

    private Long getTTL() {
        OffsetDateTime odt = OffsetDateTime.parse(this.dateTime);
        long epoch = odt.toEpochSecond();
        return epoch;

    }

    public Map<String, AttributeValue> toItem(String userId) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("UserId", AttributeValue.builder().s(userId).build());
        item.put("DateTime", AttributeValue.builder().s(dateTime).build());
        item.put("Description", AttributeValue.builder().s(description).build());
        item.put("ContactMethod", AttributeValue.builder().s(contactMethod).build());
        item.put("TTL", AttributeValue.builder().n(Long.toString(getTTL())).build());
        return item;
    }
}
