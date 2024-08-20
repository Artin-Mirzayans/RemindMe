import React, { useContext, useState } from "react";
import { useNavigate } from "react-router-dom";
import PageSizeContext from "../PageSizeContext";
import useElementHeight from "./useElementHeight";
import MobileNav from "./MobileNav";
import DesktopNav from "./DesktopNav";
import { clearAuthTokens } from "../Auth/authUtils";

const Nav: React.FC = () => {
  const { width } = useContext(PageSizeContext);
  const [navRef, navHeight] = useElementHeight();
  const isMobile = width <= 1070;
  const [isOpen, setIsOpen] = useState(false);
  const navigate = useNavigate();

  const toggleMenu = () => {
    setIsOpen(!isOpen);
    document.querySelector(".content")?.classList.toggle("dimmed", !isOpen);
  };

  const handleLogout = () => {
    clearAuthTokens();
    navigate("/login");
  };

  return (
    // @ts-expect-error TS2761: Neither 'LegacyRef' nor 'RefObject' is assignable to type
    <div ref={navRef}>
      {isMobile ? (
        <MobileNav
          isOpen={isOpen}
          toggleMenu={toggleMenu}
          navHeight={navHeight as number}
          handleLogout={handleLogout}
        />
      ) : (
        <DesktopNav handleLogout={handleLogout} />
      )}
    </div>
  );
};

export default Nav;
