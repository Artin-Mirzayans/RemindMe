import React, { useContext } from "react";
import PageSizeContext from "../PageSizeContext";
import { ClockLoader } from "react-spinners";

import "./Loader.css";

const Loader = () => {
  const { width } = useContext(PageSizeContext);

  let size: number;
  if (width > 1200) {
    size = 175;
  } else if (width > 768) {
    size = 140;
  } else {
    size = 100;
  }

  return <ClockLoader size={size} color="#955d1c" />;
};

export default Loader;
