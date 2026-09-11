import React, {
  createContext,
  useContext,
  useState,
  useEffect,
  ReactNode,
} from "react";
import {
  clearAuthTokens,
  refreshAccessToken,
  validateAccessToken,
  fetchUserData,
} from "./authUtils";

export interface User {
  email: string;
  phoneNumber: string;
  isVerified: boolean;
  lastOtpSentTimestamp: number;
}

interface UserContextType {
  user: User | null;
  setUser: React.Dispatch<React.SetStateAction<User | null>>;
  authLoading: boolean;
}

const UserContext = createContext<UserContextType | undefined>(undefined);

export const UserProvider: React.FC<{ children: ReactNode }> = ({
  children,
}) => {
  const [user, setUser] = useState<User | null>(null);
  const [authLoading, setAuthLoading] = useState(true);

  useEffect(() => {
    const applyToken = async (token: string): Promise<boolean> => {
      const { isValid, email } = await validateAccessToken(token);
      if (isValid && email) {
        const userData = await fetchUserData(email);
        setUser(userData);
        return true;
      }
      return false;
    };

    const checkAuth = async () => {
      const accessToken = localStorage.getItem("access_token");
      const refreshToken = localStorage.getItem("refresh_token");

      if (accessToken && (await applyToken(accessToken))) {
        setAuthLoading(false);
        return;
      }

      if (refreshToken) {
        const newAccessToken = await refreshAccessToken(refreshToken);
        if (newAccessToken && (await applyToken(newAccessToken))) {
          localStorage.setItem("access_token", newAccessToken);
          setAuthLoading(false);
          return;
        }
      }

      clearAuthTokens();
      setUser(null);
      setAuthLoading(false);
    };

    checkAuth();
  }, []);

  useEffect(() => {
    const onLogout = () => setUser(null);
    window.addEventListener("auth:logout", onLogout);
    return () => window.removeEventListener("auth:logout", onLogout);
  }, []);

  return (
    <UserContext.Provider value={{ user, setUser, authLoading }}>
      {children}
    </UserContext.Provider>
  );
};

export const useUser = () => {
  const context = useContext(UserContext);
  if (context === undefined) {
    throw new Error("useUser must be used within a UserProvider");
  }
  return context;
};
