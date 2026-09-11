package com.remindme.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.exception.GlobalExceptionHandler;
import com.remindme.models.Reminder;
import com.remindme.models.ReminderSeriesRequest;
import com.remindme.services.ReminderService;

@ExtendWith(MockitoExtension.class)
class ReminderControllerTest {

    private static final String USER = "someone@example.com";

    @Mock
    private ReminderService reminderService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ReminderController(reminderService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private String future() {
        return OffsetDateTime.now(ZoneOffset.UTC).plusDays(1)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
    }

    private String past() {
        return OffsetDateTime.now(ZoneOffset.UTC).minusDays(1)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
    }

    private String body(String dateTime, String description, String contactMethod) {
        return """
                {"dateTime":"%s","description":"%s","contactMethod":"%s"}
                """.formatted(dateTime, description, contactMethod);
    }

    @Test
    @DisplayName("creates a reminder for the user resolved from the token")
    void createsReminder() throws Exception {
        when(reminderService.createReminder(any(Reminder.class), eq(USER))).thenReturn(true);

        mockMvc.perform(post("/reminders")
                .requestAttr("userId", USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(future(), "call mum", "Text")))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("rejects a reminder dated in the past")
    void rejectsPastDateTime() throws Exception {
        mockMvc.perform(post("/reminders")
                .requestAttr("userId", USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(past(), "call mum", "Text")))
                .andExpect(status().isBadRequest());

        verify(reminderService, never()).createReminder(any(), any());
    }

    @Test
    @DisplayName("rejects a description shorter than three characters")
    void rejectsShortDescription() throws Exception {
        mockMvc.perform(post("/reminders")
                .requestAttr("userId", USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(future(), "hi", "Text")))
                .andExpect(status().isBadRequest());

        verify(reminderService, never()).createReminder(any(), any());
    }

    @Test
    @DisplayName("rejects a contact method the scheduler has no target for")
    void rejectsUnknownContactMethod() throws Exception {
        mockMvc.perform(post("/reminders")
                .requestAttr("userId", USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(future(), "call mum", "Pigeon")))
                .andExpect(status().isBadRequest());

        verify(reminderService, never()).createReminder(any(), any());
    }

    @Test
    @DisplayName("returns an empty list rather than a 404 when there is nothing scheduled")
    void returnsEmptyListWhenNoReminders() throws Exception {
        when(reminderService.getReminders(USER)).thenReturn(List.of());

        mockMvc.perform(get("/reminders").requestAttr("userId", USER))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("reports 404 when the reminder to cancel is not found")
    void reportsNotFoundOnFailedDelete() throws Exception {
        when(reminderService.deleteReminder(USER, "Text", "2030-01-01T09:00:00Z")).thenReturn(false);

        mockMvc.perform(delete("/reminders")
                .requestAttr("userId", USER)
                .param("contactMethod", "Text")
                .param("dateTime", "2030-01-01T09:00:00Z"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("edits a reminder identified by its current contact method and time")
    void editsReminder() throws Exception {
        when(reminderService.updateReminder(eq(USER), eq("Text"), eq("2030-01-01T09:00:00Z"), any(Reminder.class)))
                .thenReturn(true);

        mockMvc.perform(put("/reminders")
                .requestAttr("userId", USER)
                .param("contactMethod", "Text")
                .param("dateTime", "2030-01-01T09:00:00Z")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(future(), "call mum back", "Text")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("reports 404 when the reminder to edit is not found")
    void reportsNotFoundOnFailedEdit() throws Exception {
        when(reminderService.updateReminder(eq(USER), eq("Text"), eq("2030-01-01T09:00:00Z"), any(Reminder.class)))
                .thenReturn(false);

        mockMvc.perform(put("/reminders")
                .requestAttr("userId", USER)
                .param("contactMethod", "Text")
                .param("dateTime", "2030-01-01T09:00:00Z")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(future(), "call mum back", "Text")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("rejects an edit whose new values fail validation")
    void rejectsInvalidEdit() throws Exception {
        mockMvc.perform(put("/reminders")
                .requestAttr("userId", USER)
                .param("contactMethod", "Text")
                .param("dateTime", "2030-01-01T09:00:00Z")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(past(), "call mum back", "Text")))
                .andExpect(status().isBadRequest());

        verify(reminderService, never()).updateReminder(any(), any(), any(), any());
    }

    private String seriesBody(String dateTime, String description, String contactMethod, String frequency,
            int occurrences) {
        return """
                {"reminder":%s,"frequency":"%s","occurrences":%d}
                """.formatted(body(dateTime, description, contactMethod), frequency, occurrences);
    }

    @Test
    @DisplayName("creates a series for the user resolved from the token")
    void createsSeries() throws Exception {
        when(reminderService.createSeries(any(ReminderSeriesRequest.class), eq(USER)))
                .thenReturn(List.of(new Reminder(USER, future(), "Text", "take pills")));

        mockMvc.perform(post("/reminders/series")
                .requestAttr("userId", USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(seriesBody(future(), "take pills", "Text", "DAILY", 3)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("reports a conflict when a series creates nothing")
    void reportsConflictWhenSeriesCreatesNothing() throws Exception {
        when(reminderService.createSeries(any(ReminderSeriesRequest.class), eq(USER))).thenReturn(List.of());

        mockMvc.perform(post("/reminders/series")
                .requestAttr("userId", USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(seriesBody(future(), "take pills", "Text", "DAILY", 3)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("rejects a series with an unknown frequency")
    void rejectsUnknownFrequency() throws Exception {
        mockMvc.perform(post("/reminders/series")
                .requestAttr("userId", USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(seriesBody(future(), "take pills", "Text", "HOURLY", 3)))
                .andExpect(status().isBadRequest());

        verify(reminderService, never()).createSeries(any(), any());
    }

    @Test
    @DisplayName("rejects a series of just one occurrence")
    void rejectsTooFewOccurrences() throws Exception {
        mockMvc.perform(post("/reminders/series")
                .requestAttr("userId", USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(seriesBody(future(), "take pills", "Text", "DAILY", 1)))
                .andExpect(status().isBadRequest());

        verify(reminderService, never()).createSeries(any(), any());
    }

    @Test
    @DisplayName("cancels a series")
    void cancelsSeries() throws Exception {
        when(reminderService.deleteSeries(USER, "series-1")).thenReturn(3);

        mockMvc.perform(delete("/reminders/series")
                .requestAttr("userId", USER)
                .param("seriesId", "series-1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("reports 404 when the series to cancel is not found")
    void reportsNotFoundWhenSeriesToCancelIsMissing() throws Exception {
        when(reminderService.deleteSeries(USER, "no-such-series")).thenReturn(0);

        mockMvc.perform(delete("/reminders/series")
                .requestAttr("userId", USER)
                .param("seriesId", "no-such-series"))
                .andExpect(status().isNotFound());
    }
}
