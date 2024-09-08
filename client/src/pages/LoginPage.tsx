import React, { useContext, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useUser } from "../components/Auth/UserContext";
import { handleLogin } from "../components/Auth/handleLogin";
import PageSizeContext from "../components/PageSizeContext";
import logo from "../assets/logo-no-background.png";
import mobileWorry from "../assets/worry-less-mobile.jpg";
import worry from "../assets/worry-less-large.jpg";

import "./LoginPage.css";

const LoginPage = () => {
  const [currentBackground, setCurrentBackground] = useState<string>(worry);
  const navigate = useNavigate();
  const { width } = useContext(PageSizeContext);
  const { setUser } = useUser();

  const onLoginClick = () => {
    handleLogin(setUser, navigate);
  };

  useEffect(() => {
    if (width < 600) {
      setCurrentBackground(mobileWorry);
    } else {
      setCurrentBackground(worry);
    }
  }, [width]);

  const style = {
    backgroundImage: `linear-gradient(rgba(255, 255, 255, 0.2), rgba(255, 255, 255, 0.5)), url(${currentBackground})`,
    backgroundSize: "cover",
    backgroundPosition: "center",
    backgroundRepeat: "no-repeat",
  };

  return (
    <div className="login-page" style={style}>
      <div className="login-page-content">
        <img className="login-page-logo" src={logo}></img>
        <div className="login-page-slogan">
          &quot;Simply Your Day, One Reminder Away &quot;
        </div>
        <div className="login-page-signin" onClick={onLoginClick}>
          <img
            src="https://developers.google.com/identity/images/g-logo.png"
            alt="Google logo"
            className="google-logo"
          />
          <div>Sign in with Google</div>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
