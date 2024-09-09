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
import { format } from "date-fns";
import { fromZonedTime } from "date-fns-tz";

import "./ReminderAddModal.css";
import "react-datepicker/dist/react-datepicker.css";

interface ReminderAddModalProps {
  reminders;
  isOpen: boolean;
  onClose: () => void;
  onAddReminder: (reminder: ReminderProps) => void;
}

interface State {
  description: string;
  contactMethod: "Email" | "Text";
  date: Date | null;
  time: Date | null;
}

type Action =
  | { type: "SET_DESCRIPTION"; payload: string }
  | { type: "SET_CONTACT_METHOD"; payload: "Email" | "Text" }
  | { type: "SET_DATE"; payload: Date | null }
  | { type: "SET_TIME"; payload: Date | null }
  | { type: "RESET" };

const initialState = (isVerified: boolean): State => ({
  description: "",
  contactMethod: isVerified ? "Text" : "Email",
  date: null,
  time: null,
});

const reducer = (state: State, action: Action): State => {
  switch (action.type) {
    case "SET_DESCRIPTION":
      return { ...state, description: action.payload };
    case "SET_CONTACT_METHOD":
      return { ...state, contactMethod: action.payload };
    case "SET_DATE":
      return { ...state, date: action.payload };
    case "SET_TIME":
      return { ...state, time: action.payload };
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
    let dateTime: string;
    const timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
    if (state.date && state.time) {
      const localDateTime = new Date(
        `${format(state.date, "yyyy-MM-dd")}T${format(state.time, "HH:mm:ss")}`
      );
      const utcDateTime = fromZonedTime(localDateTime, timeZone);
      dateTime = utcDateTime.toISOString().split(".")[0] + "Z";
    } else {
      console.log("date/time invalid");
      return;
    }

    const existingReminder = reminders.find(
      (reminder) =>
        reminder.dateTime === dateTime && reminder.userId === user.email
    );

    if (existingReminder) {
      alert("A reminder with the same date/time has already been created");
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
        if (error.response?.status == 400)
          alert(
            "Description must be at least 3 characters\nDate/Time must be in future"
          );
        else {
          alert("Unknown Error");
        }
      });

    dispatch({ type: "RESET" });
    onClose();
  };

  return (
    <Modal
      isOpen={isOpen}
      onRequestClose={onClose}
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
      <InputFields
        description={state.description}
        setDescription={(desc) =>
          dispatch({ type: "SET_DESCRIPTION", payload: desc })
        }
        date={state.date}
        setDate={(date) => dispatch({ type: "SET_DATE", payload: date })}
        time={state.time}
        setTime={(time) => dispatch({ type: "SET_TIME", payload: time })}
      />
      {loading && (
        <div className="">
          <ClockLoader size={iconSize} />
        </div>
      )}
      <div className="reminder-add-modal-buttons">
        <button onClick={onClose}>
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
