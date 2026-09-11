import React from "react";
import { IoPhonePortrait } from "react-icons/io5";
import { MdEmail } from "react-icons/md";

interface ContactMethodProps {
  contactMethod: "Text" | "Email";
  setContactMethod: (method: "Text" | "Email") => void;
  isSMSEnabled: boolean;
}

const ContactMethod: React.FC<ContactMethodProps> = ({
  contactMethod,
  setContactMethod,
  isSMSEnabled,
}) => (
  <div className="reminder-add-modal-contact">
    <div className="reminder-add-modal-contact-method">
      <input
        id="contact-method-text"
        name="contact-method"
        type="radio"
        value="text"
        checked={contactMethod === "Text"}
        onChange={() => setContactMethod("Text")}
        disabled={!isSMSEnabled}
      />
      <label htmlFor="contact-method-text" title="Send by text message">
        <IoPhonePortrait className="reminder-add-modal-contact-icon" />
        <span className="visually-hidden">Send by text message</span>
      </label>
    </div>
    <div className="reminder-add-modal-contact-method">
      <input
        id="contact-method-email"
        name="contact-method"
        type="radio"
        value="email"
        checked={contactMethod === "Email"}
        onChange={() => setContactMethod("Email")}
      />
      <label htmlFor="contact-method-email" title="Send by email">
        <MdEmail className="reminder-add-modal-contact-icon" />
        <span className="visually-hidden">Send by email</span>
      </label>
    </div>
  </div>
);

export default ContactMethod;
