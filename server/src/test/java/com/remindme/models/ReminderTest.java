package com.remindme.models;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

class ReminderTest {

    @Test
    @DisplayName("writes the authenticated user id rather than whatever the body carried")
    void itemUsesSuppliedUserId() {
        Reminder reminder = new Reminder("spoofed@example.com", "2030-01-01T09:00:00Z", "Email", "call mum");

        Map<String, AttributeValue> item = reminder.toItem("real@example.com");

        assertThat(item.get("UserId").s()).isEqualTo("real@example.com");
    }

    @Test
    @DisplayName("stores a TTL matching the reminder instant")
    void itemCarriesTtlForTheReminderInstant() {
        String when = "2030-01-01T09:00:00Z";
        Reminder reminder = new Reminder("someone@example.com", when, "Text", "call mum");

        Map<String, AttributeValue> item = reminder.toItem("someone@example.com");

        assertThat(item.get("TTL").n())
                .isEqualTo(Long.toString(OffsetDateTime.parse(when).toEpochSecond()));
    }

    @Test
    @DisplayName("carries every attribute the repository reads back")
    void itemContainsAllReadAttributes() {
        Reminder reminder = new Reminder("someone@example.com", "2030-01-01T09:00:00Z", "Text", "call mum");

        assertThat(reminder.toItem("someone@example.com"))
                .containsKeys("UserId", "DateTime", "Description", "ContactMethod", "TTL");
    }
}
