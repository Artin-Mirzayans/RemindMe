package com.remindme.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import com.remindme.models.Reminder;
import com.remindme.services.ReminderService;

import java.util.List;

@RestController
@RequestMapping("/reminders")
@Validated
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @PostMapping
    public ResponseEntity<String> createReminder(@Valid @RequestBody Reminder reminder) {
        boolean isCreated = reminderService.createReminder(reminder);
        if (isCreated)
            return new ResponseEntity<>("Reminder created", HttpStatus.CREATED);
        else
            return new ResponseEntity<>("Failed to create reminder", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @GetMapping
    public ResponseEntity<List<Reminder>> getReminders() {
        List<Reminder> reminders = reminderService.getReminders();

        if (reminders.isEmpty())
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        else if (reminders.size() > 0)
            return new ResponseEntity<>(reminders, HttpStatus.OK);
        else
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteReminder(@PathVariable("id") String reminderId,
            @RequestParam("contactMethod") String contactMethod) {
        boolean isDeleted = reminderService.deleteReminder(reminderId, contactMethod);
        if (isDeleted) {
            return new ResponseEntity<>("Reminder deleted successfully", HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>("Reminder not found", HttpStatus.NOT_FOUND);
        }
    }
}
