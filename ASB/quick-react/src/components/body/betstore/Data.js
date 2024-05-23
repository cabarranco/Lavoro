import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import betstoreSiteData from "../../../data/betstoreSiteData";
import StoredEventAnalyticsData from "./Charts";
// import Charts_amCharts from "./Charts_amCharts";

const DataContainer = styled.div`
  ${props => props.span2cols ? "grid-column: 2 / span 2" : null};
  ${props => props.span2rows ? "grid-row: 1 / span 2" : null};
  height: ${props => props.span2rows ? "720px" : props.isGraph ? "500px" : "200px"};
  border-radius: 5px;
  margin: 10px;
  background: #252524;
  /* ${props => props.allowOverflow ? "overflow-y: scroll;" : null}; */
`;

const ChartsHeader = styled.div`
  display: flex;
  align-items: center;
  /* justify-content: center; */
  width: 100%;
  height: 40px;
  background: #191919;
`;

const DataContainerTitle = styled.div`
  display: inline-block;
  /* height: 20px; */

  font-family: 'Montserrat', sans-serif;
  font-size: 14px;
  font-weight: 500;
  text-transform: uppercase;

  margin: 0 10px;
`;

const ChartsSubheader = styled.div`
  display: flex;
  align-items: center;
  /* justify-content: center; */
  width: 100%;
  height: 40px;
  background: #383838;
`;

const SubheaderContainer = styled.div`
	position: relative;
	display: ${props => props.isGraph ? "flex" : "grid"};
	grid-template-columns: ${props => props.isGraph ? "20px auto" : (!props.eventDate && !props.competition) ? "auto" : props.has4cols ? "30% 25% 30% 15%" : "30% 25% 45%"};
	width: ${props => (props.has4cols || props.isGraph) ? "calc(100% - 10px)" : "calc(100% - 17px)"};
	margin-left: 10px;
	/* ${props => props.has4cols ? "border-bottom: 1px solid #979797;" : null} */
	/* overflow-x: hidden; ANDY HERE - Monitor this */
	${props => props.isGraph ? 
	"height: calc(100% - 80px); overflow-y:hidden; > div:first-child {border-right: 1px solid white;}" 
	: null};
	${props => props.isEvents ? 
	"max-height: 640px; overflow-y: scroll; align-items: center;}" 
	: null};
	${props => props.isScheduledEvents ? 
	"max-height: 120px; overflow-y: scroll; }" 
	: null};

	&::-webkit-scrollbar {
		width: 6px;
	};
	&::-webkit-scrollbar-track {
		background: #676767;
	};
	&::-webkit-scrollbar-thumb {
		background: #BCBCBC;
	};

	${props => props.isGraph ? "overflow-x: hidden;&::-webkit-scrollbar {width: 0px;height: 0px;};" : null};

`;

const SubheaderTitleContainer = styled.div`
	position: relative;
	font-family: 'Montserrat', sans-serif;
	font-size: 12px;
	font-weight: 400;
	text-transform: uppercase;
	
	${props => props.has4cols ? 
		`&:after {
			position: relative;
			top: 10px;
			margin-left: 8px;
			content: "";
			width: 0; 
			height: 0; 
			border-left: 3px solid transparent;
			border-right: 3px solid transparent;
			border-top: 4px solid #8CC63D;

			@media only screen and (max-width: 1200px) {
				top: 9px;
			}
		}` 
		: null
	};
	@media only screen and (max-width: 1200px) {
		font-size: 11px;
	}
	@media only screen and (max-width: 1100px) {
		font-size: 10px;
	}
`;

