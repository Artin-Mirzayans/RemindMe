import React from "react";
import { Outlet } from "react-router-dom";
import Nav from "../components/Navbar/Nav";
import Content from "../components/Content/Content";

import "./MainPage.css";

const MainPage = () => {
  return (
    <div className="main-page">
      <Nav />
      <Content>
        <Outlet />
      </Content>
    </div>
  );
};

export default MainPage;
