package com.remindme.services;

import java.util.List;
import org.springframework.stereotype.Service;

import com.remindme.models.Reminder;
import com.remindme.repositories.ReminderRepository;

@Service
public class ReminderService {
    private final ReminderRepository reminderRepository;
    private final EventBridgeScheduler eventBridgeScheduler;

    public ReminderService(ReminderRepository reminderRepository, EventBridgeScheduler eventBridgeScheduler) {
        this.reminderRepository = reminderRepository;
        this.eventBridgeScheduler = eventBridgeScheduler;
    }

    public boolean createReminder(Reminder reminder, String userId) {
        Boolean createdReminder = reminderRepository.saveReminder(reminder, userId);
        Boolean createdScheduler = eventBridgeScheduler.createSchedule(reminder, userId);
        return createdReminder && createdScheduler;
    }

    public List<Reminder> getReminders(String userId) {
        return reminderRepository.findAll(userId);
    }

    public boolean deleteReminder(String userId, String contactMethod, String dateTime) {
        Boolean deletedReminder = reminderRepository.deleteReminder(userId, dateTime);
        Boolean deletedScheduler = eventBridgeScheduler.deleteSchedule(userId, contactMethod, dateTime);
        return deletedReminder && deletedScheduler;
    }
}
