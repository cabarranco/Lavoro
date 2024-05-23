'use strict';
var mongoose = require('mongoose');
var Schema = mongoose.Schema;

// Create schema for scheduled event
var EventSchema = new Schema({
  eventId: {
    type: Integer,
    required: 'Event ID is compulsory'
  },
  event: {
    type: String,
    required: 'Event name is compulsory'
  },
  openDate: {
    type: Date,
    required:  'Open Date is compulsory'
  },
  competitionId: {
    type: Integer,
    required:  'Competition ID is compulsory'
  },
  competition: {
    type: String,
    required: 'Competition name is compulsory'
  },
  entryTs: {
    type: Date
  },
  checked: {
      type: Boolean,
      default: false
  }
});

module.exports = mongoose.model('Event', EventSchema);