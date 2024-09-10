import React, { useContext } from "react";
import dayjs, { Dayjs } from "dayjs";
import { AdapterDayjs } from "@mui/x-date-pickers/AdapterDayjs";
import { LocalizationProvider } from "@mui/x-date-pickers/LocalizationProvider";
import { DesktopDateTimePicker } from "@mui/x-date-pickers/DesktopDateTimePicker";
import { MobileDateTimePicker } from "@mui/x-date-pickers";
import PageSizeContext from "../../PageSizeContext";

interface InputFieldsProps {
  description: string;
  setDescription: (value: string) => void;
  dateTime: Date | null;
  setDateTime: (dateTime: Date | null) => void;
  contactMethod: "Email" | "Text";
  email: string;
  phoneNumber: string | null;
}

const InputFields: React.FC<InputFieldsProps> = ({
  description,
  setDescription,
  dateTime,
  setDateTime,
  contactMethod,
  email,
  phoneNumber,
}) => {
  const { width } = useContext(PageSizeContext);

  const handleDateChange = (newValue: Dayjs | null) => {
    setDateTime(newValue ? newValue.toDate() : null);
  };
  return (
    <>
      <LocalizationProvider dateAdapter={AdapterDayjs}>
        <div className="reminder-add-modal-inputs">
          <input
            aria-disabled
            className="reminder-add-modal-input"
            type="text"
            readOnly
            value={contactMethod == "Text" ? phoneNumber : email}
            placeholder="Contact"
            maxLength={20}
          />
          <input
            className="reminder-add-modal-input"
            type="text"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Description"
            maxLength={20}
          />
          {width <= 600 ? (
            <MobileDateTimePicker
              value={dateTime == null ? dayjs() : dayjs(dateTime)}
              onChange={handleDateChange}
              minDateTime={dayjs()}
            />
          ) : (
            <DesktopDateTimePicker
              value={dateTime == null ? dayjs() : dayjs(dateTime)}
              onChange={handleDateChange}
              minDateTime={dayjs()}
            />
          )}
        </div>
      </LocalizationProvider>
    </>
  );
};
export default InputFields;
