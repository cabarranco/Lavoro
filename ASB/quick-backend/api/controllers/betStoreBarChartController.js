// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

'use strict';

// [START bigquery_query_batch]
// Import the Google Cloud client library and create a client
const {BigQuery} = require('@google-cloud/bigquery');

const options = {
    keyFilename: 'asbanalytics-981daed66622.json',
    projectId: 'asbanalytics',
    };

const bigquery = new BigQuery(options);

var storedEventsLeagueMax = 0;
  
exports.get_chart_values = async function(req, res) {
  
    // Run the query as a job
    const [job] = await bigquery.createQueryJob(barChartQuery());
  
    // Wait for the query to finish
    const [rows] = await job.getQueryResults();

    var competitions = getGroupedBy(rows, "competition")

    // Run the query as a job
    const [jobo] = await bigquery.createQueryJob(eventsAverage());
  
      // Wait for the query to finish
    const [row] = await jobo.getQueryResults();

    // This method also set the storedEventsLeadueMax, must be called before
    // that variable is used
    var storedEvents = storedEventsLeagueSplit(competitions);

    var response = {
        "storedEventsAvg": Math.round(row[0].avg),
        
        "storedEventsLeagueMax": storedEventsLeagueMax,
        
        "storedEventsLeagueSplit": storedEvents
      }

    res.send(response);

    // res.send({
    //     "storedEventsAvg": [43],
    //     "storedEventsLeagueMax": [65],
    //     "storedEventsLeagueSplit": {
    //         "Serie A": [13,19,26,7],
    //         "Primera Liga": [6,17,35,0],
    //         "Bundesliga": [3,6,26,23],
    //         "Ligue 1": [25,13,2,2],
    //         "Premier League": [19,8,4,8],
    //         "Eredivisie": [5,8,10,2],
    //         "Liga Portuguesa": [3,4,5,3],
    //     }
    // });
}

/**
 * Return the query to retrieve the bars in the chart
 */
function barChartQuery() {
    // Create query job configuration. For all options, see
    // https://cloud.google.com/bigquery/docs/reference/rest/v2/Job#jobconfigurationquery
    const query = `SELECT COUNT (distinct market_catalogue.eventId) AS events_number, competition, startDate 
    FROM \`asbanalytics.odds_sizes.market_catalogue\` market_catalogue
    INNER JOIN \`asbanalytics.odds_sizes.event_updates\` event_updates
    ON (market_catalogue.eventId = event_updates.eventId)
    WHERE (market_catalogue.frequency = "TICK" OR market_catalogue.frequency IS NULL) AND event_updates.inPlayMatchStatus = "Finished"
    AND (market_catalogue.startDate >= '`+startYear()+`-08-01' AND market_catalogue.startDate <= '`+endYear()+`-07-31')
    GROUP by startDate, competition order by competition`;

    /// For all options, see https://cloud.google.com/bigquery/docs/reference/rest/v2/jobs/query
    const optionsQuery = {
        query: query,
        // Location must match that of the dataset(s) referenced in the query.
        location: 'europe-west2',
    };

    return optionsQuery;
}

/**
 * Return the query to get the events average value
 */
function eventsAverage() {
    const averageQuery = `SELECT AVG(events_number) as avg
    FROM (
      SELECT COUNT (distinct market_catalogue.eventId) AS events_number
      FROM \`asbanalytics.odds_sizes.market_catalogue\` market_catalogue
      INNER JOIN \`asbanalytics.odds_sizes.event_updates\` event_updates
      ON (market_catalogue.eventId = event_updates.eventId)
      WHERE event_updates.inPlayMatchStatus = "Finished"
      AND (market_catalogue.startDate >= '`+startYear()+`-08-01' AND market_catalogue.startDate <= '`+endYear()+`-07-31')
      GROUP by competition
    );`;

    /// For all options, see https://cloud.google.com/bigquery/docs/reference/rest/v2/jobs/query
    const optQuery = {
        query: averageQuery,
        // Location must match that of the dataset(s) referenced in the query.
        location: 'europe-west2',
      };
  
    return optQuery;
}

/**
 * Get the current year
 */
function startYear() {
    var currentDate = new Date();
    var currentMonth = currentDate.getMonth();
    var currentYear = currentDate.getFullYear();

    if (currentMonth > 7)
        return currentYear;

    return currentYear -1;
}

/**
 * Get the current year
 */
function endYear() {
    var currentDate = new Date();
    var currentMonth = currentDate.getMonth();
    var currentYear = currentDate.getFullYear();

    var endYear = currentYear;

    if (currentMonth > 7)
        endYear = currentYear + 1;

    return endYear;
}

/**
 * Goup the list of competitions by name.
 * 
 * @param {*} competitions is the list of competitions
 * @param {*} key is name of the field used to group the list
 */
function getGroupedBy(competitions, key) {
    var groups = {}, result = [];

    competitions.forEach(function (a) {
        if (!(a[key] in groups)) {
            groups[a[key]] = [];
            result.push(groups[a[key]]);
        }
        groups[a[key]].push(a);
    });
    return result;
}

/**
 * Get the quarters for each competition and sum the number of events.
 * This method also set the storedEventsLeadueMax, must be called before
 * that variable is used
 * 
 * @param {list} groups this is a list of competitions grouped by namwe
 */
function storedEventsLeagueSplit(groups) {

    var storedEvents = {};

    groups.forEach(function (a) {
        var q1 = 0; var q2 = 0; var q3 = 0; var q4 = 0;
        a.forEach(function(b) {
            var date = new Date(Date.parse((b["startDate"].value)));
            var month = date.getMonth();

            if (month == 7 || month == 8 || month == 9) {
                q1+=b["events_number"]
            }
            if (month == 10 || month == 11 || month == 0) {
                q2+=b["events_number"]
            }
            if (month == 1 || month == 2 || month == 3) {
                q3+=b["events_number"]
            }
            if (month == 4 || month == 5 || month == 6) {
                q4+=b["events_number"]
            }
        });

        if (q1 > storedEventsLeagueMax) storedEventsLeagueMax = q1;
        if (q2 > storedEventsLeagueMax) storedEventsLeagueMax = q2;
        if (q3 > storedEventsLeagueMax) storedEventsLeagueMax = q3;
        if (q4 > storedEventsLeagueMax) storedEventsLeagueMax = q4;

        storedEvents[a[0]["competition"]] = [q1,q2,q3,q4];
    });

    return sortEvents(storedEvents);
}

/**
 * Sort events by sum of quarters in DESC order.
 * 
 * @param {*} events list of stored events.
 */
function sortEvents(events) {

    let objectKeys = Object.keys(events);
    let orderedEvents = {};
    let sums = [];

    objectKeys.forEach(function(e) {
        let obj = {
            name: e,
            sum: (events[e][0] + events[e][1] + events[e][2] + events[e][3])
        }
        sums.push(obj);
    });

    let ordered = sums.sort(function(a, b) {
        return parseInt(b.sum) - parseInt(a.sum);
    });

    ordered.forEach(function(e) {
        orderedEvents[e.name] = events[e.name];
    });  

    return orderedEvents;
}