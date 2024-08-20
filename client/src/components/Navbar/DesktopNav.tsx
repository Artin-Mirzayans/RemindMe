import React from "react";
import { Link } from "react-router-dom";

import { LuHome } from "react-icons/lu";
import { CgProfile } from "react-icons/cg";
import { TbZoomQuestion } from "react-icons/tb";
import { FaGithub } from "react-icons/fa";
import { MdLogout } from "react-icons/md";

import Logo from "./Logo";

import "./Nav.css";

interface DesktopNavProps {
  handleLogout: () => void;
}

const DesktopNav: React.FC<DesktopNavProps> = ({ handleLogout }) => {
  return (
    <div className="nav">
      <div className="nav-info">
        <Logo />
        <Link to="/" className="nav-link">
          <LuHome size={45} />
        </Link>
        <Link to="profile" className="nav-link">
          <CgProfile size={45} />
        </Link>
        <Link to="faq" className="nav-link">
          <TbZoomQuestion size={47} />
        </Link>
      </div>
      <div className="nav-user">
        <a
          href="https://github.com/Artin-Mirzayans/RemindMe"
          className="nav-link"
          target="_blank"
          rel="noopener noreferrer"
        >
          <FaGithub size={47} />
        </a>
        <button onClick={handleLogout}>
          <span>
            <MdLogout size={42} />
          </span>
        </button>
      </div>
    </div>
  );
};

export default DesktopNav;
