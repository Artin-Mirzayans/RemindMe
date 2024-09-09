import React, { useEffect, useRef } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import queryString from "query-string";
import axios from "axios";
import Loader from "../Loader/Loader";

const AuthCallback: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();

  const effectRan = useRef(false);

  useEffect(() => {
    if (effectRan.current) return;

    const fetchTokens = async () => {
      const queryParams = queryString.parse(
        location.hash ? location.hash.substring(1) : location.search
      );
      const authorizationCode = queryParams.code as string;

      if (authorizationCode) {
        try {
          const response = await axios.post(
            `${process.env.SERVER_API_URL}/oauth2callback`,
            null,
            {
              params: { code: authorizationCode },
              headers: {
                "Content-Type": "application/x-www-form-urlencoded",
              },
            }
          );

          const responseData = response.data;
          const { refreshToken, accessToken } = responseData;

          if (refreshToken && accessToken) {
            localStorage.setItem("refresh_token", refreshToken);
            localStorage.setItem("access_token", accessToken);
            navigate("/");
          } else {
            console.log("Tokens not received from the backend.");
          }
        } catch (err) {
          console.error("Error fetching tokens:", err);
          console.log("Failed to fetch tokens. Please try again.");
        }
      } else {
        console.log("Authorization code not found.");
      }
    };

    fetchTokens();
    effectRan.current = true;
  }, [location]);

  return (
    <div className="auth-loader">
      <Loader />
    </div>
  );
};

export default AuthCallback;
