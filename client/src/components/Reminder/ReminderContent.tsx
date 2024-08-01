import React, { useState } from "react";
import ReminderAddButton from "./ReminderAddButton";
import ReminderAddModal from "./Modal/ReminderAddModal";
import ReminderList from "./List/ReminderList";
import RemindersData from "./List/RemindersData.json";
import { ReminderProps } from "../../props/ReminderProps";

import "./ReminderContent.css";

const ReminderContent = () => {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [reminders, setReminders] = useState<ReminderProps[]>([]);
  const Reminders: ReminderProps[] = RemindersData as ReminderProps[];

  const handleAddReminder = (reminderData) => {
    const newReminders = [...reminders, reminderData];
    const sortedReminders = newReminders.sort(
      (a, b) => a.date.getTime() - b.date.getTime()
    );

    setReminders(sortedReminders);
    console.log(sortedReminders);
  };
  return (
    <div className="reminder-content">
      <div className="content-title">RemindMe</div>
      <ReminderAddButton openModal={() => setIsModalOpen(true)} />
      <ReminderAddModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onAddReminder={handleAddReminder}
        isSMSEnabled={false}
      />
      <ReminderList reminders={Reminders} />
    </div>
  );
};

export default ReminderContent;
