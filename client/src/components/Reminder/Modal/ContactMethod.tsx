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
  <>
    <div className="reminder-add-modal-contact">
      <div className="reminder-add-modal-contact-method">
        <input
          type="radio"
          value="text"
          checked={contactMethod === "Text"}
          onChange={() => setContactMethod("Text")}
          disabled={!isSMSEnabled}
        />
        <label>
          <IoPhonePortrait className="reminder-add-modal-contact-icon" />
        </label>
      </div>
      <div className="reminder-add-modal-contact-method">
        <input
          type="radio"
          value="email"
          checked={contactMethod === "Email"}
          onChange={() => setContactMethod("Email")}
        />
        <label>
          <MdEmail className="reminder-add-modal-contact-icon" />
        </label>
      </div>
    </div>
  </>
);

export default ContactMethod;
