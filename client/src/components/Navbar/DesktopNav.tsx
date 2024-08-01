import React from "react";
import { Link } from "react-router-dom";

import { LuHome } from "react-icons/lu";
import { CgProfile } from "react-icons/cg";
import { TbZoomQuestion } from "react-icons/tb";
import { MdLogout } from "react-icons/md";

import Logo from "./Logo";

import "./Nav.css";

const DesktopNav: React.FC = () => {
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
        <div className="nav-name">First Lastname</div>
        <button>
          <span>
            <MdLogout size={42} />
          </span>
        </button>
      </div>
    </div>
  );
};

export default DesktopNav;
