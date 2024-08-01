import React from "react";

const renderVerificationStatusIcon = (verificationStatus) => {
  switch (verificationStatus) {
    case "verified":
      return <span style={{ color: "green" }}>✔️</span>;
    case "notVerified":
      return <span style={{ color: "red" }}>❌</span>;
    case "pending":
      return <span style={{ color: "yellow" }}>⚠️</span>;
    default:
      return null;
  }
};

export default renderVerificationStatusIcon;
