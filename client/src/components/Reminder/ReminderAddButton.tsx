import React from "react";
import { IoCreateSharp } from "react-icons/io5";

import "./ReminderAddButton.css";

interface ReminderAddButtonProps {
  openModal: () => void;
}

const ReminderAddButton: React.FC<ReminderAddButtonProps> = ({ openModal }) => {
  return (
    <div className="reminder-add-button">
      <button onClick={openModal}>
        <span>
          <IoCreateSharp size={48} />
        </span>
      </button>
    </div>
  );
};

export default ReminderAddButton;
