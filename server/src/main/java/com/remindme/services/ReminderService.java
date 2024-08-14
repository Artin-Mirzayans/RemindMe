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

    public boolean createReminder(Reminder reminder) {
        Boolean createdReminder = reminderRepository.saveReminder(reminder);
        Boolean createdScheduler = eventBridgeScheduler.createSchedule(reminder);
        return createdReminder && createdScheduler;
    }

    public List<Reminder> getReminders() {
        return reminderRepository.findAll("1");
    }

    public boolean deleteReminder(String reminderId, String contactMethod) {
        return eventBridgeScheduler.deleteSchedule(reminderId, contactMethod);
    }
}
