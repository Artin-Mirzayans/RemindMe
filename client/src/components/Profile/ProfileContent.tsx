import React, { useState } from "react";
import formatPhoneNumber from "./formatPhoneNumber";
import renderVerificationStatusIcon from "./renderVerificationStatusIcon";

import "./ProfileContent.css";

interface ProfileContentProps {
  email?: string;
  verificationStatus?: "verified" | "notVerified" | "pending";
}

const ProfileContent: React.FC<ProfileContentProps> = ({
  email = "johndoe12345@gmail.com",
  verificationStatus = "pending",
}) => {
  const [sms, setSms] = useState("");

  const handleSmsChange = (e) => {
    const formattedPhoneNumber = formatPhoneNumber(e.target.value);
    setSms(formattedPhoneNumber);
  };

  return (
    <div className="profile-content">
      <div className="content-title">Profile</div>

      <div className="profile-content-inputs">
        <div className="profile-content-input">
          <label className="profile-content-input-label" htmlFor="email">
            Email:
          </label>
          <input
            className="profile-content-input-field"
            type="email"
            id="email"
            value={email}
            readOnly
          />
          {renderVerificationStatusIcon("verified")}
        </div>

        <div className="profile-content-input">
          <label className="profile-content-input-label" htmlFor="sms">
            Phone:
          </label>
          <input
            className="profile-content-input-field"
            type="text"
            id="sms"
            placeholder="(123) 456-7890"
            value={sms}
            onChange={handleSmsChange}
            pattern="\d*"
          />
          {renderVerificationStatusIcon(verificationStatus)}
        </div>
      </div>
    </div>
  );
};

export default ProfileContent;
