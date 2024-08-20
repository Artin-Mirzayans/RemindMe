import React, { useEffect, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import queryString from "query-string";
import axios from "axios";

const AuthCallback: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);

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
            setError("Tokens not received from the backend.");
          }
        } catch (err) {
          console.error("Error fetching tokens:", err);
          setError("Failed to fetch tokens. Please try again.");
        }
      } else {
        setError("Authorization code not found.");
      }
    };

    fetchTokens();
    effectRan.current = true;
  }, [location]);

  return (
    <div>
      <h1>Authorization Callback</h1>
      {error ? <p>{error}</p> : <p>Processing your authentication...</p>}
    </div>
  );
};

export default AuthCallback;
