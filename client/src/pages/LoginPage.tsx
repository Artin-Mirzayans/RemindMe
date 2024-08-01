import React from "react";
import { Link } from "react-router-dom";

const LoginPage = () => {
  return (
    <div className="login-page">
      <h1>Welcome to RemindMe</h1>
      <Link to={"/"}>Login</Link>
    </div>
  );
};

export default LoginPage;
