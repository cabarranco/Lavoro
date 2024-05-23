import React from 'react';
import styled from 'styled-components';
import quickviewSiteData from '../../data/quickviewSiteData';
import betstoreSiteData from '../../data/betstoreSiteData';

const PageHeaderContainerSC = styled.div`
  display: flex;
  align-items: center;
  /* justify-content: center; */
  width: 100%;
  height: 70px;
  /* outline: 1px solid red; */
`;

const PageTitle = styled.div`
  display: flex;
  align-items: center;
  /* justify-content: center; */
  width: ${props => props.quickview ? "130px" : "auto"};
  height: 20px;

  font-family: 'Montserrat', sans-serif;
  font-size: 18px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  line-height: 1.1;

  /* outline: 1px solid white; */
  margin-left: 70px;
`;

const DateDropdown = styled.div`
  display: flex;
  align-items: center;
  /* justify-content: center; */

  font-family: 'Montserrat', sans-serif;
  font-size: 10px;
  text-transform: uppercase;

  width: 110px;
  height: 20px;
  outline: 1px solid white;
  margin-left: 10px;
`;

const VenueDropdown = styled.div`
  display: flex;
  align-items: center;
  /* justify-content: center; */

  font-family: 'Montserrat', sans-serif;
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;

  width: 110px;
  height: 20px;
  outline: 1px solid white;
  margin-left: 10px;
`;

const PageHeader = (props) => {

  const {
    quickview,
    betstore
  } = props;

  return (
      <PageHeaderContainerSC>
          {quickview ? (
          <>
            <PageTitle>{quickviewSiteData.navigation.quickview}</PageTitle>
            <DateDropdown></DateDropdown> {/* make into dropdown*/}
            <VenueDropdown>{quickviewSiteData.venues[0]}</VenueDropdown> {/* make into dropdown*/}
          </>
          )
          : betstore ? 
          (<PageTitle>{betstoreSiteData.navigation.betstore}</PageTitle>) 
          : null}
        </PageHeaderContainerSC>
  )
};

export default PageHeader;