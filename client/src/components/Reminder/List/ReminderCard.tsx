import React from "react";
import UTCToZoned from "./UTCtoZoned";
import { ReminderProps } from "../../../props/ReminderProps";
import { IoPhonePortrait } from "react-icons/io5";
import { MdEmail } from "react-icons/md";
import { FcCancel } from "react-icons/fc";

import "./ReminderCard.css";

interface ReminderCardProps {
  reminder: ReminderProps;
}

const ReminderCard: React.FC<ReminderCardProps> = ({ reminder }) => {
  console.log(reminder);

  const { formattedDate, formattedTime } = UTCToZoned(
    reminder.utcDateTimeString
  );

  return (
    <div className="reminder-card">
      <div className="reminder-card-description">{reminder.description}</div>
      <div className="reminder-card-date">{formattedDate}</div>
      <div className="reminder-card-time">{formattedTime}</div>
      <div className="reminder-card-contact">
        {reminder.contactMethod == "Text" ? <IoPhonePortrait /> : <MdEmail />}
      </div>
      <div className="reminder-card-cancel">
        <FcCancel />
      </div>
    </div>
  );
};

export default ReminderCard;
