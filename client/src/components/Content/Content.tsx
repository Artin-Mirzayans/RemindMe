import React from "react";
import { ChildrenProps } from "../../props/ChildrenProps";

import "./Content.css";

const Content: React.FC<ChildrenProps> = ({ children }) => {
  return <div className="content">{children}</div>;
};

export default Content;
