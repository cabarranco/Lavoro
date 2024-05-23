import React, { useEffect, useState, useRef } from 'react';
import styled from 'styled-components';

// async function graphData() {
// 	let response = await fetch("https://betstore.asbresearch.com/api/barchart", {mode: 'cors'});
// 	let data = await response.json();
// 	return data;
// };
// // graphData().then(data => console.log("ANDY DATA",data));
// console.log("ANDY graphData - ", graphData());

// let results = fetch("https://betstore.asbresearch.com/api/barchart")
//         .then((res) => {
//             return res.json();
//         })
//         .then((data) => {
//             return data;
// 		})
// 		console.log("ANDY results - ", results);

// const mockGraphData = {
// 	"storedEventsAvg": [43],
// 	"storedEventsLeagueMax": [65],
// 	"storedEventsLeagueSplit": {
// 		"Serie A": [13,19,26,7],
// 		"Primera Liga": [6,17,35,null],
// 		"Bundesliga": [3,6,26,23],
// 		"Ligue 1": [25,13,2,2],
// 		"Premier League": [19,8,4,8],
// 		"Eredivisie": [5,8,10,2],
// 		"Liga Portuguesa": [3,4,5,3],
// 	}
// }

let amountOfColumns = 0;
let amountOfColumnsCalc = () => {
	// console.log(100 / amountOfColumns);
	let x = `${100 / amountOfColumns}% `
	return x.repeat(amountOfColumns);
}


const SingleColumnContainer = styled.div`
display: grid;
grid-template-rows: calc(100% - 50px) 50px;
align-items: flex-end;
justify-content: center;
width: 100%;
height: 100%;
text-align: center;

> div:first-child {
	/* border-bottom: 1px solid white;
	padding: 0 100px; */
	&:after {
		content: ""; /* This is necessary for the pseudo element to work. */ 
		position: relative;
		/* top: 10px; */
		left: -25px;
		display: block; /* This will put the pseudo element on its own line. */
		margin: 0 auto; /* This will center the border. */
		width: 220%; /* Change this to whatever width you want. */
		border-bottom: 1px solid white; /* This creates the border. Replace black with whatever color you want. */
	}
}
`;

const LeagueName = styled.div`
display: flex;
justify-content: center;
align-items: center;
position: relative;
font-size: 8px;
height: 50px;
> div {
	width: 50px;
}
@media only screen and (max-width: 1100px) {
	font-size: 8px;
}
@media only screen and (max-width: 1000px) {
	font-size: 7px;
}
`;

const Quarter = styled.div`
	&& {
		position: relative;
		width: 45px;
		height: ${props => props.heightValue};
		background: ${props => props.i === 0 ? "#91FBB7" : props.i === 1 ? "#3DC66E" : props.i === 2 ? "#217C42" : props.i === 3 ? "#004318" : null};
		margin-left: 2px;
		opacity: 0.9;
		/* box-shadow: 0px 1px 3px rgba(0, 0, 0, 0.05) inset, 0px 0px 8px rgba(145, 251, 183, 0.6); */
		@media only screen and (max-width: 1100px) {
			width: 35px;
			margin: 0 auto;
		}
	}
	/* Just want shadows on the left and right. use this shadow color -> rgba(61, 198, 110, 0.6) */
`;

const ConstructSingleColumn = ({data, storedEventsLeagueSplit}) => {
	// console.log("DATA", data);
	// console.log("Object.keys(data)", Object.keys?.(storedEventsLeagueSplit));

	let column = Object.keys(storedEventsLeagueSplit).map((name, i) => {
		amountOfColumns = i + 1;
		// need to create an array here which will map over storedEventsLeagueSplit keys, take value and add it all up, then arrange the array in order from largest value to lowest value. Then use the new array in place of 'data.storedEventsLeagueSplit' on line 111
		const Quarters = () => {
			let constructQuarters = data.storedEventsLeagueSplit[name].map((value, i) => {
				let heightValue = `${(350 /  100) * ((value / data.storedEventsLeagueMax) * 100)}px`; 
				// (containerHeight / 100 ( = 1%)) * ((value / maxValue) * 100 ( = value% of maxValue)) = percentage of height 
				//e.g (370 / 100 = 3.7 = 1%) * ((13 / 65) = 0.2 * 100 = 20% of container) | 20 * 3.7 = 74 | 74px = 20% 
				return (
					<div>
						<Quarter key={i} heightValue={heightValue} i={i}/>
					</div>
				)
			})
			return constructQuarters;
		}
		return (
			<>
				<SingleColumnContainer>
					<div>
						<Quarters key={i}/>
					</div>
					<LeagueName><div>{name}</div></LeagueName>
				</SingleColumnContainer>
			</>
		)
	})
	
	return (
		<>
			{column}
		</>
	)
}

