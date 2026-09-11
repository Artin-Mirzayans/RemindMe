import React, { useEffect, useRef, useState } from "react";
import { MdEvent } from "react-icons/md";
import { SiGooglecalendar } from "react-icons/si";
import { ReminderProps } from "../../../props/ReminderProps";
import { googleCalendarUrl, downloadIcsFile } from "./calendarLinks";

import "./AddToCalendar.css";

interface AddToCalendarProps {
  reminder: ReminderProps;
}

// "add to calendar" menu on each reminder card - a Google Calendar quick-add link plus a
// downloadable .ics for everything else
const AddToCalendar: React.FC<AddToCalendarProps> = ({ reminder }) => {
  const [open, setOpen] = useState(false);
  const wrapperRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;

    const handleClickOutside = (e: MouseEvent) => {
      if (wrapperRef.current && !wrapperRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [open]);

  const handleDownload = () => {
    downloadIcsFile(reminder);
    setOpen(false);
  };

  return (
    <div className="add-to-calendar" ref={wrapperRef}>
      <button
        type="button"
        className="reminder-card-icon-btn"
        onClick={() => setOpen((o) => !o)}
        aria-label={`Add "${reminder.description}" to your calendar`}
        aria-expanded={open}
      >
        <MdEvent />
      </button>
      {open && (
        <div className="add-to-calendar-menu" role="menu">
          <a
            className="add-to-calendar-option"
            role="menuitem"
            href={googleCalendarUrl(reminder)}
            target="_blank"
            rel="noopener noreferrer"
            onClick={() => setOpen(false)}
          >
            <SiGooglecalendar />
            Google Calendar
          </a>
          <button
            type="button"
            className="add-to-calendar-option"
            role="menuitem"
            onClick={handleDownload}
          >
            <MdEvent />
            Apple / Outlook (.ics)
          </button>
        </div>
      )}
    </div>
  );
};

export default AddToCalendar;
