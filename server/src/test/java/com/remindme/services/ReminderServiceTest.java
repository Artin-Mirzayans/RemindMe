package com.remindme.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.remindme.models.Reminder;
import com.remindme.models.ReminderSeriesRequest;
import com.remindme.repositories.ReminderRepository;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    private static final String USER = "someone@example.com";
    private static final String WHEN = "2030-01-01T09:00:00Z";

    @Mock
    private ReminderRepository reminderRepository;

    @Mock
    private EventBridgeScheduler eventBridgeScheduler;

    @InjectMocks
    private ReminderService reminderService;

    private Reminder reminder;

    @BeforeEach
    void setUp() {
        reminder = new Reminder(USER, WHEN, "Text", "call mum");
    }

    @Test
    @DisplayName("stores the reminder then schedules it")
    void createsReminderWhenBothSucceed() {
        when(reminderRepository.saveReminder(reminder, USER)).thenReturn(true);
        when(eventBridgeScheduler.createSchedule(reminder, USER)).thenReturn(true);

        assertThat(reminderService.createReminder(reminder, USER)).isTrue();

        verify(reminderRepository).saveReminder(reminder, USER);
        verify(eventBridgeScheduler).createSchedule(reminder, USER);
    }

    @Test
    @DisplayName("does not schedule anything when the store write fails")
    void skipsSchedulingWhenStoreFails() {
        when(reminderRepository.saveReminder(reminder, USER)).thenReturn(false);

        assertThat(reminderService.createReminder(reminder, USER)).isFalse();

        verifyNoInteractions(eventBridgeScheduler);
    }

    @Test
    @DisplayName("rolls the stored reminder back when scheduling fails")
    void rollsBackStoredReminderWhenSchedulingFails() {
        when(reminderRepository.saveReminder(reminder, USER)).thenReturn(true);
        when(eventBridgeScheduler.createSchedule(reminder, USER)).thenReturn(false);
        when(reminderRepository.deleteReminder(USER, WHEN)).thenReturn(true);

        assertThat(reminderService.createReminder(reminder, USER)).isFalse();

        verify(reminderRepository).deleteReminder(USER, WHEN);
    }

    @Test
    @DisplayName("still reports failure when the rollback itself fails")
    void reportsFailureWhenRollbackFails() {
        when(reminderRepository.saveReminder(reminder, USER)).thenReturn(true);
        when(eventBridgeScheduler.createSchedule(reminder, USER)).thenReturn(false);
        when(reminderRepository.deleteReminder(USER, WHEN)).thenReturn(false);

        assertThat(reminderService.createReminder(reminder, USER)).isFalse();
    }

    @Test
    @DisplayName("removes the schedule before the stored reminder")
    void deletesScheduleBeforeStore() {
        when(eventBridgeScheduler.deleteSchedule(USER, "Text", WHEN)).thenReturn(true);
        when(reminderRepository.deleteReminder(USER, WHEN)).thenReturn(true);

        assertThat(reminderService.deleteReminder(USER, "Text", WHEN)).isTrue();

        verify(eventBridgeScheduler).deleteSchedule(USER, "Text", WHEN);
        verify(reminderRepository).deleteReminder(USER, WHEN);
    }

    @Test
    @DisplayName("keeps the stored reminder when the schedule cannot be removed")
    void keepsStoredReminderWhenScheduleDeleteFails() {
        when(eventBridgeScheduler.deleteSchedule(USER, "Text", WHEN)).thenReturn(false);

        assertThat(reminderService.deleteReminder(USER, "Text", WHEN)).isFalse();

        verify(reminderRepository, never()).deleteReminder(any(), any());
    }

    @Test
    @DisplayName("reports failure when the schedule is gone but the store delete fails")
    void reportsFailureWhenStoreDeleteFails() {
        when(eventBridgeScheduler.deleteSchedule(USER, "Text", WHEN)).thenReturn(true);
        when(reminderRepository.deleteReminder(USER, WHEN)).thenReturn(false);

        assertThat(reminderService.deleteReminder(USER, "Text", WHEN)).isFalse();
    }

    @Test
    @DisplayName("reads reminders straight from the repository")
    void readsRemindersFromRepository() {
        when(reminderRepository.findAll(eq(USER))).thenReturn(java.util.List.of(reminder));

        assertThat(reminderService.getReminders(USER)).containsExactly(reminder);
    }

    @Test
    @DisplayName("editing just the description rewrites it in place, without touching the schedule")
    void updateSamePlaceRewritesDescriptionOnly() {
        Reminder edited = new Reminder(USER, WHEN, "Text", "call mum back");
        when(reminderRepository.updateDescription(USER, WHEN, "call mum back")).thenReturn(true);

        assertThat(reminderService.updateReminder(USER, "Text", WHEN, edited)).isTrue();

        verify(reminderRepository).updateDescription(USER, WHEN, "call mum back");
        verifyNoInteractions(eventBridgeScheduler);
    }

    @Test
    @DisplayName("editing the time creates the new slot and removes the old one")
    void updateToNewTimeMovesTheReminder() {
        String newWhen = "2030-01-02T09:00:00Z";
        Reminder edited = new Reminder(USER, newWhen, "Text", "call mum");

        when(reminderRepository.saveReminder(edited, USER)).thenReturn(true);
        when(eventBridgeScheduler.createSchedule(edited, USER)).thenReturn(true);
        when(eventBridgeScheduler.deleteSchedule(USER, "Text", WHEN)).thenReturn(true);
        when(reminderRepository.deleteReminder(USER, WHEN)).thenReturn(true);

        assertThat(reminderService.updateReminder(USER, "Text", WHEN, edited)).isTrue();

        verify(reminderRepository).saveReminder(edited, USER);
        verify(eventBridgeScheduler).createSchedule(edited, USER);
        verify(eventBridgeScheduler).deleteSchedule(USER, "Text", WHEN);
        verify(reminderRepository).deleteReminder(USER, WHEN);
    }

    @Test
    @DisplayName("editing the contact method moves the reminder even if the time is unchanged")
    void updateToNewContactMethodMovesTheReminder() {
        Reminder edited = new Reminder(USER, WHEN, "Email", "call mum");

        when(reminderRepository.saveReminder(edited, USER)).thenReturn(true);
        when(eventBridgeScheduler.createSchedule(edited, USER)).thenReturn(true);
        when(eventBridgeScheduler.deleteSchedule(USER, "Text", WHEN)).thenReturn(true);
        when(reminderRepository.deleteReminder(USER, WHEN)).thenReturn(true);

        assertThat(reminderService.updateReminder(USER, "Text", WHEN, edited)).isTrue();

        verify(eventBridgeScheduler).createSchedule(edited, USER);
        verify(eventBridgeScheduler).deleteSchedule(USER, "Text", WHEN);
    }

    @Test
    @DisplayName("a move that fails to create the new slot leaves the original reminder untouched")
    void updateToNewTimeFailsWithoutTouchingTheOriginal() {
        String newWhen = "2030-01-02T09:00:00Z";
        Reminder edited = new Reminder(USER, newWhen, "Text", "call mum");

        when(reminderRepository.saveReminder(edited, USER)).thenReturn(false);

        assertThat(reminderService.updateReminder(USER, "Text", WHEN, edited)).isFalse();

        verify(eventBridgeScheduler, never()).deleteSchedule(any(), any(), any());
        verify(reminderRepository, never()).deleteReminder(eq(USER), eq(WHEN));
    }

    @Test
    @DisplayName("a move still reports success even if cleaning up the old slot fails")
    void updateToNewTimeSucceedsEvenIfOldSlotCannotBeRemoved() {
        String newWhen = "2030-01-02T09:00:00Z";
        Reminder edited = new Reminder(USER, newWhen, "Text", "call mum");

        when(reminderRepository.saveReminder(edited, USER)).thenReturn(true);
        when(eventBridgeScheduler.createSchedule(edited, USER)).thenReturn(true);
        when(eventBridgeScheduler.deleteSchedule(USER, "Text", WHEN)).thenReturn(false);

        assertThat(reminderService.updateReminder(USER, "Text", WHEN, edited)).isTrue();
    }

    @Test
    @DisplayName("a weekly series creates one reminder per occurrence, 7 days apart, sharing a series id")
    void createsWeeklySeries() {
        when(reminderRepository.findAll(USER)).thenReturn(List.of());
        when(reminderRepository.saveReminder(any(Reminder.class), eq(USER))).thenReturn(true);
        when(eventBridgeScheduler.createSchedule(any(Reminder.class), eq(USER))).thenReturn(true);

        ReminderSeriesRequest request = new ReminderSeriesRequest(reminder, "WEEKLY", 3);
        List<Reminder> created = reminderService.createSeries(request, USER);

        assertThat(created).hasSize(3);
        assertThat(created).extracting(Reminder::getDateTime).containsExactly(
                "2030-01-01T09:00:00Z", "2030-01-08T09:00:00Z", "2030-01-15T09:00:00Z");
        assertThat(created).extracting(Reminder::getSeriesId).doesNotContainNull();
        assertThat(created.stream().map(Reminder::getSeriesId).distinct()).hasSize(1);
    }

    @Test
    @DisplayName("a monthly series advances by calendar month")
    void createsMonthlySeries() {
        when(reminderRepository.findAll(USER)).thenReturn(List.of());
        when(reminderRepository.saveReminder(any(Reminder.class), eq(USER))).thenReturn(true);
        when(eventBridgeScheduler.createSchedule(any(Reminder.class), eq(USER))).thenReturn(true);

        ReminderSeriesRequest request = new ReminderSeriesRequest(reminder, "MONTHLY", 2);
        List<Reminder> created = reminderService.createSeries(request, USER);

        assertThat(created).extracting(Reminder::getDateTime)
                .containsExactly("2030-01-01T09:00:00Z", "2030-02-01T09:00:00Z");
    }

    @Test
    @DisplayName("a series is capped so it never pushes the user past the 15 reminder limit")
    void capsSeriesAtTheReminderLimit() {
        when(reminderRepository.findAll(USER)).thenReturn(java.util.Collections.nCopies(13, reminder));
        when(reminderRepository.saveReminder(any(Reminder.class), eq(USER))).thenReturn(true);
        when(eventBridgeScheduler.createSchedule(any(Reminder.class), eq(USER))).thenReturn(true);

        ReminderSeriesRequest request = new ReminderSeriesRequest(reminder, "DAILY", 5);
        List<Reminder> created = reminderService.createSeries(request, USER);

        assertThat(created).hasSize(2);
    }

    @Test
    @DisplayName("a series creates nothing when already at the reminder limit")
    void createsNothingWhenAlreadyAtTheLimit() {
        when(reminderRepository.findAll(USER)).thenReturn(java.util.Collections.nCopies(15, reminder));

        ReminderSeriesRequest request = new ReminderSeriesRequest(reminder, "DAILY", 3);
        List<Reminder> created = reminderService.createSeries(request, USER);

        assertThat(created).isEmpty();
        verifyNoInteractions(eventBridgeScheduler);
    }

    @Test
    @DisplayName("a series keeps going past an occurrence that fails to schedule")
    void seriesContinuesPastAFailedOccurrence() {
        when(reminderRepository.findAll(USER)).thenReturn(List.of());
        when(reminderRepository.saveReminder(any(Reminder.class), eq(USER))).thenReturn(true);
        when(eventBridgeScheduler.createSchedule(any(Reminder.class), eq(USER)))
                .thenReturn(true, false, true);
        when(reminderRepository.deleteReminder(eq(USER), any())).thenReturn(true);

        ReminderSeriesRequest request = new ReminderSeriesRequest(reminder, "DAILY", 3);
        List<Reminder> created = reminderService.createSeries(request, USER);

        assertThat(created).hasSize(2);
        verify(reminderRepository, times(3)).saveReminder(any(Reminder.class), eq(USER));
    }

    @Test
    @DisplayName("cancelling a series only removes reminders that belong to it")
    void deleteSeriesRemovesOnlyMatchingReminders() {
        Reminder inSeries1 = new Reminder(USER, "2030-01-01T09:00:00Z", "Text", "call mum");
        inSeries1.setSeriesId("series-1");
        Reminder inSeries2 = new Reminder(USER, "2030-01-08T09:00:00Z", "Text", "call mum");
        inSeries2.setSeriesId("series-1");
        Reminder standalone = new Reminder(USER, "2030-01-10T09:00:00Z", "Email", "pay rent");

        when(reminderRepository.findAll(USER)).thenReturn(List.of(inSeries1, inSeries2, standalone));
        when(eventBridgeScheduler.deleteSchedule(eq(USER), any(), any())).thenReturn(true);
        when(reminderRepository.deleteReminder(eq(USER), any())).thenReturn(true);

        int deleted = reminderService.deleteSeries(USER, "series-1");

        assertThat(deleted).isEqualTo(2);
        verify(eventBridgeScheduler).deleteSchedule(USER, "Text", "2030-01-01T09:00:00Z");
        verify(eventBridgeScheduler).deleteSchedule(USER, "Text", "2030-01-08T09:00:00Z");
        verify(eventBridgeScheduler, never()).deleteSchedule(USER, "Email", "2030-01-10T09:00:00Z");
    }

    @Test
    @DisplayName("cancelling an unknown series removes nothing")
    void deleteSeriesReturnsZeroWhenNothingMatches() {
        when(reminderRepository.findAll(USER)).thenReturn(List.of(reminder));

        assertThat(reminderService.deleteSeries(USER, "no-such-series")).isEqualTo(0);
        verifyNoInteractions(eventBridgeScheduler);
    }
}
