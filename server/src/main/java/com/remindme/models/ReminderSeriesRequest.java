package com.remindme.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

// a request to create several reminders at once on a fixed cadence, e.g. "every Monday at 8am,
// 6 times" - each occurrence is an ordinary standalone Reminder, just sharing a seriesId so the
// whole run can be cancelled together
public class ReminderSeriesRequest {

    @NotNull(message = "Reminder cannot be null")
    @Valid
    private Reminder reminder;

    @NotNull(message = "Frequency cannot be null")
    @Pattern(regexp = "^(DAILY|WEEKLY|MONTHLY)$", message = "Frequency must be DAILY, WEEKLY, or MONTHLY")
    private String frequency;

    @Min(value = 2, message = "A series needs at least 2 occurrences - create a single reminder for just one")
    @Max(value = 12, message = "A series can include at most 12 occurrences")
    private int occurrences;

    public ReminderSeriesRequest() {
    }

    public ReminderSeriesRequest(Reminder reminder, String frequency, int occurrences) {
        this.reminder = reminder;
        this.frequency = frequency;
        this.occurrences = occurrences;
    }

    public Reminder getReminder() {
        return reminder;
    }

    public void setReminder(Reminder reminder) {
        this.reminder = reminder;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public int getOccurrences() {
        return occurrences;
    }

    public void setOccurrences(int occurrences) {
        this.occurrences = occurrences;
    }
}
