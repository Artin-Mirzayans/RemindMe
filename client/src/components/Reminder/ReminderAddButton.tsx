import React from "react";
import { IoCreateSharp } from "react-icons/io5";

import "./ReminderAddButton.css";

interface ReminderAddButtonProps {
  remindersCount: number;
  openModal: () => void;
}

const ReminderAddButton: React.FC<ReminderAddButtonProps> = ({
  remindersCount,
  openModal,
}) => {
  const handleMaxReminders = () => {
    if (remindersCount >= 15) {
      alert("Max Reminder Amount: 15\nContact support for rate increase.");
    } else {
      openModal();
    }
  };
  return (
    <div className="reminder-add-button">
      <button onClick={handleMaxReminders}>
        <span>
          <IoCreateSharp size={48} />
        </span>
      </button>
    </div>
  );
};

export default ReminderAddButton;
