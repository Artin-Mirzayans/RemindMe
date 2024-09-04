export interface ReminderProps {
    contactMethod: "Email" | "Text";
    description: string;
    dateTime: string;
}