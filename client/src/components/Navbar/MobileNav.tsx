import React from "react";
import { Link } from "react-router-dom";

import Hamburger from "hamburger-react";
import { LuNewspaper, LuBellRing, LuCalendarClock, LuMapPin, LuActivity } from "react-icons/lu";
import { CgProfile } from "react-icons/cg";
import { FaGithub } from "react-icons/fa6";
import { MdLogout } from "react-icons/md";

import Logo from "./Logo";
import { useNearbyCity } from "../Nearby/nearbyCity";
import { useUser } from "../Auth/UserContext";

import "./Nav.css";

interface MobileNavProps {
  isOpen: boolean;
  toggleMenu: () => void;
  navHeight: number;
  handleLogout: () => void;
}

const MobileNav: React.FC<MobileNavProps> = ({
  isOpen,
  toggleMenu,
  navHeight,
  handleLogout,
}) => {
  const { user } = useUser();
  const nearbyCity = useNearbyCity();

  return (
    <div className="mobile-nav">
      <Hamburger size={42} toggled={isOpen} toggle={toggleMenu} />
      <Logo />
      <div
        className={`sliding-menu ${isOpen ? "open" : ""}`}
        style={{ top: `${navHeight}px` }}
      >
        <div className="sliding-menu-content">
          <Link to="/" className="mobile-nav-link" onClick={toggleMenu}>
            <span className="mobile-nav-desc">Today &amp; Tomorrow</span>
            <span className="mobile-nav-icon">
              <LuNewspaper size={32} />
            </span>
          </Link>
          <Link to="planning-ahead" className="mobile-nav-link" onClick={toggleMenu}>
            <span className="mobile-nav-desc">Planning Ahead</span>
            <span className="mobile-nav-icon">
              <LuCalendarClock size={32} />
            </span>
          </Link>
          <Link to="nearby" className="mobile-nav-link" onClick={toggleMenu}>
            <span className="mobile-nav-desc">{nearbyCity}</span>
            <span className="mobile-nav-icon">
              <LuMapPin size={32} />
            </span>
          </Link>
          <Link to="reminders" className="mobile-nav-link" onClick={toggleMenu}>
            <span className="mobile-nav-desc">Reminders</span>
            <span className="mobile-nav-icon">
              <LuBellRing size={32} />
            </span>
          </Link>
          <Link to="profile" className="mobile-nav-link" onClick={toggleMenu}>
            <span className="mobile-nav-desc">Profile</span>
            <span className="mobile-nav-icon">
              <CgProfile size={32} />
            </span>
          </Link>
          <Link to="system" className="mobile-nav-link" onClick={toggleMenu}>
            <span className="mobile-nav-desc">System</span>
            <span className="mobile-nav-icon">
              <LuActivity size={32} />
            </span>
          </Link>
          <a
            href="https://github.com/Artin-Mirzayans/RemindMe"
            className="mobile-nav-link"
            target="_blank"
            rel="noopener noreferrer"
            onClick={toggleMenu} // Close the menu when the link is clicked
          >
            <span className="mobile-nav-desc">GitHub</span>
            <span className="mobile-nav-icon">
              <FaGithub size={32} />
            </span>
          </a>
          {user && (
            <button
              type="button"
              className="mobile-nav-link"
              onClick={handleLogout}
            >
              <span className="mobile-nav-desc">Logout</span>
              <span className="mobile-nav-icon">
                <MdLogout size={32} />
              </span>
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

export default MobileNav;