const SingleColumn = () => {
	const [data, setData] = useState('');
	const [storedEventsLeagueSplit, setStoredEventsLeagueSplit] = useState('');
    useEffect(() => {
        async function graphData() {
			let response = await fetch("https://betstore.asbresearch.com/api/barchart", {mode: 'cors'});
			let data = await response.json();
			setData(data)
			return data;
		};
		graphData().then(data => setStoredEventsLeagueSplit(data.storedEventsLeagueSplit));
	}, []);

	return (
		<>
			<ConstructSingleColumn data={data} storedEventsLeagueSplit={storedEventsLeagueSplit}/>
		</>
	)
}

const ColumnContainer = styled.div`
	position: relative;
    top: 10px;
	display: flex;
	/* display: grid; */
	grid-template-columns: ${amountOfColumnsCalc};
	/* grid-gap: 10px; */
	/* width: 100%; might need to do condition here where if it is above a certain height and storedEventsLeagueSplit.length is a certain value or below then 'width: 100%' can be used else 'width: min-content' should be used */
	width: min-content;
	height: 100%;
	overflow-x: scroll;
`;

const GraphYTextContainer = styled.div`
	position: relative;
	width: 35px;
	display: grid;
	align-items: center;
	justify-content: center;
	font-size: 10px;
	/* top: 10px; THIS WORKED WITH OLD API - Monitor */
	/* height: calc(100% - 43px); THIS WORKED WITH OLD API - Monitor */
	top: 5px;
	height: calc(100% - 32px);
	${props => props.amountOfLines <= 5 ? `
	top: -5px;
	height: 100%;
	` : null}
	${props => props.amountOfLines === 6 ? `
	top: 1px;
	height: calc(100% - 12px);
	` : null}
	${props => props.amountOfLines === 7 ? `
	top: 5px;
	height: calc(100% - 20px);
	` : null}
	${props => props.amountOfLines === 8 ? `
	top: 9px;
	height: calc(100% - 27px);
	` : null}
	${props => props.amountOfLines === 9 ? `
	top: 11px;
	height: calc(100% - 31px);
	` : null}
	${props => props.amountOfLines === 10 ? `
	top: 13px;
	height: calc(100% - 35px);
	` : null}
	${props => props.amountOfLines === 11 ? `
	top: 15px;
	height: calc(100% - 39px);
	` : null}
	${props => props.amountOfLines === 12 ? `
	top: 16px;
	height: calc(100% - 41px);
	` : null}
	${props => props.amountOfLines === 13 ? `
	top: 17px;
	height: calc(100% - 43px);
	` : null}
	${props => props.amountOfLines === 14 ? `
	top: 18px;
	height: calc(100% - 45px);
	` : null}
	${props => props.amountOfLines === 15 ? `
	top: 19px;
	height: calc(100% - 47px);
	` : null}
	${props => props.amountOfLines === 16 ? `
	top: 20px;
	height: calc(100% - 48px);
	` : null}
	${props => props.amountOfLines === 17 ? `
	top: 20px;
	height: calc(100% - 49px);
	` : null}
	${props => props.amountOfLines === 18 ? `
	top: 21px;
	height: calc(100% - 51px);
	` : null}
	${props => props.amountOfLines === 19 ? `
	top: 21px;
	height: calc(100% - 51px);
	` : null}
	${props => props.amountOfLines >= 20 ? `
	top: 22px;
	height: calc(100% - 53px);
	` : null}
`;
const GraphYText = (data) => {
	let maxValue = data.data.storedEventsLeagueMax;
	let amountOfDivisions = maxValue / 5;
	for (amountOfDivisions; amountOfDivisions > 0; amountOfDivisions--) {
		maxValue =- 5;
	}

	return <></>
}

const GridLinesX = styled.div`
	&:after {
		/* content: "";
		position: relative;
		top: -7px;
		left: 17px;
		display: block;
		margin: 0 auto;
		width: ${true ? null : "300px"}; /* must render conditionally
		border-bottom: 1px solid #979797; */
		content: "";
		position: absolute;
		left: 105%;
		display: block;
		margin: -8px auto;
		border-bottom: 1px solid #979797;
		width: 2000px;
}
	}
`;

