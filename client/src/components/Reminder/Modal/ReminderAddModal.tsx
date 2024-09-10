import React, { useReducer, useMemo, useContext, useState } from "react";
import Modal from "react-modal";
import apiClient from "../../Auth/apiClient";
import ModalStyles from "./ModalStyles";
import ModalStylesMobile from "./ModalStyles-mobile";
import ContactMethod from "./ContactMethod";
import InputFields from "./InputFields";
import PageSizeContext from "../../PageSizeContext";
import ClockLoader from "react-spinners/ClockLoader";
import { useUser } from "../../Auth/UserContext";
import { ReminderProps } from "../../../props/ReminderProps";
import { FaRegCircleCheck } from "react-icons/fa6";
import { MdCancel } from "react-icons/md";
import { fromZonedTime } from "date-fns-tz";

import "./ReminderAddModal.css";
import "react-datepicker/dist/react-datepicker.css";

interface ReminderAddModalProps {
  reminders: ReminderProps[];
  isOpen: boolean;
  onClose: () => void;
  onAddReminder: (reminder: ReminderProps) => void;
}

interface State {
  description: string;
  contactMethod: "Email" | "Text";
  dateTime: Date | null;
}

type Action =
  | { type: "SET_DESCRIPTION"; payload: string }
  | { type: "SET_CONTACT_METHOD"; payload: "Email" | "Text" }
  | { type: "SET_DATETIME"; payload: Date | null }
  | { type: "RESET" };

const initialState = (isVerified: boolean): State => ({
  description: "",
  contactMethod: isVerified ? "Text" : "Email",
  dateTime: null,
});

const reducer = (state: State, action: Action): State => {
  switch (action.type) {
    case "SET_DESCRIPTION":
      return { ...state, description: action.payload };
    case "SET_CONTACT_METHOD":
      return { ...state, contactMethod: action.payload };
    case "SET_DATETIME":
      return { ...state, dateTime: action.payload };
    case "RESET":
      return initialState(state.contactMethod === "Text");
    default:
      return state;
  }
};

const ReminderAddModal: React.FC<ReminderAddModalProps> = ({
  reminders,
  isOpen,
  onClose,
  onAddReminder,
}) => {
  const { width } = useContext(PageSizeContext);
  const { user } = useUser();
  const [errorMessage, setErrorMessage] = useState<string | null>(
    user.isVerified
      ? null
      : "To enable text reminders, please verify Phone Number in the profile tab."
  );
  const [state, dispatch] = useReducer(reducer, initialState(user.isVerified));

  const [loading, setLoading] = useState(false);

  const modalStyles = useMemo(() => {
    return width <= 1070 ? ModalStylesMobile(width) : ModalStyles;
  }, [width]);

  const iconSize = useMemo(() => {
    if (width >= 1070) return 36;
    else if (width > 500) return 30;
    else return 22;
  }, [width]);

  const createReminder = async (reminderData: {
    contactMethod: "Email" | "Text";
    description: string;
    dateTime: string;
  }) => {
    const response = await apiClient.post("/reminders", reminderData);
    return response.data;
  };

  const handleAddReminder = async () => {
    setErrorMessage(null);
    const timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;

    if (state.description.length < 3 || state.description.length > 20) {
      setErrorMessage("Description must be between 3 and 20 characters long.");
      return;
    }

    if (!state.dateTime) {
      setErrorMessage("Selected Date/Time must be in the future");
      return;
    }

    const utcDateTime = fromZonedTime(state.dateTime, timeZone);
    const dateTime = utcDateTime.toISOString().split(".")[0] + "Z";

    const existingReminder = reminders.find(
      (reminder) =>
        reminder.dateTime === dateTime && reminder.userId === user.email
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
    createReminder(reminderData)
      .then(() => {
        onAddReminder(reminderData);
        setLoading(false);
      })
      .catch((error) => {
        setLoading(false);
        console.error("Failed to create reminder:", error);
        if (error.response?.status === 400)
          alert(
            "Description must be at least 3 characters\nDate/Time must be in the future"
          );
        else {
          alert("Unknown Error");
        }
      });

    dispatch({ type: "RESET" });
    onClose();
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
      style={modalStyles}
      appElement={document.getElementById("root")}
    >
      <div className="reminder-add-modal-title">New Reminder</div>
      <ContactMethod
        contactMethod={state.contactMethod}
        setContactMethod={(method) =>
          dispatch({ type: "SET_CONTACT_METHOD", payload: method })
        }
        isSMSEnabled={user.isVerified}
      />
      {errorMessage && (
        <div className="reminder-modal-error-message">{errorMessage}</div>
      )}
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
      {loading && (
        <div className="">
          <ClockLoader size={iconSize} />
        </div>
      )}
      <div className="reminder-add-modal-buttons">
        <button onClick={handleCancel}>
          <MdCancel size={iconSize} />
        </button>
        <button onClick={handleAddReminder}>
          <FaRegCircleCheck size={iconSize} />
        </button>
      </div>
    </Modal>
  );
};

export default ReminderAddModal;
