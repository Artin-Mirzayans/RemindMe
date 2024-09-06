import React, { useState, useEffect, useMemo, useContext } from "react";
import SMSModal from "./SMSModal";
import formatPhoneNumber from "./formatPhoneNumber";
import renderVerificationStatusIcon from "./renderVerificationStatusIcon";
import apiClient from "../Auth/apiClient";
import PageSizeContext from "../PageSizeContext";
import { useUser } from "../Auth/UserContext";

import { IoOpenOutline } from "react-icons/io5";
import "./ProfileContent.css";

const ProfileContent: React.FC = () => {
  const { width } = useContext(PageSizeContext);
  const { user } = useUser();
  const [placeholder, setPlaceholder] = useState("(123) 456-7890");
  const [sms, setSms] = useState<string>(
    user?.phoneNumber ? formatPhoneNumber(user.phoneNumber) : ""
  );
  const [verificationStatus, setVerificationStatus] = useState(() => {
    if (user?.isVerified) return "verified";
    if (user?.phoneNumber) return "pending";
    return "notVerified";
  });
  const [modalIsOpen, setModalIsOpen] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const iconSize = useMemo(() => {
    if (width >= 1070) return 36;
    else if (width > 500) return 30;
    else return 22;
  }, [width]);

  useEffect(() => {
    if (
      sms &&
      sms.length === 14 &&
      user?.phoneNumber == null &&
      !user.isVerified
    ) {
      sendCode(sms);
    }
  }, [sms]);

  const handleSmsChange = (e) => {
    const formattedPhoneNumber = formatPhoneNumber(e.target.value);
    setSms(formattedPhoneNumber);
  };

  const handleFocus = () => {
    setPlaceholder("");
  };

  const handleBlur = (e) => {
    if (e.target.value === "") {
      setPlaceholder("(123) 456-7890");
    }
  };

  const openModal = () => {
    setModalIsOpen(true);
    setErrorMessage(null);
  };

  const closeModal = () => {
    setModalIsOpen(false);
  };

  const sendCode = async (phoneNumber: string) => {
    apiClient
      .post("/otp/send", null, {
        params: {
          phoneNumber: phoneNumber,
        },
      })
      .then((response) => {
        if (response.status === 200) {
          setVerificationStatus("pending");
          openModal();
        }
      })
      .catch((error) => {
        if (error)
          setErrorMessage(
            "Please wait 30 seconds to receive another verification code"
          );
      });
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
            value={user.email}
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
            placeholder={placeholder}
            value={sms}
            onChange={handleSmsChange}
            onFocus={handleFocus}
            onBlur={handleBlur}
            pattern="\d*"
          />
          {renderVerificationStatusIcon(verificationStatus)}
        </div>
      </div>

      {verificationStatus == "verified" && (
        <div className="profile-content-verified">
          You are verified to receive reminders by e-mail & text message.
        </div>
      )}

      {verificationStatus == "pending" && (
        <div className="profile-content-button">
          <button onClick={openModal}>
            <IoOpenOutline size={iconSize} />
          </button>
        </div>
      )}

      {verificationStatus == "notVerified" && (
        <div className="profile-content-not-verified">
          Enter your phone number to receive a verification code.
        </div>
      )}

      <SMSModal
        isOpen={modalIsOpen}
        onRequestClose={closeModal}
        onResendCode={sendCode}
        user={user}
        sms={sms}
        errorMessage={errorMessage}
        setErrorMessage={setErrorMessage}
      />
    </div>
  );
};

export default ProfileContent;