const GridXLinesAmount = (data) => {

	let amountOfLines = (data.data.storedEventsLeagueMax / 5);

	if (amountOfLines <= 5) {
		let lines = [];
		let i = data.data.storedEventsLeagueMax;
		for (i; i >= 0; i--) {
			lines.push(<GridLinesX key={i}>{i}</GridLinesX>)
		}
		return lines.map(x => {
			return x;
		});
	}
	if (amountOfLines <= 20) {
		let lines = [];
		let i = amountOfLines;
		for (i; i >= 0; i--) {
			lines.push(<GridLinesX key={i}>{i*5}</GridLinesX>)
		}
		return lines.map(x => {
			return x;
		});
	}
	// if (amountOfLines > 20) {
	// 	console.log("create logic if x > 20 ");
	// 	return <GridLinesX>0</GridLinesX>
	// }
	return null;
}

const KeyContainer = styled.div`
	position: absolute;
	top: 10px;
	right: 10px;
	width: 85px;
	height: 145px;
	/* opacity: 0.9; */
	background-color: rgba(00, 00, 00, 0.7);
	
	div {
	display: grid;
	grid-template-columns: 50% 50%;
	align-items: center;
	width: 100%;
	height: 100%;

		div {
			width: 20px;
			height: 20px;
		}
	}
`;
const KeyColour = styled.div`
	&&&{
		width: 14px;
		height: 14px;
		background: ${props => props.backgroundColour ? props.backgroundColour : "#000000"};
		margin: 0 auto;
	}
`;
const KeyQuarter = styled.div`
	font-size: 14px;
	line-height: 20px;
	letter-spacing: 1.4px;
`
const Key = () => {
	return (
		<>
			<KeyContainer>
				<div>
					<KeyColour backgroundColour="#69FFB1"></KeyColour>
					<KeyQuarter>Q1</KeyQuarter>
					<KeyColour backgroundColour="#3DC66E"></KeyColour>
					<KeyQuarter>Q2</KeyQuarter>
					<KeyColour backgroundColour="#007F39"></KeyColour>
					<KeyQuarter>Q3</KeyQuarter>
					<KeyColour backgroundColour="#004511"></KeyColour>
					<KeyQuarter>Q4</KeyQuarter>
				</div>
			</KeyContainer>
		</>
	)
}


const AverageLineSC = styled.div`
	position: absolute;
	bottom: ${props => `${props.averageLine}px`};
	left: 6%;
    width: calc(100% - 6%); /* I know this can be 94%... Just doing it like this as a reminder ;]*/
	/* width: 100%; */
	height: 2px;
	background-color: white;
`;
const AverageScoreSC = styled.div`
	position: absolute;
	bottom: ${props => `${props.averageLine + 2}px`};
	right: 0px;
	width: 125px;
	height: 50px;
	background-color: rgba(00,00,00,0.5);

	/* Text */
	display: flex;
    align-items: center;
    justify-content: center;
    text-align: center;
    text-transform: uppercase;
    font-size: 9px;
    font-weight: 600;
	letter-spacing: 0.9px;
`;
const AverageLine = (data) => {

	let averageLine = ((350 / data.data.storedEventsLeagueMax) * data.data.storedEventsAvg) + 39;

	return (
		<>
			<AverageLineSC averageLine={averageLine}/>
			<AverageScoreSC averageLine={averageLine}>{`Stored Events Average: ${data.data.storedEventsAvg}`}</AverageScoreSC>
		</>
	)
}

const StoredEventAnalyticsData = () => {
	const [data, setData] = useState('');
    useEffect(() => {
        async function graphData() {
			let response = await fetch("https://betstore.asbresearch.com/api/barchart", {mode: 'cors'});
			let data = await response.json();
			// setData(data);
			return data;
		};
		graphData().then(data => setData(data));
	}, []);

    let columnContainerRef = useRef(null);
	return (
		<>
			<GraphYTextContainer amountOfLines={data.storedEventsLeagueMax / 5}>
				<GraphYText data={data}/>
				<GridXLinesAmount data={data}/>
			</GraphYTextContainer>
			<ColumnContainer ref={columnContainerRef}>
				<SingleColumn />
			</ColumnContainer>
			<Key />
			<AverageLine data={data}/>
		</>
	);
};

export default StoredEventAnalyticsData;