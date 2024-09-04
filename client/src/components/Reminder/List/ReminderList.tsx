import React from "react";
import ReminderCard from "./ReminderCard";
import { ReminderProps } from "../../../props/ReminderProps";
import { FcAlarmClock } from "react-icons/fc";

import "./ReminderList.css";

interface ReminderListProps {
  reminders: ReminderProps[] | null;
  onDeleteReminder: (index: number) => void;
}

const ReminderList: React.FC<ReminderListProps> = ({
  reminders,
  onDeleteReminder,
}) => {
  return (
    <div className="reminder-list">
      {!reminders || reminders?.length === 0 ? (
        <div className="reminder-list-empty">
          <div>Create a reminder!</div>
          <div>
            <FcAlarmClock size={80} />
          </div>
        </div>
      ) : (
        reminders.map((reminder, index) => (
          <ReminderCard
            key={index}
            index={index}
            reminder={reminder}
            onDeleteReminder={onDeleteReminder}
          />
        ))
      )}
    </div>
  );
};

export default ReminderList;
