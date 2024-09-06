import React, { useEffect, useState } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useUser } from "./UserContext";
import {
  clearAuthTokens,
  refreshAccessToken,
  validateAccessToken,
  fetchUserData,
} from "./authUtils";

interface PrivateRouteProps {
  element: React.FC;
}

const PrivateRoute: React.FC<PrivateRouteProps> = ({ element: Component }) => {
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(true);
  const location = useLocation();
  const { setUser } = useUser();

  useEffect(() => {
    const checkAuth = async () => {
      const accessToken = localStorage.getItem("access_token");
      const refreshToken = localStorage.getItem("refresh_token");

      if (accessToken) {
        const { isValid, email } = await validateAccessToken(accessToken);
        if (isValid && email) {
          const userData = await fetchUserData(email);
          setUser(userData);
          setIsAuthenticated(true);
        } else {
          await handleRefreshToken(refreshToken);
        }
      } else if (refreshToken) {
        await handleRefreshToken(refreshToken);
      } else {
        setIsAuthenticated(false);
      }

      setLoading(false);
    };

    const handleRefreshToken = async (refreshToken: string | null) => {
      if (refreshToken) {
        const newAccessToken = await refreshAccessToken(refreshToken);
        if (newAccessToken) {
          const { isValid, email } = await validateAccessToken(newAccessToken);
          if (isValid && email) {
            const userData = await fetchUserData(email);
            setUser(userData);
            localStorage.setItem("access_token", newAccessToken);
            setIsAuthenticated(true);
          } else {
            clearAuthTokens();
            setIsAuthenticated(false);
          }
        } else {
          clearAuthTokens();
          setIsAuthenticated(false);
        }
      } else {
        clearAuthTokens();
        setIsAuthenticated(false);
      }
    };

    checkAuth();
  }, [setUser]);

  if (loading) {
    return <div>Loading...</div>; // Or some loading spinner
  }

  return isAuthenticated ? (
    <Component />
  ) : (
    <Navigate to="/login" state={{ from: location }} />
  );
};

export default PrivateRoute;
