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

const betfairController = require('./betfairController');
const MongoClient = require('mongodb').MongoClient;
const url = "mongodb://admin:XUFpy9Zc8A6pRNtfhGgQHzSG@localhost:27017/";
// const url = "mongodb://localhost:27017/";

/**
 * Get the list of the schedule events.
 * TODO: Apply filter to get events in progress or future
 */
exports.get_scheduled_events = async function(req, res) {

    let events = await new Promise((resolve, reject) => scheduled_events(resolve, reject));

    res.send(events);
}

/**
 * Get the list of the schedule events.
 * TODO: Apply filter to get events in progress or future
 */
exports.schedule = async function(req, res) {

    let event = await new Promise((resolve, reject) => add_event(resolve, reject, req.params.eventId));
    res.send(event);
}

/**
 * Get the list of the schedule events.
 * TODO: Apply filter to get events in progress or future
 */
exports.remove_event = async function(req, res) {

    let response = await new Promise((resolve, reject) => remove_event(resolve, reject, req.params.eventId));

    res.send(response);
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
          resolve(result)
        });
    });
}

/**
 * Get kist of ids for scheduled events in mongodb.
 * 
 * @param {*} resolve promise callback to resolve it.
 * @param {*} reject promise callback to reject it.
 */
async function add_event(resolve, reject, eventId) {

    let token = await new Promise((resolve, reject) => betfairController.bf_login(resolve, reject));
    let event = await new Promise((resolve, reject) => betfairController.get_event(resolve, reject, eventId, token));
    let competition = await new Promise((resolve, reject) => betfairController.bf_get_competition(resolve, reject, token, eventId))

    let response = event[0];
    response.competition = competition[0].competition.name;
    response.competitionId = competition[0].competition.id;

    MongoClient.connect(url, function(err, db) {
        if (err) throw err;
        var dbo = db.db("events_scheduler");

        var myobj = {
            eventId : response.event.id,
            event : response.event.name,
            openDate : response.event.openDate,
            competitionId : response.competitionId,
            competition : response.competition,
            entryTs : new Date(),
            checked : false
        };

        dbo.collection("events").insertOne(myobj, function(err, res) {
          if (err) reject(err);
          
          resolve("ok")
          
          db.close();
        });
      });
}

/**
 * Get kist of ids for scheduled events in mongodb.
 * 
 * @param {*} resolve promise callback to resolve it.
 * @param {*} reject promise callback to reject it.
 */
async function remove_event(resolve, reject, eventId) {
    
    MongoClient.connect(url, function(err, db) {

        if (err) throw err;

        var dbo = db.db("events_scheduler");
        var myquery = { eventId: eventId.toString() };

        dbo.collection("events").deleteOne(myquery, function(err, obj) {
            if (err) reject(err);
            console.log("object: " + obj);
            console.log("err: " + err);
            db.close();
            resolve("ok")
        });
    });
}