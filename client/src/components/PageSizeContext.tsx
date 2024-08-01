import React, { createContext, useState, useEffect } from "react";
import { ChildrenProps } from "../props/ChildrenProps";

interface PageSizeContextType {
  width: number;
  height: number;
}

const defaultValue: PageSizeContextType = {
  width: window.innerWidth,
  height: window.innerHeight,
};

const PageSizeContext = createContext<PageSizeContextType>(defaultValue);

export const PageSizeProvider: React.FC<ChildrenProps> = ({ children }) => {
  const [pageSize, setPageSize] = useState({
    width: window.innerWidth,
    height: window.innerHeight,
  });

  useEffect(() => {
    const handleResize = () => {
      setPageSize({
        width: window.innerWidth,
        height: window.innerHeight,
      });
    };

    window.addEventListener("resize", handleResize);

    return () => {
      window.removeEventListener("resize", handleResize);
    };
  }, []);

  return (
    <PageSizeContext.Provider value={pageSize}>
      {children}
    </PageSizeContext.Provider>
  );
};

export default PageSizeContext;
