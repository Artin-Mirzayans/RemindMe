import React from "react";
import { Link } from "react-router-dom";
import { TbClockOff } from "react-icons/tb";

import "./NotFoundPage.css";

const NotFoundPage = () => {
  return (
    <div className="not-found-page">
      <TbClockOff className="not-found-icon" aria-hidden="true" />
      <h1 className="not-found-title">Nothing scheduled here</h1>
      <p className="not-found-text">
        That page doesn&apos;t exist &mdash; it may have been moved, or the link
        might be wrong.
      </p>
      <Link to="/" className="btn">
        Back home
      </Link>
    </div>
  );
};

export default NotFoundPage;
