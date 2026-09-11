package com.remindme.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import com.remindme.models.Reminder;
import com.remindme.models.ReminderSeriesRequest;
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
    public ResponseEntity<String> createReminder(HttpServletRequest request,
            @Valid @RequestBody Reminder reminder) {
        String userId = (String) request.getAttribute("userId");

        boolean isCreated = reminderService.createReminder(reminder, userId);

        if (isCreated)
            return new ResponseEntity<>("Reminder created", HttpStatus.CREATED);
        else
            return new ResponseEntity<>("Failed to create reminder", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @PutMapping
    public ResponseEntity<String> updateReminder(HttpServletRequest request,
            @RequestParam String contactMethod,
            @RequestParam String dateTime,
            @Valid @RequestBody Reminder updated) {
        String userId = (String) request.getAttribute("userId");

        boolean isUpdated = reminderService.updateReminder(userId, contactMethod, dateTime, updated);

        if (isUpdated) {
            return new ResponseEntity<>("Reminder updated", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Reminder not found", HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/series")
    public ResponseEntity<List<Reminder>> createSeries(HttpServletRequest request,
            @Valid @RequestBody ReminderSeriesRequest seriesRequest) {
        String userId = (String) request.getAttribute("userId");

        List<Reminder> created = reminderService.createSeries(seriesRequest, userId);

        if (created.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @DeleteMapping("/series")
    public ResponseEntity<String> deleteSeries(HttpServletRequest request, @RequestParam String seriesId) {
        String userId = (String) request.getAttribute("userId");

        int deleted = reminderService.deleteSeries(userId, seriesId);

        if (deleted > 0) {
            return new ResponseEntity<>("Cancelled " + deleted + " reminder(s)", HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>("No reminders found for that series", HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping
    public ResponseEntity<List<Reminder>> getReminders(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");

        List<Reminder> reminders = reminderService.getReminders(userId);

        return new ResponseEntity<>(reminders, HttpStatus.OK);
    }

    @DeleteMapping
    public ResponseEntity<String> deleteReminder(
            HttpServletRequest request,
            @RequestParam String contactMethod,
            @RequestParam String dateTime) {

        String userId = (String) request.getAttribute("userId");

        boolean isDeleted = reminderService.deleteReminder(userId, contactMethod, dateTime);

        if (isDeleted) {
            return new ResponseEntity<>("Reminder deleted successfully", HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>("Reminder not found", HttpStatus.NOT_FOUND);
        }
    }
}
