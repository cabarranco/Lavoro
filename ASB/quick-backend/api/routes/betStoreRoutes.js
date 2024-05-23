'use strict';

module.exports = function(app) {
  
  var bsBarChartController = require('../controllers/betStoreBarChartController');
  var betfaitController = require('../controllers/betfairController');
  var schedulerController = require('../controllers/schedulerController');

  app.route('/api/barchart')
    .get(bsBarChartController.get_chart_values);

  app.route('/api/events')
    .get(betfaitController.get_events);

  app.route('/api/scheduled-events')
    .get(schedulerController.get_scheduled_events);

  app.route('/api/schedule/:eventId')
    .get(schedulerController.schedule);

  app.route('/api/delete/:eventId')
    .get(schedulerController.remove_event);
    
};