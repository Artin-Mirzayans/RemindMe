import React from 'react';
import logo from '../../assets/logo-no-background.png'

import "./Logo.css"

const Logo = () => {
    return (
        <div className="logo">
            <img src={logo} alt='RemindMe Logo'></img>
        </div>
    );
};

export default Logo;
