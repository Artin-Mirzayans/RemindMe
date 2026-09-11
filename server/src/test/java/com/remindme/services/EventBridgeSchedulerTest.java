package com.remindme.services;

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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.remindme.models.Reminder;
import com.target.TargetFactory;

import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.CreateScheduleRequest;
import software.amazon.awssdk.services.scheduler.model.CreateScheduleResponse;
import software.amazon.awssdk.services.scheduler.model.DeleteScheduleRequest;
import software.amazon.awssdk.services.scheduler.model.ResourceNotFoundException;
import software.amazon.awssdk.services.scheduler.model.SchedulerException;
import software.amazon.awssdk.services.scheduler.model.Target;

@ExtendWith(MockitoExtension.class)
class EventBridgeSchedulerTest {

    private static final String USER = "someone@example.com";
    private static final String WHEN = "2030-01-01T09:00:00Z";

    @Mock
    private SchedulerClient schedulerClient;

    @Mock
    private TargetFactory targetFactory;

    @InjectMocks
    private EventBridgeScheduler scheduler;

    private CreateScheduleResponse successfulResponse() {
        CreateScheduleResponse response = mock(CreateScheduleResponse.class);
        doReturn(SdkHttpResponse.builder().statusCode(200).build()).when(response).sdkHttpResponse();
        return response;
    }

    @Test
    @DisplayName("builds a schedule name that survives round-tripping through delete")
    void scheduleNameIsStableAcrossCreateAndDelete() {
        String name = EventBridgeScheduler.scheduleName(USER, "Text", WHEN);

        assertThat(name).isEqualTo("someone-example.com-Text-2030-01-01T09.00.00Z");
        assertThat(name).doesNotContain("@").doesNotContain(":");
    }

    @Test
    @DisplayName("strips the trailing Z from the at() expression")
    void createsScheduleWithoutTrailingZulu() {
        CreateScheduleResponse response = successfulResponse();
        when(targetFactory.createLambdaTarget("Text")).thenReturn(Target.builder().arn("arn:lambda").build());
        when(schedulerClient.createSchedule(any(CreateScheduleRequest.class))).thenReturn(response);

        boolean created = scheduler.createSchedule(new Reminder(USER, WHEN, "Text", "call mum"), USER);

        assertThat(created).isTrue();

        ArgumentCaptor<CreateScheduleRequest> captor = ArgumentCaptor.forClass(CreateScheduleRequest.class);
        verify(schedulerClient).createSchedule(captor.capture());

        assertThat(captor.getValue().scheduleExpression()).isEqualTo("at(2030-01-01T09:00:00)");
        assertThat(captor.getValue().groupName()).isEqualTo("Text");
    }

    @Test
    @DisplayName("reports failure when the scheduler rejects the request")
    void returnsFalseWhenCreateThrows() {
        when(targetFactory.createLambdaTarget("Text")).thenReturn(Target.builder().arn("arn:lambda").build());
        when(schedulerClient.createSchedule(any(CreateScheduleRequest.class)))
                .thenThrow(SchedulerException.builder().message("boom").build());

        assertThat(scheduler.createSchedule(new Reminder(USER, WHEN, "Text", "call mum"), USER)).isFalse();
    }

    @Test
    @DisplayName("reports failure when the contact method has no lambda target")
    void returnsFalseForUnknownContactMethod() {
        when(targetFactory.createLambdaTarget("Carrier Pigeon"))
                .thenThrow(new IllegalArgumentException("Unknown event type"));

        assertThat(scheduler.createSchedule(new Reminder(USER, WHEN, "Carrier Pigeon", "call mum"), USER)).isFalse();
    }

    @Test
    @DisplayName("treats an already-missing schedule as successfully deleted")
    void deleteIsIdempotent() {
        when(schedulerClient.deleteSchedule(any(DeleteScheduleRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().message("gone").build());

        assertThat(scheduler.deleteSchedule(USER, "Text", WHEN)).isTrue();
    }

    @Test
    @DisplayName("reports failure when the schedule cannot be deleted")
    void returnsFalseWhenDeleteThrows() {
        when(schedulerClient.deleteSchedule(any(DeleteScheduleRequest.class)))
                .thenThrow(SchedulerException.builder().message("boom").build());

        assertThat(scheduler.deleteSchedule(USER, "Text", WHEN)).isFalse();
    }
}
