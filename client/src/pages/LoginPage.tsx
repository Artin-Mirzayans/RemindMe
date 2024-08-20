import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  clearAuthTokens,
  refreshAccessToken,
  validateAccessToken,
} from "../components/Auth/authUtils";
import { useUser } from "../components/Auth/UserContext";

const CLIENT_ID =
  "344473433718-a1vsjaaules6d3i934gmnbjs72nep4va.apps.googleusercontent.com";
const REDIRECT_URI = process.env.OAUTH_REDIRECT_URI;
const authUrl = `https://accounts.google.com/o/oauth2/v2/auth?client_id=${CLIENT_ID}&redirect_uri=${REDIRECT_URI}&response_type=code&scope=email&access_type=offline`;

const LoginPage = () => {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { setUser } = useUser();

  const handleLogin = async () => {
    const accessToken = localStorage.getItem("access_token");
    const refreshToken = localStorage.getItem("refresh_token");

    if (accessToken) {
      console.log(accessToken);
      const { isValid, email } = await validateAccessToken(accessToken);
      if (isValid && email) {
        setUser({ email });
        navigate("/");
      }
    } else if (refreshToken) {
      const newAccessToken = await refreshAccessToken(refreshToken);
      if (newAccessToken) {
        const { isValid, email } = await validateAccessToken(newAccessToken);
        if (isValid && email) {
          setUser({ email });
          localStorage.setItem("access_token", newAccessToken);
          navigate("/");
        } else {
          clearAuthTokens();
          window.location.href = authUrl;
        }
      } else {
        clearAuthTokens();
        window.location.href = authUrl;
      }
    } else {
      console.log("gotta sign in w/ google");
      window.location.href = authUrl;
    }
    setLoading(false);
  };

  if (loading) {
    return <div>Loading...</div>;
  }

  return (
    <div className="login-page">
      <h1>Welcome to RemindMe</h1>
      <button onClick={handleLogin}>Sign in with Google</button>
    </div>
  );
};

export default LoginPage;
