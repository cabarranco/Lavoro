import React, { useState } from 'react';
import styled from 'styled-components';
// import { BrowserRouter as Router, Switch, Route, Link } from "react-router-dom";
import Quickview from './components/body/quickview/Quickview';
import Betstore from './components/body/betstore/Betstore';
import quickviewSiteData from './data/quickviewSiteData';
import logo from './logo/asb_logo.png';

const NavMenuContainerSC = styled.div`
  display: inline-block;
  width: 100%;
  min-height: 100vh;
  background: #191919;
`;

const LogoContainerSC = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 90px;

  img {
    width: 120px;
  }
`;

const NavigationSC = styled.div`
  width: 100%;
  height: auto;
`;

const NavigationLinkSC = styled.div`
  display: flex;
  align-items: center;
  /* justify-content: center; */
  width: 100%;
  opacity: ${props => props.isActivePage ? "1" :"0.5"};

  div {
    width: 120px;
    margin: 0 auto;

    p {
      font-size: 14px;
      text-transform: uppercase;
      letter-spacing: 0.1em;
      line-height: 1.1;
      font-weight: 500;

      &:hover {
        cursor: pointer;
      }
    }
  }
`;

const NavMenu = (props) => {
  const {
    changePage,
    activePage
  } = props;

  return (
  <>
    <NavMenuContainerSC>
      <LogoContainerSC>
        <img src={logo} alt="ASB Logo" />
      </LogoContainerSC>
      <NavigationSC>
        {/* <Link to="/"> */}
          <NavigationLinkSC onClick={() => changePage("quickview")} isActivePage={activePage === "quickview"}>
            <div>
              <p>{quickviewSiteData.navigation.quickview}</p>
            </div>
          </NavigationLinkSC>
        {/* </Link> */}
        {/* <Link to="/betstore"> */}
          <NavigationLinkSC onClick={() => changePage("betstore")} isActivePage={ activePage === "betstore"}>
            <div>
              <p>{quickviewSiteData.navigation.betstore}</p>
            </div>
          </NavigationLinkSC>
        {/* </Link> */}
        {/* <Link to="/logout"> */}
          <NavigationLinkSC onClick={() => changePage("log out")} isActivePage={activePage === "log out"}>
            <div>
              <p>{quickviewSiteData.navigation.logOut}</p>
            </div>
          </NavigationLinkSC>
        {/* </Link> */}
      </NavigationSC>
    </NavMenuContainerSC>
  </>
  );
};

const AppContainer = styled.div`
  display: grid;
  grid-template-columns: 15% 85%;
`;

function App() {
  const [currentPage, setCurrentPage] = useState("betstore") // Change for initial page

  const changePage = (page) => {
    setCurrentPage(page);
  }
  return (
    <>
        <AppContainer>
          {/* <Router> */}
            <NavMenu changePage={changePage} activePage={currentPage}/> 
            {
              currentPage === "homepage" ? <h1>HOMEPAGE</h1> 
              : currentPage === "quickview" ? <Quickview />
              : currentPage === "betstore" ? <Betstore />
              : <h1>404 PAGE NOT FOUND</h1>
            }
            {/* <Switch>
              <Route exact path="/"><Quickview /></Route>
              <Route path="/betstore"><Betstore /></Route>
              <Route path="/logout"><h1>404 PAGE NOT FOUND</h1></Route>
            </Switch> */}
          {/* </Router> */}
        </AppContainer>
    </>
  );
}

export default App;
