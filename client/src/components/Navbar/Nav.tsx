import React, { useContext, useState } from "react";
import PageSizeContext from "../PageSizeContext";
import useElementHeight from "./useElementHeight";
import MobileNav from "./MobileNav";
import DesktopNav from "./DesktopNav";

const Nav: React.FC = () => {
  const { width } = useContext(PageSizeContext);
  const [navRef, navHeight] = useElementHeight();
  const isMobile = width <= 1070;
  const [isOpen, setIsOpen] = useState(false);

  const toggleMenu = () => {
    setIsOpen(!isOpen);
    document.querySelector(".content")?.classList.toggle("dimmed", !isOpen);
  };

  return (
    // @ts-expect-error TS2761: Neither 'LegacyRef' nor 'RefObject' is assignable to type
    <div ref={navRef}>
      {isMobile ? (
        <MobileNav
          isOpen={isOpen}
          toggleMenu={toggleMenu}
          navHeight={navHeight as number}
        />
      ) : (
        <DesktopNav />
      )}
    </div>
  );
};

export default Nav;
