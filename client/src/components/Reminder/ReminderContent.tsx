import React, { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import ReminderAddButton from "./ReminderAddButton";
import ReminderAddModal, { ReminderPrefill } from "./Modal/ReminderAddModal";
import ReminderList from "./List/ReminderList";
import apiClient from "../Auth/apiClient";
import Loader from "../Loader/Loader";
import SignInPrompt from "../Auth/SignInPrompt";
import { useUser } from "../Auth/UserContext";
import { ReminderProps } from "../../props/ReminderProps";

import "./ReminderContent.css";

const ReminderContent = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, authLoading } = useUser();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [prefill, setPrefill] = useState<ReminderPrefill | null>(null);
  const [editingReminder, setEditingReminder] = useState<ReminderProps | null>(null);
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
    if (user) {
      fetchReminders();
    } else {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => {
    if (!user) return;

    const incoming = location.state?.prefill as ReminderPrefill | undefined;
    if (!incoming) return;

    setEditingReminder(null);
    setPrefill(incoming);
    setIsModalOpen(true);
    navigate(location.pathname, { replace: true, state: null });
  }, [location, navigate, user]);

  const sortByDateTime = (list: ReminderProps[]) =>
    [...list].sort(
      (a, b) => new Date(a.dateTime).getTime() - new Date(b.dateTime).getTime()
    );

  const onAddReminder = (reminderData: ReminderProps) => {
    setReminders((prevReminders) => sortByDateTime([...prevReminders, reminderData]));
  };

  const onSeriesCreated = (created: ReminderProps[]) => {
    setReminders((prevReminders) => sortByDateTime([...prevReminders, ...created]));
  };

  const onDeleteSeries = (seriesId: string) => {
    setReminders((prevReminders) =>
      prevReminders.filter((reminder) => reminder.seriesId !== seriesId)
    );
  };

  const onEditReminder = (original: ReminderProps, updated: ReminderProps) => {
    setReminders((prevReminders) =>
      sortByDateTime(
        prevReminders.map((reminder) =>
          reminder.contactMethod === original.contactMethod &&
          reminder.dateTime === original.dateTime
            ? updated
            : reminder
        )
      )
    );
  };

  const onDeleteReminder = (index: number) => {
    setReminders((prevReminders) => {
      return prevReminders.filter((_, i) => i !== index);
    });
  };

  const openAddModal = () => {
    setEditingReminder(null);
    setIsModalOpen(true);
  };

  const openEditModal = (reminder: ReminderProps) => {
    setEditingReminder(reminder);
    setIsModalOpen(true);
  };

  const closeModal = () => {
    setIsModalOpen(false);
    setPrefill(null);
    setEditingReminder(null);
  };
  if (authLoading || loading) {
    return (
      <div className="reminder-content">
        <div className="reminders-loader">
          <Loader />
        </div>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="reminder-content">
        <div className="content-title">Reminders</div>
        <SignInPrompt
          title="Sign in to create reminders"
          description="RemindMe sends you a text or email when it's time - sign in with Google to get started."
        />
      </div>
    );
  }

  return (
    <div className="reminder-content">
      <div className="content-title">Reminders</div>
      <ReminderAddButton
        remindersCount={reminders.length}
        openModal={openAddModal}
      />
      <ReminderAddModal
        reminders={reminders}
        isOpen={isModalOpen}
        prefill={prefill}
        editing={editingReminder}
        onClose={closeModal}
        onAddReminder={onAddReminder}
        onEditReminder={onEditReminder}
        onSeriesCreated={onSeriesCreated}
      />
      <ReminderList
        reminders={reminders}
        onDeleteReminder={onDeleteReminder}
        onEditReminder={openEditModal}
        onDeleteSeries={onDeleteSeries}
      />
    </div>
  );
};

export default ReminderContent;
