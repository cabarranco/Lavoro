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

const request = require('request');
const MongoClient = require('mongodb').MongoClient;
const url = "mongodb://admin:XUFpy9Zc8A6pRNtfhGgQHzSG@localhost:27017/";

exports.get_events = async function(req, res) {

    let token = await new Promise((resolve, reject) => login(resolve, reject));
    let eventsIds = await new Promise((resolve, reject) => scheduled_events(resolve, reject));

    let events = await new Promise((resolve, reject) => get_events(resolve, reject, token, eventsIds));

    let eventsWithCompetition = [];

    for (const e of events) {
        // let competition = await new Promise((resolve, reject) => get_competition(resolve, reject, token, e.event.id))
    
        // e.competition = competition[0].competition.name;
        e.competition = "Not found";
        eventsWithCompetition.push(e);
    }

    res.send(eventsWithCompetition);
}

exports.get_event = async function(resolve, reject, eventId, token) {

    request.post({
        url: 'https://api.betfair.com/exchange/betting/rest/v1.0/listEvents/',
        headers: {
            'X-Authentication': token,
            'Content-Type': 'application/json',
            'X-Application': 'HbJqZMPZB4oB0NsW'
        },
        json: {
            filter: {
                eventTypeIds: [1],
                eventIds: [eventId],
                marketTypeCodes: ["MATCH_ODDS", "OVER_UNDER_25", "CORRECT_SCORE"]
            }
        },
        rejectUnauthorized: false
    }, function (err, httpResponse, body) {

        if (err) reject(err);

        resolve(body);
    });
}

async function get_events(resolve, reject, token, eventsIds) {
    request.post({
        url: 'https://api.betfair.com/exchange/betting/rest/v1.0/listEvents/',
        headers: {
            'X-Authentication': token,
            'Content-Type': 'application/json',
            'X-Application': 'HbJqZMPZB4oB0NsW'
        },
        json: {
            filter: {
                eventTypeIds: [1],
                marketTypeCodes: ["MATCH_ODDS", "OVER_UNDER_25", "CORRECT_SCORE"]
            }
        },
        rejectUnauthorized: false
    }, function (err, httpResponse, body) {

        let events = [];

        if (err) reject(err);

        body.forEach(function(e){
            e.selected = eventsIds.includes(e.event.id)

            events.push(e);
        });

        resolve(events);
    });
}

/**
 * Get kist of ids for scheduled events in mongodb.
 * 
 * @param {*} resolve promise callback to resolve it.
 * @param {*} reject promise callback to reject it.
 */
async function scheduled_events(resolve, reject) {
    
    MongoClient.connect(url, function(err, db) {

        if (err) throw err;
        var dbo = db.db("events_scheduler");
        var query = {};

        dbo.collection("events").find(query).toArray(function(err, result) {
          
          if (err) reject(err);

          db.close();

          let ids = [];

          result.forEach(function(e) {
            ids.push(e.eventId);
          });

          resolve(ids)
        });
    });
}

/**
 * Perform betfair login and return the token.
 * 
 * @param {*} resolve promise callback to resolve it.
 * @param {*} reject promise callback to reject it.
 */
async function login(resolve, reject) {
    
    request.post({
        url: 'https://identitysso.betfair.com/api/login',
        form: {
            username: 'fdr@asbresearch.com',
            password: 'asbcheqai87'
        },
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/x-www-form-urlencoded',
            'X-Application': 'HbJqZMPZB4oB0NsW'
        },
        rejectUnauthorized: false
    }, function (err, httpResponse, body) { 
        let response = JSON.parse(body)
        resolve(response.token);
    });
}

/**
 * Public login method. Call the private one
 */
exports.bf_login = async function(resolve, reject) {
    let token = await new Promise((resolve, reject) => login(resolve, reject));

    resolve(token);
}

/**
 * Get competition from event id.
 */
async function get_competition(resolve, reject, token, eventId) {
    request.post({
        url: 'https://api.betfair.com/exchange/betting/rest/v1.0/listCompetitions/',
        headers: {
            'X-Authentication': token,
            'Content-Type': 'application/json',
            'X-Application': 'HbJqZMPZB4oB0NsW'
        },
        json: {
            filter: {
                eventTypeIds: [1],
                eventIds: [eventId]
            }
        },
        rejectUnauthorized: false
    }, function (err, httpResponse, body) {

        if (err) reject(err);

        resolve(body);
    });
}

/**
 * Public version of the private method get competition
 */
exports.bf_get_competition = async function(resolve, reject, token, eventId) {
    let competition = await new Promise((resolve, reject) => get_competition(resolve, reject, token, eventId))

    resolve(competition);
}


