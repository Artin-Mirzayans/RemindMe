import React from "react";
import { Link } from "react-router-dom";

import Hamburger from "hamburger-react";
import { LuHome } from "react-icons/lu";
import { CgProfile } from "react-icons/cg";
import { TbZoomQuestion } from "react-icons/tb";
import { MdLogout } from "react-icons/md";

import Logo from "./Logo";

import "./Nav.css";

interface MobileNavProps {
  isOpen: boolean;
  toggleMenu: () => void;
  navHeight: number;
}

const MobileNav: React.FC<MobileNavProps> = ({
  isOpen,
  toggleMenu,
  navHeight,
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
          <Link to="faq" className="mobile-nav-link" onClick={toggleMenu}>
            <span className="mobile-nav-desc">FAQ</span>
            <span className="mobile-nav-icon">
              <TbZoomQuestion size={47} />
            </span>
          </Link>
          <Link to="login" className="mobile-nav-link" onClick={toggleMenu}>
            <span className="mobile-nav-desc">Logout</span>
            <span className="mobile-nav-icon">
              <MdLogout size={42} />
            </span>
          </Link>
        </div>
      </div>
    </div>
  );
};

export default MobileNav;
