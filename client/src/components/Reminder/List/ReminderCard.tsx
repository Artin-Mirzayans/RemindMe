import React, { useState } from "react";
import apiClient from "../../Auth/apiClient";
import UTCToZoned from "./UTCtoZoned";
import AddToCalendar from "./AddToCalendar";
import { ReminderProps } from "../../../props/ReminderProps";
import { IoPhonePortrait } from "react-icons/io5";
import { MdEmail, MdClose, MdEdit, MdRepeat } from "react-icons/md";

import "./ReminderCard.css";

interface ReminderCardProps {
  index: number;
  reminder: ReminderProps;
  // how many reminders share this one's seriesId, including itself - 0 for a standalone reminder
  seriesCount: number;
  onDeleteReminder: (index: number) => void;
  onEditReminder: (reminder: ReminderProps) => void;
  onDeleteSeries: (seriesId: string) => void;
}

const ReminderCard: React.FC<ReminderCardProps> = ({
  index,
  reminder,
  seriesCount,
  onDeleteReminder,
  onEditReminder,
  onDeleteSeries,
}) => {
  const { formattedDate, formattedTime, relative } = UTCToZoned(
    reminder.dateTime
  );
  const [confirming, setConfirming] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const isRepeating = seriesCount > 1 && !!reminder.seriesId;

  const handleDeleteReminder = async () => {
    setDeleting(true);
    setError(null);

    try {
      const response = await apiClient.delete("/reminders", {
        params: {
          contactMethod: reminder.contactMethod,
          dateTime: reminder.dateTime,
        },
      });

      if (response.status === 204) {
        onDeleteReminder(index);
        return;
      }

      setError("This reminder cannot be cancelled right now.");
    } catch (err) {
      console.error("Error deleting reminder:", err);
      setError("This reminder cannot be cancelled right now.");
    } finally {
      setDeleting(false);
      setConfirming(false);
    }
  };

  const handleDeleteSeries = async () => {
    if (!reminder.seriesId) return;
    setDeleting(true);
    setError(null);

    try {
      const response = await apiClient.delete("/reminders/series", {
        params: { seriesId: reminder.seriesId },
      });

      if (response.status === 204) {
        onDeleteSeries(reminder.seriesId);
        return;
      }

      setError("This series cannot be cancelled right now.");
    } catch (err) {
      console.error("Error deleting series:", err);
      setError("This series cannot be cancelled right now.");
    } finally {
      setDeleting(false);
      setConfirming(false);
    }
  };

  if (confirming) {
    return (
      <div className="reminder-card reminder-card--confirming">
        <div className="reminder-card-description">
          Cancel &ldquo;{reminder.description}&rdquo;?
        </div>
        <div className="reminder-card-confirm-actions">
          <button
            type="button"
            className="btn btn--ghost btn--compact"
            onClick={() => setConfirming(false)}
            disabled={deleting}
          >
            Keep
          </button>
          <button
            type="button"
            className="btn btn--danger btn--compact"
            onClick={handleDeleteReminder}
            disabled={deleting}
          >
            {deleting ? "Cancelling..." : isRepeating ? "Just this one" : "Cancel it"}
          </button>
          {isRepeating && (
            <button
              type="button"
              className="btn btn--danger btn--compact"
              onClick={handleDeleteSeries}
              disabled={deleting}
            >
              {deleting ? "Cancelling..." : `All ${seriesCount} in series`}
            </button>
          )}
        </div>
      </div>
    );
  }

  return (
    <div className="reminder-card">
      <div className="reminder-card-description">{reminder.description}</div>
      <div className="reminder-card-when">
        <span className="reminder-card-date">
          {formattedDate} · {formattedTime}
        </span>
        <span className="reminder-card-relative">{relative}</span>
      </div>
      <div
        className="reminder-card-contact"
        title={
          reminder.contactMethod == "Text"
            ? "Sent by text message"
            : "Sent by email"
        }
      >
        {reminder.contactMethod == "Text" ? <IoPhonePortrait /> : <MdEmail />}
      </div>
      <div
        className="reminder-card-contact"
        title={isRepeating ? `Repeats - ${seriesCount} reminders in this series` : undefined}
      >
        {isRepeating && <MdRepeat />}
      </div>
      <button
        type="button"
        className="reminder-card-icon-btn"
        onClick={() => onEditReminder(reminder)}
        aria-label={`Edit reminder: ${reminder.description}`}
      >
        <MdEdit />
      </button>
      <AddToCalendar reminder={reminder} />
      <button
        type="button"
        className="reminder-card-icon-btn reminder-card-cancel"
        onClick={() => setConfirming(true)}
        aria-label={`Cancel reminder: ${reminder.description}`}
      >
        <MdClose />
      </button>
      {error && <div className="reminder-card-error">{error}</div>}
    </div>
  );
};

export default ReminderCard;
