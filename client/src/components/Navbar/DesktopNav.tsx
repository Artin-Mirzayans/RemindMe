import React from "react";
import { Link } from "react-router-dom";

import { LuNewspaper, LuBellRing, LuCalendarClock, LuMapPin, LuActivity } from "react-icons/lu";
import { CgProfile } from "react-icons/cg";
import { FaGithub } from "react-icons/fa";
import { MdLogout } from "react-icons/md";

import Logo from "./Logo";
import { useNearbyCity } from "../Nearby/nearbyCity";
import { useUser } from "../Auth/UserContext";

import "./Nav.css";

interface DesktopNavProps {
  handleLogout: () => void;
}

const DesktopNav: React.FC<DesktopNavProps> = ({ handleLogout }) => {
  const { user } = useUser();
  const nearbyCity = useNearbyCity();

  return (
    <div className="nav">
      <div className="nav-info">
        <Logo />
        <Link to="/" className="nav-link">
          <LuNewspaper size={26} />
          <span>Today &amp; Tomorrow</span>
        </Link>
        <Link to="planning-ahead" className="nav-link">
          <LuCalendarClock size={26} />
          <span>Planning Ahead</span>
        </Link>
        <Link to="nearby" className="nav-link">
          <LuMapPin size={26} />
          <span>{nearbyCity}</span>
        </Link>
        <Link to="reminders" className="nav-link">
          <LuBellRing size={26} />
          <span>Reminders</span>
        </Link>
      </div>
      <div className="nav-user">
        <Link to="profile" className="nav-link">
          <CgProfile size={26} />
          <span>Profile</span>
        </Link>
        <Link to="system" className="nav-link">
          <LuActivity size={26} />
          <span>System</span>
        </Link>
        <a
          href="https://github.com/Artin-Mirzayans/RemindMe"
          className="nav-link"
          target="_blank"
          rel="noopener noreferrer"
        >
          <FaGithub size={26} />
        </a>
        {user && (
          <button
            type="button"
            className="btn btn--icon"
            onClick={handleLogout}
            aria-label="Log out"
          >
            <MdLogout size={24} />
          </button>
        )}
      </div>
    </div>
  );
};

export default DesktopNav;
