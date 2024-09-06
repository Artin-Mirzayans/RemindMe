import React, { useEffect, useState } from "react";
import ReminderAddButton from "./ReminderAddButton";
import ReminderAddModal from "./Modal/ReminderAddModal";
import ReminderList from "./List/ReminderList";
import { ReminderProps } from "../../props/ReminderProps";

import "./ReminderContent.css";
import apiClient from "../Auth/apiClient";

const ReminderContent = () => {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [reminders, setReminders] = useState<ReminderProps[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchReminders = async () => {
    try {
      const response = await apiClient.get("/reminders");
      setReminders(response.data);
    } catch (err) {
      console.log("Error fetching reminders: " + err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReminders();
  }, []);

  const onAddReminder = (reminderData) => {
    const newReminders = [...reminders, reminderData];

    const sortedReminders = newReminders.sort(
      (a, b) => new Date(a.dateTime).getTime() - new Date(b.dateTime).getTime()
    );

    setReminders(sortedReminders);
  };

  const onDeleteReminder = (index: number) => {
    setReminders((prevReminders) => {
      return prevReminders.filter((_, i) => i !== index);
    });
  };
  return (
    <div className="reminder-content">
      <div className="content-title">RemindMe</div>
      <ReminderAddButton
        remindersCount={reminders.length}
        openModal={() => setIsModalOpen(true)}
      />
      <ReminderAddModal
        reminders={reminders}
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onAddReminder={onAddReminder}
      />
      {loading ? (
        <p>Loading...</p>
      ) : (
        <ReminderList
          reminders={reminders}
          onDeleteReminder={onDeleteReminder}
        />
      )}
    </div>
  );
};

export default ReminderContent;
