import React from 'react';
import styled from 'styled-components';
import PageHeader from '../../header/PageHeader';
import Stats from './Stats';

const QuickviewBody = styled.div`
  display: inline-block;
  width: 100%;
`;

export const Quickview = () => {
    return (
        <>
          <QuickviewBody>
            <PageHeader quickview />
            <Stats />
          </QuickviewBody>
        </>
    )
}

export default Quickview;