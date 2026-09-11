import { ReminderProps } from "../../../props/ReminderProps";

// reminders don't have a duration, so give calendar entries a nominal 30 minutes
const DEFAULT_DURATION_MINUTES = 30;

// yyyyMMddTHHmmssZ - the compact UTC form Google Calendar and .ics both want
const toCompactUtc = (date: Date): string =>
    date.toISOString().replace(/[-:]/g, "").split(".")[0] + "Z";

const addMinutes = (date: Date, minutes: number): Date =>
    new Date(date.getTime() + minutes * 60_000);

// opens Google's own pre-filled create-event page, no auth or API call needed
export const googleCalendarUrl = (reminder: ReminderProps): string => {
    const start = new Date(reminder.dateTime);
    const end = addMinutes(start, DEFAULT_DURATION_MINUTES);

    const params = new URLSearchParams({
        action: "TEMPLATE",
        text: reminder.description,
        dates: `${toCompactUtc(start)}/${toCompactUtc(end)}`,
        details: "Added from RemindMe",
    });

    return `https://calendar.google.com/calendar/render?${params.toString()}`;
};

// escapes the characters RFC 5545 requires escaping in text fields
const escapeIcsText = (text: string): string =>
    text.replace(/\\/g, "\\\\").replace(/;/g, "\\;").replace(/,/g, "\\,");

// a standalone .ics file for Apple Calendar, Outlook, etc - Google is covered better by the
// quick-add link above
export const buildIcsFile = (reminder: ReminderProps): string => {
    const start = new Date(reminder.dateTime);
    const end = addMinutes(start, DEFAULT_DURATION_MINUTES);
    const uid = `${reminder.contactMethod}-${reminder.dateTime}@remindme.amsksolutions.com`;

    // RFC 5545 wants CRLF line endings
    return [
        "BEGIN:VCALENDAR",
        "VERSION:2.0",
        "PRODID:-//RemindMe//Reminder//EN",
        "BEGIN:VEVENT",
        `UID:${uid}`,
        `DTSTAMP:${toCompactUtc(new Date())}`,
        `DTSTART:${toCompactUtc(start)}`,
        `DTEND:${toCompactUtc(end)}`,
        `SUMMARY:${escapeIcsText(reminder.description)}`,
        "DESCRIPTION:Added from RemindMe",
        "END:VEVENT",
        "END:VCALENDAR",
        "",
    ].join("\r\n");
};

export const downloadIcsFile = (reminder: ReminderProps): void => {
    const blob = new Blob([buildIcsFile(reminder)], { type: "text/calendar;charset=utf-8" });
    const url = URL.createObjectURL(blob);

    const link = document.createElement("a");
    link.href = url;
    link.download = `${reminder.description.slice(0, 30).replace(/[^\w -]/g, "").trim() || "reminder"}.ics`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    URL.revokeObjectURL(url);
};
