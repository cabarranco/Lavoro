import React from 'react';
import styled from 'styled-components';
import Data from "./Data";

const StatsContainer = styled.div`
  display: grid;
  grid-template-columns: 52% 48%;
  padding: 0px 60px 20px 60px;
`;

export const Stats = () => {
    return (
        <>
            <StatsContainer>
                <Data title="Events" eventName="Event name" span2rows allowOverflow eventDate competition has4cols hasDropdown isEvents/>
                <Data title="Scheduled events - Tick frequency" eventName="Event name" eventDate competition has4cols isScheduledEvents/>
                <Data title="Stored events analytics - Tick frequency" eventName="2020 breakdown by competition / quarter" isGraph />
            </StatsContainer>
        </>
    );
};

export default Stats;