import React from "react";
import ReminderCard from "./ReminderCard";
import { ReminderProps } from "../../../props/ReminderProps";

import "./ReminderList.css";

interface ReminderListProps {
  reminders: ReminderProps[] | null;
}

const ReminderList: React.FC<ReminderListProps> = ({ reminders }) => {
  console.log(reminders);
  return (
    <div className="reminder-list">
      {!reminders || reminders?.length === 0 ? (
        <div className="reminder-list-empty">No reminders active!</div>
      ) : (
        reminders.map((reminder, index) => (
          <ReminderCard key={index} reminder={reminder} />
        ))
      )}
    </div>
  );
};

export default ReminderList;
