package com.remindme.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.*;

import com.remindme.models.Reminder;
import com.target.TargetFactory;

@Service
public class EventBridgeScheduler {

    private static final Logger log = LoggerFactory.getLogger(EventBridgeScheduler.class);

    private final SchedulerClient schedulerClient;
    private final TargetFactory targetFactory;

    public EventBridgeScheduler(SchedulerClient schedulerClient, TargetFactory targetFactory) {
        this.schedulerClient = schedulerClient;
        this.targetFactory = targetFactory;
    }

    static String scheduleName(String userId, String contactMethod, String dateTime) {
        return userId.replaceFirst("@", "-") + "-" + contactMethod + "-" + dateTime.replace(":", ".");
    }

    public boolean createSchedule(Reminder reminder, String userId) {
        String contactMethod = reminder.getContactMethod();
        String utcDateTime = reminder.getDateTime();
        String name = scheduleName(userId, contactMethod, utcDateTime);

        try {
            Target target = targetFactory.createLambdaTarget(contactMethod);

            String scheduleDateTime = utcDateTime.endsWith("Z")
                    ? utcDateTime.substring(0, utcDateTime.length() - 1)
                    : utcDateTime;

            CreateScheduleRequest createScheduleRequest = CreateScheduleRequest.builder()
                    .name(name)
                    .scheduleExpression("at(" + scheduleDateTime + ")")
                    .target(target)
                    .flexibleTimeWindow(FlexibleTimeWindow.builder()
                            .mode(FlexibleTimeWindowMode.OFF)
                            .build())
                    .groupName(contactMethod)
                    .build();

            CreateScheduleResponse response = schedulerClient.createSchedule(createScheduleRequest);

            return response.sdkHttpResponse().isSuccessful();

        } catch (ConflictException e) {
            log.warn("Schedule {} already exists", name);
            return false;
        } catch (Exception e) {
            log.error("Failed to create schedule {}", name, e);
            return false;
        }
    }

    public boolean deleteSchedule(String userId, String contactMethod, String dateTime) {
        String name = scheduleName(userId, contactMethod, dateTime);

        try {
            DeleteScheduleRequest deleteScheduleRequest = DeleteScheduleRequest.builder()
                    .name(name)
                    .groupName(contactMethod)
                    .build();

            schedulerClient.deleteSchedule(deleteScheduleRequest);

            return true;
        } catch (ResourceNotFoundException e) {
            log.info("Schedule {} already gone, treating delete as successful", name);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete schedule {}", name, e);
            return false;
        }
    }
}
