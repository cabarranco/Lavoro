import React from 'react';
import styled from 'styled-components';
import Data from './Data';
// import quickviewSiteData from '../../../data/quickviewSiteData';

const StatsContainer = styled.div`
  display: grid;
  grid-template-columns: 33.4% 33.3% 33.3%;
  padding: 0px 60px 20px 60px;
`;

export const Stats = () => {
  // map over quickviewSiteData.portfolio to render Data components with correct titles, ensuring specific titles also include extra attributes (span2cols and isGraph)
  return (
      <StatsContainer>
        <Data title="My portfolio"/>
        <Data title="Bets" span2cols/>
        <Data title="My accounts" />
        <Data title="Qarbs" span2cols/>
        <Data title="Trends" isGraph/>
        <Data title="Trends" isGraph/>
        <Data title="Trends" isGraph/>
      </StatsContainer>
  )
}

export default Stats;