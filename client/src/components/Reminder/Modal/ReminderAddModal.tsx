import React, { useEffect, useReducer, useState } from "react";
import Modal from "react-modal";
import apiClient from "../../Auth/apiClient";
import ContactMethod from "./ContactMethod";
import InputFields from "./InputFields";
import ClockLoader from "react-spinners/ClockLoader";
import { useUser } from "../../Auth/UserContext";
import { ReminderProps } from "../../../props/ReminderProps";
import { FaRegCircleCheck } from "react-icons/fa6";
import { MdCancel } from "react-icons/md";
import { fromZonedTime } from "date-fns-tz";

import "./ReminderAddModal.css";

export interface ReminderPrefill {
  description: string;
  dateTime: string | null;
}

export type RepeatFrequency = "NONE" | "DAILY" | "WEEKLY" | "MONTHLY";

interface ReminderAddModalProps {
  reminders: ReminderProps[];
  isOpen: boolean;
  onClose: () => void;
  onAddReminder: (reminder: ReminderProps) => void;
  onEditReminder: (original: ReminderProps, updated: ReminderProps) => void;
  onSeriesCreated: (created: ReminderProps[]) => void;
  prefill?: ReminderPrefill | null;
  editing?: ReminderProps | null;
}

interface State {
  description: string;
  contactMethod: "Email" | "Text";
  dateTime: Date | null;
  frequency: RepeatFrequency;
  occurrences: number;
}

type Action =
  | { type: "SET_DESCRIPTION"; payload: string }
  | { type: "SET_CONTACT_METHOD"; payload: "Email" | "Text" }
  | { type: "SET_DATETIME"; payload: Date | null }
  | { type: "SET_FREQUENCY"; payload: RepeatFrequency }
  | { type: "SET_OCCURRENCES"; payload: number }
  | { type: "APPLY_PREFILL"; payload: ReminderPrefill }
  | { type: "APPLY_EDIT"; payload: ReminderProps }
  | { type: "RESET" };

// a reasonable default per frequency - a couple weeks of daily, a month of weekly, a quarter
// of monthly - so the user doesn't have to think about it unless they want something different
const DEFAULT_OCCURRENCES: Record<RepeatFrequency, number> = {
  NONE: 1,
  DAILY: 7,
  WEEKLY: 4,
  MONTHLY: 3,
};

const initialState = (isVerified: boolean): State => ({
  description: "",
  contactMethod: isVerified ? "Text" : "Email",
  dateTime: null,
  frequency: "NONE",
  occurrences: DEFAULT_OCCURRENCES.NONE,
});

const reducer = (state: State, action: Action): State => {
  switch (action.type) {
    case "SET_DESCRIPTION":
      return { ...state, description: action.payload };
    case "SET_CONTACT_METHOD":
      return { ...state, contactMethod: action.payload };
    case "SET_DATETIME":
      return { ...state, dateTime: action.payload };
    case "SET_FREQUENCY":
      return {
        ...state,
        frequency: action.payload,
        occurrences: DEFAULT_OCCURRENCES[action.payload],
      };
    case "SET_OCCURRENCES":
      return { ...state, occurrences: action.payload };
    case "APPLY_PREFILL":
      return {
        ...state,
        description: action.payload.description,
        dateTime: action.payload.dateTime
          ? new Date(action.payload.dateTime)
          : state.dateTime,
      };
    case "APPLY_EDIT":
      return {
        ...state,
        description: action.payload.description,
        contactMethod: action.payload.contactMethod,
        dateTime: new Date(action.payload.dateTime),
      };
    case "RESET":
      return initialState(state.contactMethod === "Text");
    default:
      return state;
  }
};

const FREQUENCY_LABELS: Record<RepeatFrequency, string> = {
  NONE: "Doesn't repeat",
  DAILY: "Daily",
  WEEKLY: "Weekly",
  MONTHLY: "Monthly",
};

const OCCURRENCE_OPTIONS = Array.from({ length: 11 }, (_, i) => i + 2); // 2..12

