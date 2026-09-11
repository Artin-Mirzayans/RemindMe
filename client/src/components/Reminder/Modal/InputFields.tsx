import React, { useContext, useMemo } from "react";
import dayjs, { Dayjs } from "dayjs";
import relativeTime from "dayjs/plugin/relativeTime";
import { ThemeProvider, createTheme } from "@mui/material/styles";
import { AdapterDayjs } from "@mui/x-date-pickers/AdapterDayjs";
import { LocalizationProvider } from "@mui/x-date-pickers/LocalizationProvider";
import { DesktopDateTimePicker } from "@mui/x-date-pickers/DesktopDateTimePicker";
import { MobileDateTimePicker } from "@mui/x-date-pickers";
import PageSizeContext from "../../PageSizeContext";
import { buildQuickPresets } from "./quickPresets";

dayjs.extend(relativeTime);

// MUI's picker is the only place in the app that isn't styled from our own tokens - without
// this it renders in MUI's default blue, which clashes hard against the mobile picker's full
// screen dialog. Just enough theme to make it match.
const pickerTheme = createTheme({
  palette: {
    primary: { main: "#658352", dark: "#526b42" },
  },
});

const MAX_DESCRIPTION = 40;

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
  const isMobile = width <= 600;

  const presets = useMemo(() => buildQuickPresets(), []);
  const timeZone = useMemo(
    () => Intl.DateTimeFormat().resolvedOptions().timeZone,
    []
  );

  const selected = dateTime ? dayjs(dateTime) : null;

  const handleDateChange = (newValue: Dayjs | null) => {
    setDateTime(newValue ? newValue.toDate() : null);
  };

  const isPresetActive = (value: Dayjs) =>
    selected != null && selected.isSame(value, "minute");

  const Picker = isMobile ? MobileDateTimePicker : DesktopDateTimePicker;

  return (
    <ThemeProvider theme={pickerTheme}>
    <LocalizationProvider dateAdapter={AdapterDayjs}>
      <div className="reminder-add-modal-inputs">
        <input
          aria-disabled
          aria-label="Where the reminder will be sent"
          className="input"
          type="text"
          readOnly
          value={contactMethod == "Text" ? phoneNumber : email}
          placeholder="Contact"
          maxLength={20}
        />

        <div className="reminder-field">
          <input
            className="input"
            type="text"
            aria-label="Reminder description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Remind me to..."
            maxLength={MAX_DESCRIPTION}
          />
          <span className="reminder-field-hint" aria-live="polite">
            {description.length}/{MAX_DESCRIPTION}
          </span>
        </div>

        {presets.length > 0 && (
          <div
            className="reminder-presets"
            role="group"
            aria-label="Quick times"
          >
            {presets.map((preset) => (
              <button
                key={preset.label}
                type="button"
                className="reminder-preset"
                aria-pressed={isPresetActive(preset.value)}
                onClick={() => setDateTime(preset.value.toDate())}
              >
                {preset.label}
              </button>
            ))}
          </div>
        )}

        <Picker
          value={selected}
          onChange={handleDateChange}
          minDateTime={dayjs()}
          format="MMM D, YYYY  h:mm A"
          label="Date and time"
          slotProps={{
            textField: {
              fullWidth: true,
              inputProps: { spellCheck: false },
            },
          }}
        />

        <p className="reminder-field-summary">
          {selected ? (
            <>
              <strong>{selected.format("ddd, MMM D [at] h:mm A")}</strong>
              <span> · {selected.fromNow()}</span>
            </>
          ) : (
            <span>Pick a time above, or choose one of the shortcuts.</span>
          )}
          <span className="reminder-field-tz">{timeZone.replace(/_/g, " ")}</span>
        </p>
      </div>
    </LocalizationProvider>
    </ThemeProvider>
  );
};

export default InputFields;
