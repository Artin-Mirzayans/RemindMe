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

    public boolean createSchedule(Reminder reminder, String userId) {
        try {
            String contactMethod = reminder.getContactMethod();
            Target target = targetFactory.createLambdaTarget(contactMethod);

            String utcDateTime = reminder.getDateTime();
            String scheduleDateTime = utcDateTime.length() > 0 ? utcDateTime.substring(0, utcDateTime.length() - 1)
                    : utcDateTime;

            String sanitizedUserId = userId.replaceFirst("@", "-");
            String scheduleName = sanitizedUserId + "-" + contactMethod + "-" + utcDateTime.replace(":", ".");

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
            System.out.println("An error occurred: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteSchedule(String userId, String contactMethod, String dateTime) {
        String scheduleName = userId.replaceFirst("@", "-") + "-" + contactMethod + "-" + dateTime.replace(":", ".");
        try {
            DeleteScheduleRequest deleteScheduleRequest = DeleteScheduleRequest.builder()
                    .name(scheduleName)
                    .groupName(contactMethod)
                    .build();

            schedulerClient.deleteSchedule(deleteScheduleRequest);

            return true;
        } catch (SchedulerException e) {
            System.out.println("Failed to delete schedule with ID " + scheduleName + ": " + e.getMessage());
            return false;
        }
    }
}
