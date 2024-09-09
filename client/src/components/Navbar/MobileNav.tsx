import React from "react";
import { Link } from "react-router-dom";

import Hamburger from "hamburger-react";
import { LuHome } from "react-icons/lu";
import { CgProfile } from "react-icons/cg";
import { FaGithub } from "react-icons/fa6";
import { MdLogout } from "react-icons/md";

import Logo from "./Logo";

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
            <span className="mobile-nav-desc">Home</span>
            <span className="mobile-nav-icon">
              <LuHome size={45} />
            </span>
          </Link>
          <Link to="profile" className="mobile-nav-link" onClick={toggleMenu}>
            <span className="mobile-nav-desc">Profile</span>
            <span className="mobile-nav-icon">
              <CgProfile size={45} />
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
              <FaGithub size={47} />
            </span>
          </a>
          <a className="mobile-nav-link" onClick={handleLogout} href="">
            <span className="mobile-nav-desc">Logout</span>
            <span className="mobile-nav-icon">
              <MdLogout size={42} />
            </span>
          </a>
        </div>
      </div>
    </div>
  );
};

export default MobileNav;
