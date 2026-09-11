import React from "react";
import { BrowserRouter as Router, Route, Routes } from "react-router-dom";

import AuthCallback from "./Auth/AuthCallback";
import ReminderContent from "./Reminder/ReminderContent";
import ProfileContent from "./Profile/ProfileContent";
import DigestContent from "./Digest/DigestContent";
import WatchlistContent from "./Watchlist/WatchlistContent";
import NearbyContent from "./Nearby/NearbyContent";
import MainPage from "../pages/MainPage";
import NotFoundPage from "../pages/NotFoundPage";
import { UserProvider } from "./Auth/UserContext";

const AppRouter = () => {
  return (
    <UserProvider>
      <Router>
        <Routes>
          <Route path="/oauth2/callback" element={<AuthCallback />} />
          <Route path="/" element={<MainPage />}>
            <Route index element={<DigestContent />} />
            <Route path="planning-ahead" element={<WatchlistContent />} />
            <Route path="nearby" element={<NearbyContent />} />
            <Route path="reminders" element={<ReminderContent />} />
            <Route path="profile" element={<ProfileContent />} />
          </Route>
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </Router>
    </UserProvider>
  );
};

export default AppRouter;
