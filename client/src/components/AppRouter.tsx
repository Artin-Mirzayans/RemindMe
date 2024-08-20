import React from "react";
import { BrowserRouter as Router, Route, Routes } from "react-router-dom";

import PrivateRoute from "./Auth/PrivateRoute";
import AuthCallback from "./Auth/AuthCallback";
import ReminderContent from "./Reminder/ReminderContent";
import ProfileContent from "./Profile/ProfileContent";
import FAQContent from "./FAQ/FAQContent";
import LoginPage from "../pages/LoginPage";
import MainPage from "../pages/MainPage";
import { UserProvider } from "./Auth/UserContext";

const AppRouter = () => {
  return (
    <UserProvider>
      <Router>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/oauth2/callback" element={<AuthCallback />} />
          <Route path="/" element={<PrivateRoute element={MainPage} />}>
            <Route index element={<ReminderContent />} />
            <Route path="profile" element={<ProfileContent />} />
            <Route path="faq" element={<FAQContent />} />
          </Route>
          <Route path="*" element={<h1>CATCH ALL</h1>} />
        </Routes>
      </Router>
    </UserProvider>
  );
};

export default AppRouter;
