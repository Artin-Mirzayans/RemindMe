export interface ReminderProps {
    contactMethod: "Email" | "Text";
    description: string;
    utcDateTimeString: string;
}