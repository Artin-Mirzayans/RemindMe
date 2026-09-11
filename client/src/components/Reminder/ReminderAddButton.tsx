import React from "react";
import { IoCreateSharp } from "react-icons/io5";

import "./ReminderAddButton.css";

const MAX_REMINDERS = 15;

interface ReminderAddButtonProps {
  remindersCount: number;
  openModal: () => void;
}

const ReminderAddButton: React.FC<ReminderAddButtonProps> = ({
  remindersCount,
  openModal,
}) => {
  const atLimit = remindersCount >= MAX_REMINDERS;

  return (
    <div className="reminder-add-button">
      <button
        type="button"
        className="btn btn--icon"
        onClick={openModal}
        disabled={atLimit}
        aria-label="Add a reminder"
      >
        <IoCreateSharp size={32} />
      </button>
      {atLimit && (
        <p className="reminder-add-button-limit">
          You&apos;ve reached the {MAX_REMINDERS} reminder limit. Cancel one to
          add another.
        </p>
      )}
    </div>
  );
};

export default ReminderAddButton;