const SubheaderDropdownContainer = styled.div`
	position: absolute;
	top: -35px;
	left: -10px;
	/* width: 320px; */
	width: ${props => props.eventDateDropdown ? "150px" : "320px"};
	min-height: 65px;
	max-height: 320px;
	background: #383838;
	padding: 10px;
	margin-top: 12px;
	text-transform: uppercase;
	-webkit-box-shadow: 0px 10px 19px 0px rgba(0,0,0,0.75);
	-moz-box-shadow: 0px 10px 19px 0px rgba(0,0,0,0.75);
	box-shadow: 0px 10px 19px 0px rgba(0,0,0,0.75);
	z-index: 10;grid-area: 2 / 2;
	${props => props.eventDropdown ?  "grid-area: 2 / 1;" : null}
	${props => props.eventDateDropdown ?  "grid-area: 2 / 2;" : null}
	${props => props.competitionDropdown ? "grid-area: 2 / 3;" : null};

	@media only screen and (min-width: 1100px) { 
		top: -36px;
	}
	@media only screen and (min-width: 1100px) { 
		top: -37px;
	}

	label {
		font-family: 'Montserrat';
		font-size: 14px;
		line-height: 18px;
		letter-spacing: 1.4px;
	}
`;

const SearchInput = styled.input`
	display: block;
	margin: 10px 0;
	width: 98%;
	height: 25px;
	&::placeholder {
		font-family: 'Montserrat';
		font-size: 10px;
		line-height: 13px;
		letter-spacing: 1px;
		text-transform: uppercase;
	}
`;

const CheckboxWrapper = styled.div`
	display: inline-flex;
	width: 100%;
	height: 20px;
`;
const CheckboxInput = styled.input`
	margin: 8px 15px 8px 3px;
`;

const SubheaderTitleContainerClose = styled(SubheaderTitleContainer)`
	margin-bottom: 10px;

	&:after {
		position: relative;
		top: -10px;
		margin-left: 8px;
		content: "";
		width: 0; 
		height: 0; 
		border-left: 3px solid transparent;
		border-right: 3px solid transparent;
		border-bottom: 4px solid #8CC63D;
	}
`;

let allSelectedDates = [];

