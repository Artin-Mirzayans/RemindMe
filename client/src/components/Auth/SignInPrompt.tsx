import React from "react";
import GoogleSignInButton from "./GoogleSignInButton";

import "./SignInPrompt.css";

interface SignInPromptProps {
  title: string;
  description: string;
  compact?: boolean;
  children?: React.ReactNode;
}

const SignInPrompt: React.FC<SignInPromptProps> = ({
  title,
  description,
  compact = false,
  children,
}) => {
  if (compact) {
    return (
      <div className="sign-in-prompt sign-in-prompt--compact">
        <div className="sign-in-prompt-text">
          <strong>{title}</strong> {description}
        </div>
        <GoogleSignInButton />
      </div>
    );
  }

  return (
    <div className="sign-in-prompt">
      <h3 className="sign-in-prompt-title">{title}</h3>
      <p className="sign-in-prompt-description">{description}</p>
      <GoogleSignInButton />
      {children && <div className="sign-in-prompt-preview">{children}</div>}
    </div>
  );
};

export default SignInPrompt;
