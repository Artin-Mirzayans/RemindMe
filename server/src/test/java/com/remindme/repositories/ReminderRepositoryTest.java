package com.remindme.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.remindme.models.Reminder;

import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

@ExtendWith(MockitoExtension.class)
class ReminderRepositoryTest {

    private static final String USER = "someone@example.com";
    private static final String WHEN = "2030-01-01T09:00:00Z";

    @Mock
    private DynamoDbClient dynamoDbClient;

    private ReminderRepository repository() {
        return new ReminderRepository(dynamoDbClient);
    }

    private PutItemResponse successfulPut() {
        PutItemResponse response = mock(PutItemResponse.class);
        doReturn(SdkHttpResponse.builder().statusCode(200).build()).when(response).sdkHttpResponse();
        return response;
    }

    private UpdateItemResponse successfulUpdate() {
        UpdateItemResponse response = mock(UpdateItemResponse.class);
        doReturn(SdkHttpResponse.builder().statusCode(200).build()).when(response).sdkHttpResponse();
        return response;
    }

    @Test
    @DisplayName("saves a reminder that doesn't already occupy that slot")
    void savesANewReminder() {
        PutItemResponse response = successfulPut();
        when(dynamoDbClient.putItem(any(PutItemRequest.class))).thenReturn(response);

        assertThat(repository().saveReminder(new Reminder(USER, WHEN, "Text", "call mum"), USER)).isTrue();
    }

    @Test
    @DisplayName("guards the write so one contact method can never silently overwrite another's slot")
    void savingIncludesAConditionAgainstOverwriting() {
        PutItemResponse response = successfulPut();
        when(dynamoDbClient.putItem(any(PutItemRequest.class))).thenReturn(response);

        repository().saveReminder(new Reminder(USER, WHEN, "Text", "call mum"), USER);

        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(dynamoDbClient).putItem(captor.capture());
        assertThat(captor.getValue().conditionExpression()).isEqualTo("attribute_not_exists(UserId)");
    }

    @Test
    @DisplayName("refuses to save over an existing reminder at the same (UserId, DateTime) slot")
    void refusesToOverwriteAnExistingReminder() {
        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenThrow(ConditionalCheckFailedException.builder().message("exists").build());

        // A Text reminder already occupies this exact instant; an Email reminder for the same
        // user at the same instant would otherwise land on the same DynamoDB key and clobber it.
        boolean saved = repository().saveReminder(new Reminder(USER, WHEN, "Email", "pay rent"), USER);

        assertThat(saved).isFalse();
    }

    @Test
    @DisplayName("rewrites a description in place when the reminder still exists")
    void updatesDescriptionWhenReminderExists() {
        UpdateItemResponse response = successfulUpdate();
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class))).thenReturn(response);

        assertThat(repository().updateDescription(USER, WHEN, "call mum back")).isTrue();
    }

    @Test
    @DisplayName("does not resurrect a reminder that no longer exists when editing its description")
    void updateDescriptionFailsWhenReminderIsGone() {
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class)))
                .thenThrow(ConditionalCheckFailedException.builder().message("gone").build());

        assertThat(repository().updateDescription(USER, WHEN, "call mum back")).isFalse();
    }
}
