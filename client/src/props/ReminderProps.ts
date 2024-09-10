export interface ReminderProps {
    userId?: string;
    contactMethod: "Email" | "Text";
    description: string;
    dateTime: string;
}