const ReminderAddModal: React.FC<ReminderAddModalProps> = ({
  reminders,
  isOpen,
  onClose,
  onAddReminder,
  onEditReminder,
  onSeriesCreated,
  prefill,
  editing,
}) => {
  const { user } = useUser();
  const [errorMessage, setErrorMessage] = useState<string | null>(
    user.isVerified
      ? null
      : "To enable text reminders, please verify Phone Number in the profile tab."
  );
  const [state, dispatch] = useReducer(reducer, initialState(user.isVerified));

  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!isOpen) return;
    if (editing) {
      dispatch({ type: "APPLY_EDIT", payload: editing });
    } else if (prefill) {
      dispatch({ type: "APPLY_PREFILL", payload: prefill });
    }
  }, [isOpen, editing, prefill]);

  const submitReminder = async (reminderData: {
    contactMethod: "Email" | "Text";
    description: string;
    dateTime: string;
  }) => {
    if (editing) {
      const response = await apiClient.put("/reminders", reminderData, {
        params: {
          contactMethod: editing.contactMethod,
          dateTime: editing.dateTime,
        },
      });
      return response.data;
    }

    const response = await apiClient.post("/reminders", reminderData);
    return response.data;
  };

  const submitSeries = async (reminderData: {
    contactMethod: "Email" | "Text";
    description: string;
    dateTime: string;
  }): Promise<ReminderProps[]> => {
    const response = await apiClient.post("/reminders/series", {
      reminder: reminderData,
      frequency: state.frequency,
      occurrences: state.occurrences,
    });
    return response.data;
  };

  const handleAddReminder = async () => {
    setErrorMessage(null);
    const timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
    const isSeries = !editing && state.frequency !== "NONE";

    if (state.description.length < 3 || state.description.length > 40) {
      setErrorMessage("Description must be between 3 and 40 characters.");
      return;
    }

    if (!state.dateTime) {
      setErrorMessage(
        isSeries
          ? "Choose a date and time for the first reminder."
          : "Choose a date and time for this reminder."
      );
      return;
    }

    const utcDateTime = fromZonedTime(state.dateTime, timeZone);
    const dateTime = utcDateTime.toISOString().split(".")[0] + "Z";

    const isBeingEdited = (reminder: ReminderProps) =>
      editing != null &&
      reminder.contactMethod === editing.contactMethod &&
      reminder.dateTime === editing.dateTime;

    const existingReminder = reminders.find(
      (reminder) =>
        reminder.dateTime === dateTime &&
        reminder.userId === user.email &&
        !isBeingEdited(reminder)
    );

    if (existingReminder) {
      setErrorMessage(
        "A reminder with the same date/time has already been created"
      );
      return;
    }

    const reminderData = {
      contactMethod: state.contactMethod,
      description: state.description,
      dateTime: dateTime,
    };

    setLoading(true);

    try {
      if (isSeries) {
        const created = await submitSeries(reminderData);
        onSeriesCreated(created);

        if (created.length < state.occurrences) {
          setErrorMessage(
            `Created ${created.length} of ${state.occurrences} requested - you're at the 15 reminder limit. ` +
              "Cancel a few reminders to make room for the rest."
          );
          return;
        }

        dispatch({ type: "RESET" });
        onClose();
        return;
      }

      await submitReminder(reminderData);
      if (editing) {
        onEditReminder(editing, reminderData);
      } else {
        onAddReminder(reminderData);
      }
      dispatch({ type: "RESET" });
      onClose();
    } catch (error) {
      console.error("Failed to save reminder:", error);
      if (error.response?.status === 409) {
        setErrorMessage(
          "You're already at the 15 reminder limit - cancel a few before starting a new series."
        );
      } else if (error.response?.status === 400) {
        setErrorMessage(
          "That reminder was rejected. Check the description length and make sure the time is in the future."
        );
      } else if (editing && error.response?.status === 404) {
        setErrorMessage(
          "This reminder no longer exists - it may have already fired or been cancelled elsewhere."
        );
      } else {
        setErrorMessage(
          editing
            ? "Something went wrong saving those changes. Please try again."
            : "Something went wrong creating that reminder. Please try again."
        );
      }
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    dispatch({ type: "RESET" });
    onClose();
  };

  return (
    <Modal
      isOpen={isOpen}
      onRequestClose={handleCancel}
      contentLabel="Add Reminder Modal"
      className="reminder-modal"
      overlayClassName="reminder-modal-overlay"
      appElement={document.getElementById("root")}
    >
      <div className="reminder-add-modal-title">
        {editing ? "Edit Reminder" : "New Reminder"}
      </div>
      <ContactMethod
        contactMethod={state.contactMethod}
        setContactMethod={(method) =>
          dispatch({ type: "SET_CONTACT_METHOD", payload: method })
        }
        isSMSEnabled={user.isVerified}
      />
      {errorMessage && <div className="form-error">{errorMessage}</div>}
      <InputFields
        description={state.description}
        setDescription={(desc) =>
          dispatch({ type: "SET_DESCRIPTION", payload: desc })
        }
        dateTime={state.dateTime}
        setDateTime={(dateTime) =>
          dispatch({ type: "SET_DATETIME", payload: dateTime })
        }
        contactMethod={state.contactMethod}
        email={user.email}
        phoneNumber={user.phoneNumber}
      />
      {!editing && (
        <div className="reminder-repeat">
          <span className="reminder-repeat-label">Repeat</span>
          <div
            className="reminder-presets"
            role="group"
            aria-label="Repeat frequency"
          >
            {(Object.keys(FREQUENCY_LABELS) as RepeatFrequency[]).map((freq) => (
              <button
                key={freq}
                type="button"
                className="reminder-preset"
                aria-pressed={state.frequency === freq}
                onClick={() => dispatch({ type: "SET_FREQUENCY", payload: freq })}
              >
                {FREQUENCY_LABELS[freq]}
              </button>
            ))}
          </div>
          {state.frequency !== "NONE" && (
            <label className="reminder-repeat-count">
              For
              <select
                className="reminder-repeat-select"
                value={state.occurrences}
                onChange={(e) =>
                  dispatch({
                    type: "SET_OCCURRENCES",
                    payload: Number(e.target.value),
                  })
                }
              >
                {OCCURRENCE_OPTIONS.map((n) => (
                  <option key={n} value={n}>
                    {n} times
                  </option>
                ))}
              </select>
            </label>
          )}
        </div>
      )}
      {loading && (
        <div className="reminder-add-modal-loading">
          <ClockLoader size={28} color="var(--color-primary)" />
        </div>
      )}
      <div className="reminder-add-modal-buttons">
        <button
          type="button"
          className="btn btn--ghost"
          onClick={handleCancel}
        >
          <MdCancel size={20} />
          Cancel
        </button>
        <button
          type="button"
          className="btn"
          onClick={handleAddReminder}
          disabled={loading}
        >
          <FaRegCircleCheck size={20} />
          {editing
            ? loading
              ? "Saving..."
              : "Save changes"
            : loading
              ? "Creating..."
              : "Create"}
        </button>
      </div>
    </Modal>
  );
};

export default ReminderAddModal;
