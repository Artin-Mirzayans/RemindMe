import React from "react";
import { MdCheckCircle, MdCancel, MdWatchLater } from "react-icons/md";

const STATUS = {
  verified: {
    Icon: MdCheckCircle,
    color: "var(--color-primary)",
    label: "Verified",
  },
  notVerified: {
    Icon: MdCancel,
    color: "var(--color-danger)",
    label: "Not verified",
  },
  pending: {
    Icon: MdWatchLater,
    color: "var(--color-accent)",
    label: "Verification pending",
  },
} as const;

const renderVerificationStatusIcon = (verificationStatus: string) => {
  const status = STATUS[verificationStatus as keyof typeof STATUS];
  if (!status) return null;

  const { Icon, color, label } = status;

  return (
    <span className="profile-content-status-icon" style={{ color }}>
      <Icon role="img" aria-label={label} title={label} />
    </span>
  );
};

export default renderVerificationStatusIcon;
