import { googleCalendarUrl, buildIcsFile } from "./calendarLinks";
import { ReminderProps } from "../../../props/ReminderProps";

const reminder: ReminderProps = {
  contactMethod: "Text",
  description: "Call mum",
  dateTime: "2030-01-01T09:00:00Z",
};

describe("googleCalendarUrl", () => {
  it("builds a quick-add link with a 30 minute window and the description as the title", () => {
    const url = new URL(googleCalendarUrl(reminder));

    expect(url.origin + url.pathname).toBe("https://calendar.google.com/calendar/render");
    expect(url.searchParams.get("action")).toBe("TEMPLATE");
    expect(url.searchParams.get("text")).toBe("Call mum");
    expect(url.searchParams.get("dates")).toBe("20300101T090000Z/20300101T093000Z");
  });
});

describe("buildIcsFile", () => {
  it("produces a VEVENT with matching start/end and an escaped summary", () => {
    const withSpecialChars: ReminderProps = { ...reminder, description: "Pay rent; utilities, etc" };

    const ics = buildIcsFile(withSpecialChars);

    expect(ics).toContain("BEGIN:VCALENDAR");
    expect(ics).toContain("DTSTART:20300101T090000Z");
    expect(ics).toContain("DTEND:20300101T093000Z");
    expect(ics).toContain("SUMMARY:Pay rent\\; utilities\\, etc");
    expect(ics).toContain("END:VEVENT");
  });

  it("uses CRLF line endings as RFC 5545 requires", () => {
    expect(buildIcsFile(reminder)).toContain("\r\n");
  });
});
