import React from 'react';
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';

import ReminderContent from './Reminder/ReminderContent';
import ProfileContent from './Profile/ProfileContent';
import FAQContent from './FAQ/FAQContent';
import LoginPage from '../pages/LoginPage';
import MainPage from '../pages/MainPage';

const AppRouter = () => {
    return (
        <Router>
            <Routes>
                <Route path="/login" element={<LoginPage />} />
                <Route>
                    <Route path="/" element={<MainPage />}>
                        <Route path="/" element={<ReminderContent />} />
                        <Route path="profile" element={<ProfileContent />} />
                        <Route path="faq" element={<FAQContent />} />
                    </Route>
                </Route>
            </Routes>
        </Router>
    );
};

export default AppRouter;
