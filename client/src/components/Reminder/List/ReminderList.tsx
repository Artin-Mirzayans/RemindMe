import React from "react";
import ReminderCard from "./ReminderCard";
import { ReminderProps } from "../../../props/ReminderProps";
import { FcAlarmClock } from "react-icons/fc";

import "./ReminderList.css";

interface ReminderListProps {
  reminders: ReminderProps[] | null;
  onDeleteReminder: (index: number) => void;
  onEditReminder: (reminder: ReminderProps) => void;
  onDeleteSeries: (seriesId: string) => void;
}

const ReminderList: React.FC<ReminderListProps> = ({
  reminders,
  onDeleteReminder,
  onEditReminder,
  onDeleteSeries,
}) => {
  const seriesCounts = (reminders ?? []).reduce<Record<string, number>>(
    (counts, reminder) => {
      if (reminder.seriesId) {
        counts[reminder.seriesId] = (counts[reminder.seriesId] ?? 0) + 1;
      }
      return counts;
    },
    {}
  );

  return (
    <div className="reminder-list">
      {!reminders || reminders?.length === 0 ? (
        <div className="reminder-list-empty">
          <FcAlarmClock size={72} aria-hidden="true" />
          <div>
            <p className="reminder-list-empty-title">No reminders yet</p>
            <p className="reminder-list-empty-text">
              Add one and we&apos;ll text or email you when the time comes.
            </p>
          </div>
        </div>
      ) : (
        reminders.map((reminder, index) => (
          <ReminderCard
            key={index}
            index={index}
            reminder={reminder}
            seriesCount={reminder.seriesId ? seriesCounts[reminder.seriesId] : 0}
            onDeleteReminder={onDeleteReminder}
            onEditReminder={onEditReminder}
            onDeleteSeries={onDeleteSeries}
          />
        ))
      )}
    </div>
  );
};

export default ReminderList;
