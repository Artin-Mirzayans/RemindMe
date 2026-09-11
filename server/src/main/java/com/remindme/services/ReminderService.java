package com.remindme.services;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.remindme.models.Reminder;
import com.remindme.models.ReminderSeriesRequest;
import com.remindme.repositories.ReminderRepository;

@Service
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    // needs to match Reminder.dateTime's validation pattern - no fractional seconds
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    // single-create still only checks this client-side, series is the one path that could
    // otherwise blow past it in one request
    private static final int MAX_TOTAL_REMINDERS = 15;

    private final ReminderRepository reminderRepository;
    private final EventBridgeScheduler eventBridgeScheduler;

    public ReminderService(ReminderRepository reminderRepository, EventBridgeScheduler eventBridgeScheduler) {
        this.reminderRepository = reminderRepository;
        this.eventBridgeScheduler = eventBridgeScheduler;
    }

    public boolean createReminder(Reminder reminder, String userId) {
        if (!reminderRepository.saveReminder(reminder, userId)) {
            log.error("Reminder create aborted, store failed for user={} at={}", userId, reminder.getDateTime());
            return false;
        }

        if (eventBridgeScheduler.createSchedule(reminder, userId)) {
            log.info("Reminder created for user={} at={} via={}", userId, reminder.getDateTime(),
                    reminder.getContactMethod());
            return true;
        }

        log.error("Schedule create failed for user={} at={}, rolling back stored reminder", userId,
                reminder.getDateTime());

        if (!reminderRepository.deleteReminder(userId, reminder.getDateTime())) {
            log.error("Rollback failed for user={} at={}, stored reminder will never fire", userId,
                    reminder.getDateTime());
        }

        return false;
    }

    public List<Reminder> getReminders(String userId) {
        return reminderRepository.findAll(userId);
    }

    // If the time and contact method didn't change, this is just a description rewrite - the
    // schedule doesn't carry the description so there's nothing to move. Otherwise there's no
    // "rename" for a DynamoDB key or an EventBridge schedule name, so it creates the new one
    // first and only deletes the old one once that's confirmed, same as createReminder's rollback.
    public boolean updateReminder(String userId, String oldContactMethod, String oldDateTime, Reminder updated) {
        boolean samePlace = oldContactMethod.equals(updated.getContactMethod())
                && oldDateTime.equals(updated.getDateTime());

        if (samePlace) {
            return reminderRepository.updateDescription(userId, oldDateTime, updated.getDescription());
        }

        if (!createReminder(updated, userId)) {
            log.error("Edit aborted, could not create the new slot for user={} old={} new={}", userId, oldDateTime,
                    updated.getDateTime());
            return false;
        }

        if (!deleteReminder(userId, oldContactMethod, oldDateTime)) {
            log.error(
                    "Edited reminder created at its new time but the old slot could not be removed for user={} "
                            + "old={} new={}, it will fire on its own original schedule as a stray duplicate",
                    userId, oldDateTime, updated.getDateTime());
        }

        return true;
    }

    // "Recurring" is just N normal reminders sharing a seriesId, not a real cron schedule - the
    // delivery lambdas look up each reminder by its exact time, so every occurrence needs its own
    // row and schedule anyway. Can return fewer than requested once MAX_TOTAL_REMINDERS is hit.
    public List<Reminder> createSeries(ReminderSeriesRequest request, String userId) {
        int alreadyScheduled = reminderRepository.findAll(userId).size();
        int roomLeft = Math.max(0, MAX_TOTAL_REMINDERS - alreadyScheduled);
        int toCreate = Math.min(request.getOccurrences(), roomLeft);

        if (toCreate < request.getOccurrences()) {
            log.warn("Series for user={} capped at {} of {} requested occurrences ({} already scheduled)",
                    userId, toCreate, request.getOccurrences(), alreadyScheduled);
        }

        String seriesId = UUID.randomUUID().toString();
        Reminder template = request.getReminder();
        OffsetDateTime cursor = OffsetDateTime.parse(template.getDateTime());

        List<Reminder> created = new ArrayList<>();
        for (int i = 0; i < toCreate; i++) {
            Reminder occurrence = new Reminder(userId, cursor.format(DATE_TIME_FORMAT), template.getContactMethod(),
                    template.getDescription());
            occurrence.setSeriesId(seriesId);

            if (createReminder(occurrence, userId)) {
                created.add(occurrence);
            }

            cursor = advance(cursor, request.getFrequency());
        }

        log.info("Series {} created {} of {} requested occurrences for user={}", seriesId, created.size(),
                request.getOccurrences(), userId);
        return created;
    }

    private OffsetDateTime advance(OffsetDateTime dateTime, String frequency) {
        return switch (frequency) {
            case "DAILY" -> dateTime.plusDays(1);
            case "WEEKLY" -> dateTime.plusWeeks(1);
            case "MONTHLY" -> dateTime.plusMonths(1);
            default -> throw new IllegalArgumentException("Unknown frequency: " + frequency);
        };
    }

    // keeps going even if one cancellation fails, rather than bailing out partway through
    public int deleteSeries(String userId, String seriesId) {
        List<Reminder> matching = reminderRepository.findAll(userId).stream()
                .filter(reminder -> seriesId.equals(reminder.getSeriesId()))
                .collect(Collectors.toList());

        int deleted = 0;
        for (Reminder reminder : matching) {
            if (deleteReminder(userId, reminder.getContactMethod(), reminder.getDateTime())) {
                deleted++;
            }
        }

        log.info("Series {} cancelled {} of {} reminders for user={}", seriesId, deleted, matching.size(), userId);
        return deleted;
    }

    public boolean deleteReminder(String userId, String contactMethod, String dateTime) {
        if (!eventBridgeScheduler.deleteSchedule(userId, contactMethod, dateTime)) {
            log.error("Reminder delete aborted, schedule delete failed for user={} at={}", userId, dateTime);
            return false;
        }

        if (!reminderRepository.deleteReminder(userId, dateTime)) {
            log.error("Schedule deleted but store delete failed for user={} at={}, reminder is orphaned", userId,
                    dateTime);
            return false;
        }

        log.info("Reminder deleted for user={} at={}", userId, dateTime);
        return true;
    }
}
