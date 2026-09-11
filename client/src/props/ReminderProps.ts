export interface ReminderProps {
    userId?: string;
    contactMethod: "Email" | "Text";
    description: string;
    dateTime: string;
    // set when this is one occurrence of a recurring series, shared with every other occurrence
    // so the whole run can be cancelled together
    seriesId?: string | null;
}