const SubheaderDropdown = (props) => {
	const {
		eventDropdown,
		eventDateDropdown,
		competitionDropdown,
		eventDateData,
		competitionData,
		eventName,
		closeDropdown,
		filter,
		eventsData,
		filterDate
	} = props; // use props to generate content specific to drowdown
	
	const submitFilter = (e) => {
		if (e.keyCode === 13) {
			return filter(e.target.value);
		}
	}

	let submitSelectedDates = () => {
		filterDate(allSelectedDates);
	}

	let dropdownFields;

	if (eventDateDropdown) {
		let allEventDates = [];
		eventsData.map(data => {
			let date = new Date(data?.event?.openDate);
			let formattedDate = `${date?.getDate()}/${date?.getMonth() + 1}/${date?.getYear() - 100}`;
			if (!allEventDates.includes(formattedDate)) {
				allEventDates.push(formattedDate);
			};
		});

		let convertEventDates = allEventDates.map(date => {
			var initial = date.split(/\//);
			let mmddyy = new Date([ initial[1], initial[0], initial[2] ].join('/'));
			return mmddyy;
		});
		let sortedEventDates = convertEventDates.sort((a, b) => a - b);
		let ddmmyySortedEventDates = sortedEventDates.map(date => {
			let formattedDate = `${date?.getDate()}/${date?.getMonth() + 1}/${date?.getYear() - 100}`;
			return formattedDate;
		})

		let selectDate = (date) => {
			if (allSelectedDates.includes(date)) {
				const index = allSelectedDates.indexOf(date);
				if (index > -1) {
					allSelectedDates.splice(index, 1);
					submitSelectedDates();
				}
			} else {
				allSelectedDates.push(date);
				submitSelectedDates();
			}
		}

		dropdownFields = ddmmyySortedEventDates.map((date, i) => {
			return (
				<div key={`${date}_${i}`}>
					<CheckboxInput key={`${date}_0${i}`} type="checkbox" defaultChecked={allSelectedDates.includes(date)} name="event" value="event" onClick={() => {selectDate(date)}} />
					<label key={`${date}_00${i}`} htmlFor="event">{date}</label>
					{/* <div onClick={submitSelectedDates}>SUBMIT</div> */}
				</div>
			);
		});
	}

	if (competitionDropdown) {
		let allEventCompetitions = [];
		eventsData.map(data => {
			if (!allEventCompetitions.includes(data?.competition)) {
				allEventCompetitions.push(data?.competition);
			};
		});

		dropdownFields = allEventCompetitions.map(competition => {
			return (
			<div>
				<CheckboxInput type="checkbox" name="event" value="event" />
				<label for="event">{competition}</label>
			</div>
			)
		});
		// console.log("allEventCompetitions", allEventCompetitions);
	}

	let containerStyle = {
		height: "305px",
		overflow: "scroll"
	}

	return (
		<>
			<SubheaderDropdownContainer eventDropdown={eventDropdown} eventDateDropdown={eventDateDropdown} competitionDropdown={competitionDropdown}>
				<SubheaderTitleContainerClose onClick={() => closeDropdown(null, null)}>{eventName}</SubheaderTitleContainerClose>
				{!eventDateDropdown && <SearchInput autoFocus type="text" placeholder="Search..." height="50" onKeyDown={submitFilter}/>}
				{(eventDateDropdown || competitionDropdown) &&
					<div style={containerStyle}>
						{dropdownFields}
					</div>
				}

				{/* {!eventDateDropdown && <SearchInput autoFocus type="text" placeholder="Search..." height="50" onKeyDown={submitFilter}/>}
				{!eventDropdown &&<CheckboxWrapper>
					<CheckboxInput type="checkbox" name="event" value="event" />
					<label for="event">Event</label>
				</CheckboxWrapper>} */}
			</SubheaderDropdownContainer>
		</>
	)
}

const Subheader = ({eventName, eventDate, competition, has4cols, isGraph, hasDropdown, filter, filterEventDate, eventsData}) => {
	let [eventDropdown, setEventDropdown] = useState(false);
	let [eventDateDropdown, setEventDateDropdown] = useState(false);
	let [competitionDropdown, setCompetitionDropdown] = useState(false);
	
	const handleDropdown = (setCurrentDropdown, currentDropdown) => {
		// both work, if() effects only necessary states, other effects all state every click
		// if(currentDropdown === eventDropdown) {
		// 	setEventDateDropdown(false);
		// 	setCompetitionDropdown(false);
		// }
		// if(currentDropdown === eventDateDropdown) {
		// 	setEventDropdown(false);
		// 	setCompetitionDropdown(false);
		// }
		// if(currentDropdown === competitionDropdown) {
		// 	setEventDropdown(false);
		// 	setEventDateDropdown(false);
		// }
		setEventDropdown(false);
		setEventDateDropdown(false);
		setCompetitionDropdown(false);
		if (hasDropdown) {
			setCurrentDropdown && setCurrentDropdown(!currentDropdown);
		}
	}

	const filterDate = (e) => {
		filterEventDate(e);
	};
	const filterCompetition = (e) => {
		alert(`COMPETITION - DO WE NEED A SEPERATE FILTER FOR EACH COLUMN - e: ${e}`);
	};
	return (
		<>
			<SubheaderTitleContainer has4cols={has4cols} onClick={() => handleDropdown(setEventDropdown, eventDropdown)}>
				{eventName}
			</SubheaderTitleContainer>
			<SubheaderTitleContainer has4cols={has4cols} onClick={() => handleDropdown(setEventDateDropdown,eventDateDropdown)}>{eventDate ? "Event date" : null}</SubheaderTitleContainer>
			<SubheaderTitleContainer has4cols={has4cols} onClick={() => handleDropdown(setCompetitionDropdown ,competitionDropdown)}>{competition ? "Competition" : null}</SubheaderTitleContainer>
			{(eventDropdown && !isGraph) ? <SubheaderDropdown eventDropdown eventName={"Event name"} closeDropdown={() => handleDropdown()} filter={filter} eventsData={eventsData}/> : null}
			{eventDateDropdown && <SubheaderDropdown eventDateDropdown eventName={"Event date"} eventDateData={"data"} closeDropdown={() => handleDropdown()} eventsData={eventsData} filterDate={filterDate}/>}
			{competitionDropdown && <SubheaderDropdown competitionDropdown eventName={"Competition"} competitionData={"data"} closeDropdown={() => handleDropdown()} filter={filterCompetition} eventsData={eventsData}/>}
		</>
	)
}

// Split out below to seperate files and import

const EventsDataContainer = styled.div`
	display: flex;
	align-items: center;
	height: 30px;
	font-family: 'Montserrat', sans-serif;
	font-size: 10px;
	font-weight: 400;
	color: ${props => props.greenText ? "#8CC63D" : "inherit"};
	border-bottom: 1px solid rgba(151,151,151, 0.2);
	overflow: hidden;

	/* Ellipsis
	overflow: hidden;
	text-overflow: ellipsis;
	-webkit-line-clamp: 2;
	display: -webkit-box;
	-webkit-box-orient: vertical; */

	> div {
		color: #1A1A1A;
		font-size: 9px;
    	font-weight: 600;
		text-transform: uppercase;
		letter-spacing: 0.8px;
		cursor: pointer;

		display: flex;
    	justify-content: center;
    	align-items: center;
		width: 30px;
    	height: 15px;
		background: ${props => props.added ? "none" : "#8CC63D"};
		padding: 3px;
		border-radius: 2px;

		${props => props.added ? `
			color: #979797;

			border: 1px solid #979797;
			width: 28px;
    		height: 13px;
		` : null
		}
	}
`;

const checkmark = '\u2714';

const SingleEventsData = ({ data, scheduledEventsData }) => {
	const [add, setAdd] = useState(false);
	// const [addFunction, setAddFunction] = useState();

	let currentScheduledEvents = scheduledEventsData.map(x => {
		return x.event;
	});

	let id = data?.event?.id;

	const triggerAdd = () => {
		setAdd(!add);

			if (!currentScheduledEvents.includes(data.event.name)) {
				console.log("NEW EVENT!!");
				(async () => {
					let addToScheduledEvents = await fetch(`https://betstore.asbresearch.com/api/schedule/${id}`, {mode: 'cors'});
					return addToScheduledEvents?.url;
				})();
			}
		
		// (async () => {
		// 	let addToScheduledEvents = await fetch(`https://betstore.asbresearch.com/api/schedule/${id}`, {mode: 'cors'});
		// 	return addToScheduledEvents?.url;
		// })();

        // async function addToScheduledEvents() {
		// 	let addToScheduledEvents = await fetch(`https://betstore.asbresearch.com/api/schedule/${id}`, {mode: 'cors'});
		// 	return addToScheduledEvents?.url;
		// }

        // async function deleteFromEvents() {
		// 	let deleteFromEvents = await fetch(`https://betstore.asbresearch.com/api/delete/${id}`, {mode: 'cors'});
		// 	return deleteFromEvents?.url;
		// }

		// (async () => {
		// 	await addToScheduledEvents();
		// 	// await deleteFromEvents();
		// 	// console.log(`DELETED: ${id}`);
		// 	// let deleteFromEvents = await fetch(`https://betstore.asbresearch.com/api/delete/${id}`, {mode: 'cors'});
		// 	// return deleteFromEvents?.url;
		// })();

		// async function getAddFunction() {
		// 	let response = await fetch("https://betstore.asbresearch.com/api/events", {mode: 'cors'});
		// 	let data = await response.json();
		// 	setAddFunction(data);
		// 	// return data;
		// };
		// getAddFunction().then(data => setData(data));
	};

	let date = new Date(data?.event?.openDate);
	let formattedDate = `${date.getDate()}/${date.getMonth() + 1}/${date.getYear() - 100}`; 

	return (
		<>
			<EventsDataContainer greenText>{data?.event?.name}</EventsDataContainer>
			<EventsDataContainer>{formattedDate}</EventsDataContainer>
			<EventsDataContainer>{betstoreSiteData.events.competition}</EventsDataContainer>
			<EventsDataContainer added={add}>
				<div onClick={!add ? triggerAdd : null}>{betstoreSiteData.events.added ? (!add ? "Add" : checkmark) : null}</div>
			</EventsDataContainer>
		</>
	);
};

const EventsData = ({ 
	eventsData,
	scheduledEventsData,
	filteredEventsData,
	userSearchEventsData,
	userFilterEventsDate
	}) => {
	let userFilteredEventsData = filteredEventsData.filter(x => x.toLowerCase().includes(userSearchEventsData.toLowerCase()));

	let allDates = [];
	eventsData.map(data => {
		let date = new Date(data?.event?.openDate);
		let formattedDate = `${date?.getDate()}/${date?.getMonth() + 1}/${date?.getYear() - 100}`;
		allDates.push(formattedDate);
	})

	let filteredAllDates = [];
	if(userFilterEventsDate.length === 0) {
		filteredAllDates[0] = allDates.filter((date, index) => allDates.indexOf(date) === index);
	} else if (userFilterEventsDate[0].length === 0) {
		filteredAllDates[0] = allDates.filter((date, index) => allDates.indexOf(date) === index);
	} else {
		filteredAllDates = userFilterEventsDate;
	}
	// console.log("filteredAllDates", filteredAllDates);

	return (
		<>
		{eventsData.map((data, i) => {
			let date = new Date(data?.event?.openDate);
			let formattedDate = `${date?.getDate()}/${date?.getMonth() + 1}/${date?.getYear() - 100}`;

			if(userFilteredEventsData.includes(data?.event?.name)) {
				if(filteredAllDates[0].includes(formattedDate)) {
					return (
						<>
							<SingleEventsData key={`${data?.event?.id}_${i}`} data={data} scheduledEventsData={scheduledEventsData}/>
						</>
					)
				}
			}
			return null;
		})}
		</>
	);
};

// const ScheduledDataContainer = styled.div`
// 	display: flex;
// 	align-items: center;
// 	height: 30px;
// 	font-family: 'Montserrat', sans-serif;
// 	font-size: 12px;
// 	font-weight: 400;
// 	color: ${props => props.greenText ? "#8CC63D" : "inherit"};

// 	> div {
// 		color: #1A1A1A;
// 		font-size: 9px;
//     	font-weight: 600;
// 		text-transform: uppercase;
// 		letter-spacing: 0.8px;

// 		display: flex;
//     	justify-content: center;
//     	align-items: center;
// 		width: 30px;
//     	height: 15px;
// 		background: ${props => props.added ? "none" : "#8CC63D"};
// 		padding: 3px;
// 		border-radius: 2px;

// 		${props => props.added ? `
// 			color: #979797;

// 			border: 1px solid #979797;
// 			width: 28px;
//     		height: 13px;
// 		` : null
// 		}
// 	}
// `;

const SingleScheduledEventsData = (data) => {
	const [add, setAdd] = useState(false);

	let id = data?.data?.eventId;

	const triggerAdd = () => {
		setAdd(!add);
		(async () => {
			console.log("EVENT REMOVED");
			let addToScheduledEvents = await fetch(`https://betstore.asbresearch.com/api/delete/${id}`, {mode: 'cors'});
			return addToScheduledEvents?.url;
		})();
	};

	let date = new Date(data?.data?.openDate);
	let formattedDate = `${date.getDate()}/${date.getMonth() + 1}/${date.getYear() - 100}`;

	return (
		<>
			<EventsDataContainer greenText>{data?.data?.event}</EventsDataContainer>
			<EventsDataContainer>{formattedDate}</EventsDataContainer>
			<EventsDataContainer>{data?.data?.competition}</EventsDataContainer>
			<EventsDataContainer added={add}>
				<div onClick={!add ? triggerAdd : null}>{betstoreSiteData.events.added ? (!add ? "X" : checkmark) : null}</div>
			</EventsDataContainer>
		</>
	);
};

const ScheduledEventsData = ({ scheduledEventsData }) => {
	return (
		<>
		{scheduledEventsData.map(data => {
			return (
				<>
					<SingleScheduledEventsData key={data?.eventId} data={data}/>
				</>
			)
		})}
		</>
	);
};

// Split out above to seperate files and import

export const Data = (props) => {
    const {
        title,
        span2rows,
        isGraph,
		allowOverflow,
		eventName,
		eventDate,
		competition,
		has4cols, 
		hasDropdown,
		isEvents,
		isScheduledEvents
	} = props;

	// make API call here for Events, store in useState, pass data through to EventsData, access data in EventsData. Subheader contains SubheaderDropdown, Filter from inside SubheaderDropdown

	const [eventsData, setEventsData] = useState([]);
	const [scheduledEventsData, setScheduledEventsData] = useState([]);
	const [userSearchEventsData, setUserSearchEventsData] = useState("");
	const [userFilterEventsDate, setUserFilterEventsDate] = useState([]);

	let eventsDataNames = eventsData.map(data => {
		return data?.event?.name
	});
	let ScheduledEventsDataNames = scheduledEventsData.map(data => {
		return data?.event;
	})

	const filteredEventsData = eventsDataNames.filter(event => !ScheduledEventsDataNames.includes(event));

	useEffect(() => {
        async function eventsData() {
			let response = await fetch("https://betstore.asbresearch.com/api/events", {mode: 'cors'});
			let data = await response.json();
			return data;
		};
		eventsData().then(data => setEventsData(data));
	}, []);

	useEffect(() => {
        async function scheduledEventsData() {
			let response = await fetch("https://betstore.asbresearch.com/api/scheduled-events", {mode: 'cors'});
			let data = await response.json();
			return data;
		};
		scheduledEventsData().then(data => setScheduledEventsData(data));
	}, []);

	let filter = (e) => {
		setUserSearchEventsData(e);
	}

	let filterEventDate = (eventDate) => {
		// setUserFilterEventsDate(eventDate);
		setUserFilterEventsDate(prevState => [...prevState, eventDate]);
		console.log("userFilterEventsDate", userFilterEventsDate);
	}

	let resetEventsData = () => {
		setUserSearchEventsData("");
		setUserFilterEventsDate([]);
	}

    return (
        <>
        <DataContainer span2rows={span2rows} isGraph={isGraph} allowOverflow={allowOverflow}>
            <ChartsHeader>
              <DataContainerTitle>{title}</DataContainerTitle>
            </ChartsHeader>
            <ChartsSubheader>
				<SubheaderContainer eventDate={eventDate} competition={competition}>
					<Subheader
						eventName={eventName}
						eventDate={eventDate}
						competition={competition}
						has4cols={has4cols}
						isGraph={isGraph}
						hasDropdown={hasDropdown}
						filter={filter}
						filterEventDate={filterEventDate}
						eventsData={eventsData}
					/>
					{/* <div onClick={resetEventsData}>x</div> */}
				</SubheaderContainer>
            </ChartsSubheader>
			{/* <SubheaderContainer eventDate={eventDate} competition={competition} has4cols={has4cols}> USE THIS WITH Charts_amCharts component*/}
			<SubheaderContainer eventDate={eventDate} competition={competition} has4cols={has4cols} isGraph={isGraph} isEvents={isEvents} isScheduledEvents={isScheduledEvents}>
				{title === "Events" ? <EventsData eventsData={eventsData} scheduledEventsData={scheduledEventsData} filteredEventsData={filteredEventsData} userSearchEventsData={userSearchEventsData} userFilterEventsDate={userFilterEventsDate}/> 
				: title === "Scheduled events - Tick frequency" ? <ScheduledEventsData scheduledEventsData={scheduledEventsData}/> 
				: title === "Stored events analytics - Tick frequency" ? <StoredEventAnalyticsData />
				// : title === "Stored event analytics" ? <Charts_amCharts /> 
				: null}
			</SubheaderContainer>
        </DataContainer>
        </>
    );
};

export default Data;