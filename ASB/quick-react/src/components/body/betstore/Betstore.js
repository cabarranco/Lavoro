import React from 'react';
import styled from 'styled-components';
import PageHeader from '../../header/PageHeader';
import Stats from './Stats';

const BetstoreBody = styled.div`
  display: inline-block;
  width: 100%;
`;

export const Betstore = () => {
    return (
        <>
            <BetstoreBody>
                <PageHeader betstore /> {/* Needs work to render conditionally dependant on what page you are on using props */}
                <Stats />
            </BetstoreBody>
        </>
    );
};

export default Betstore;