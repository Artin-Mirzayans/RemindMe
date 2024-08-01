import React from "react";
import DatePicker from "react-datepicker";
import getMinTime from "./getMinTime";

interface InputFieldsProps {
  description: string;
  setDescription: (value: string) => void;
  date: Date | null;
  setDate: (date: Date | null) => void;
  time: Date | null;
  setTime: (dateTime: Date | null) => void;
}

const InputFields: React.FC<InputFieldsProps> = ({
  description,
  setDescription,
  date,
  setDate,
  time,
  setTime,
}) => (
  <>
    <input
      className="reminder-add-modal-input"
      type="text"
      value={description}
      onChange={(e) => setDescription(e.target.value)}
      placeholder="Description"
    />
    <DatePicker
      className="reminder-add-modal-input"
      placeholderText="Date"
      selected={date}
      onChange={(date: Date | null) => setDate(date)}
      dateFormat="MMMM d, yyyy"
      minDate={new Date()}
    />

    <DatePicker
      className="reminder-add-modal-input"
      placeholderText="Time"
      selected={time}
      onChange={(time: Date | null) => setTime(time)}
      showTimeSelect
      showTimeSelectOnly
      timeIntervals={15}
      timeCaption="Time"
      dateFormat="h:mm aa"
      minTime={getMinTime(date)}
      maxTime={new Date(new Date().setHours(23, 59, 59))}
    />
  </>
);

export default InputFields;
