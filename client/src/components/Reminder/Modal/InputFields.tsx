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
}

const InputFields: React.FC<InputFieldsProps> = ({
  description,
  setDescription,
  dateTime,
  setDateTime,
}) => {
  const { width } = useContext(PageSizeContext);

  const handleDateChange = (newValue: Dayjs | null) => {
    setDateTime(newValue ? newValue.toDate() : null);
  };
  return (
    <>
      <LocalizationProvider dateAdapter={AdapterDayjs}>
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
      </LocalizationProvider>
    </>
  );
};
export default InputFields;
