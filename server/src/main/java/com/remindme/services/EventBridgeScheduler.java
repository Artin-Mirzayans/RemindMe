package com.remindme.services;

import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.*;

import com.remindme.models.Reminder;
import com.target.TargetFactory;

@Service
public class EventBridgeScheduler {

    private final SchedulerClient schedulerClient;
    private final TargetFactory targetFactory;

    public EventBridgeScheduler(SchedulerClient schedulerClient, TargetFactory targetFactory) {
        this.schedulerClient = schedulerClient;
        this.targetFactory = targetFactory;
    }

    public boolean createSchedule(Reminder reminder) {
        try {
            String contactMethod = reminder.getContactMethod();
            Target target = targetFactory.createLambdaTarget(contactMethod);

            String scheduleName = reminder.getUserId() + "-" + System.currentTimeMillis();

            String utcDateTime = reminder.getDateTime();
            String scheduleDateTime = utcDateTime.length() > 0 ? utcDateTime.substring(0, utcDateTime.length() - 1)
                    : utcDateTime;

            CreateScheduleRequest createScheduleRequest = CreateScheduleRequest.builder()
                    .name(scheduleName)
                    .scheduleExpression("at(" + scheduleDateTime + ")")
                    .target(target)
                    .flexibleTimeWindow(FlexibleTimeWindow.builder()
                            .mode(FlexibleTimeWindowMode.OFF)
                            .build())
                    .groupName(contactMethod)
                    .build();

            CreateScheduleResponse response = schedulerClient.createSchedule(createScheduleRequest);

            return response.sdkHttpResponse().isSuccessful();

        } catch (SchedulerException e) {
            System.err.println("EventBridge scheduler error" + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteSchedule(String reminderId, String contactMethod) {
        try {
            DeleteScheduleRequest deleteScheduleRequest = DeleteScheduleRequest.builder()
                    .name(reminderId)
                    .groupName(contactMethod)
                    .build();

            schedulerClient.deleteSchedule(deleteScheduleRequest);

            return true;
        } catch (SchedulerException e) {
            System.err.println("Failed to delete schedule with ID " + reminderId + ": " + e.getMessage());
            return false;
        }
    }
}
