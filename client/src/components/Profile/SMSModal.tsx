import React, { useContext, useMemo, useState, useEffect } from "react";
import Modal from "react-modal";
import PageSizeContext from "../PageSizeContext";
import apiClient from "../Auth/apiClient";
import { User } from "../Auth/UserContext";
import { VscDebugRestart } from "react-icons/vsc";

import "./SMSModal.css";

Modal.setAppElement("#root");

interface SMSModalProps {
  isOpen: boolean;
  onRequestClose: () => void;
  onResendCode: (phoneNumber: string) => void;
  user: User;
  sms: string;
  errorMessage?: string;
  setErrorMessage: React.Dispatch<React.SetStateAction<string | null>>;
}

const SMSModal: React.FC<SMSModalProps> = ({
  isOpen,
  onRequestClose,
  onResendCode,
  sms,
  errorMessage,
  setErrorMessage,
}) => {
  const { width } = useContext(PageSizeContext);
  const iconSize = useMemo(() => {
    if (width >= 1070) return 36;
    else if (width > 500) return 30;
    else return 22;
  }, [width]);

  const [otpCode, setOtpCode] = useState<string>("");

  useEffect(() => {
    if (otpCode.length === 6) {
      validateOtp(otpCode);
    }
  }, [otpCode]);

  const handleResendClick = () => {
    onResendCode(sms);
  };

  const handleOtpChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value.replace(/\D/g, "").slice(0, 6);
    setOtpCode(value);
  };

  const validateOtp = async (otpCode: string) => {
    const response = await apiClient.post("/otp/validate", null, {
      params: {
        otpCode: otpCode,
      },
    });

    if (response.status == 200) window.location.reload();
    else setErrorMessage("Code does not match");
  };

  return (
    <Modal
      isOpen={isOpen}
      onRequestClose={onRequestClose}
      contentLabel="Send SMS"
      className="modal"
      overlayClassName="modal-overlay"
    >
      <div className="SMSModal">
        <h2 className="SMSModal-title">Verify Your Phone Number</h2>
        <div className="SMSModal-content">
          <input
            className="SMSModal-sms-input"
            type="text"
            placeholder={sms}
            value={sms}
            readOnly
          />
        </div>
        <div className="SMSModal-instructions">
          Enter the code we sent to your phone to complete the verification
          process
        </div>
        <div className="SMSModal-content">
          <input
            pattern="\d*"
            maxLength={6}
            placeholder="code"
            className="SMSModal-input"
            value={otpCode}
            onChange={handleOtpChange}
          />
        </div>
        {errorMessage && <div className="SMSModal-error">{errorMessage}</div>}
        <button className="SMSModal-resend-button" onClick={handleResendClick}>
          <VscDebugRestart size={iconSize} />
        </button>
      </div>
    </Modal>
  );
};

export default SMSModal;
