import React from 'react';
import styled from 'styled-components';

const DataContainer = styled.div`
  ${props => props.span2cols ? "grid-column: 2 / span 2" : null};
  height: ${props => props.isGraph ? "350px" : "200px"};
  outline: 1px solid red;
  border-radius: 5px;
  margin: 10px;
  background: #252524;
`;

const ChartsHeader = styled.div`
  display: flex;
  align-items: center;
  /* justify-content: center; */
  width: 100%;
  height: 40px;
  background: #191919;
  outline: 1px solid green;
`;

const DataContainerTitle = styled.div`
  display: inline-block;
  height: 20px;

  font-family: 'Montserrat', sans-serif;
  font-size: 14px;
  font-weight: 600;
  text-transform: uppercase;

  /* outline: 1px solid white; */
  margin-left: 10px;
`;

const Data = (props) => {
    const {
        title,
        span2cols,
        isGraph,
    } = props;

    return (
        <DataContainer span2cols={span2cols} isGraph={isGraph}>
            <ChartsHeader>
                <DataContainerTitle>{title}</DataContainerTitle>
            </ChartsHeader>
        </DataContainer>
    )
}

export default Data;