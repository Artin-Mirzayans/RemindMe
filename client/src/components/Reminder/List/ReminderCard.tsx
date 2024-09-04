import React from "react";
import apiClient from "../../Auth/apiClient";
import UTCToZoned from "./UTCtoZoned";
import { ReminderProps } from "../../../props/ReminderProps";
import { IoPhonePortrait } from "react-icons/io5";
import { MdEmail } from "react-icons/md";
import { FcCancel } from "react-icons/fc";

import "./ReminderCard.css";

interface ReminderCardProps {
  index: number;
  reminder: ReminderProps;
  onDeleteReminder: (index: number) => void;
}

const ReminderCard: React.FC<ReminderCardProps> = ({
  index,
  reminder,
  onDeleteReminder,
}) => {
  const { formattedDate, formattedTime } = UTCToZoned(reminder.dateTime);

  const handleDeleteReminder = async () => {
    const isConfirmed = window.confirm(
      `Are you sure you want to cancel reminder:\n${reminder.description}  ${formattedDate} ${formattedTime}`
    );

    if (isConfirmed) {
      try {
        const response = await apiClient.delete("/reminders", {
          params: {
            contactMethod: reminder.contactMethod,
            dateTime: reminder.dateTime,
          },
        });

        if (response.status === 204) {
          onDeleteReminder(index);
        } else {
          alert("This reminder cannot be deleted at this time.");
        }
      } catch (error) {
        console.error("Error deleting reminder:", error);
        alert("This reminder cannot be deleted at this time.");
      }
    }
  };

  return (
    <div className="reminder-card">
      <div className="reminder-card-description">{reminder.description}</div>
      <div className="reminder-card-date">{formattedDate}</div>
      <div className="reminder-card-time">{formattedTime}</div>
      <div className="reminder-card-contact">
        {reminder.contactMethod == "Text" ? <IoPhonePortrait /> : <MdEmail />}
      </div>
      <div className="reminder-card-cancel">
        <FcCancel onClick={handleDeleteReminder} />
      </div>
    </div>
  );
};

export default ReminderCard;
