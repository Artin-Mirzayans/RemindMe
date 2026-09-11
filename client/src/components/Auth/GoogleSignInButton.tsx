import React from "react";
import { useNavigate } from "react-router-dom";
import { useUser } from "./UserContext";
import { handleLogin } from "./handleLogin";

import "./GoogleSignInButton.css";

const GoogleSignInButton: React.FC = () => {
  const navigate = useNavigate();
  const { setUser } = useUser();

  return (
    <button
      type="button"
      className="google-signin-btn"
      onClick={() => handleLogin(setUser, navigate)}
    >
      <img
        src="https://developers.google.com/identity/images/g-logo.png"
        alt=""
        className="google-logo"
      />
      Sign in with Google
    </button>
  );
};

export default GoogleSignInButton;
