var express = require('express'),
  app = express(),
  host = process.env.HOST || '127.0.0.1',
  port = process.env.PORT || 3000,
  bodyParser = require('body-parser');

app.use(bodyParser.urlencoded({ extended: true }));
app.use(bodyParser.json());

var routes = require('./api/routes/betStoreRoutes'); //importing route
routes(app); //register the route

app.listen(port, host);

console.log('quickview RESTful API server started on port: ' + port);

app.use(function(req, res) {
  res.status(404).send({url: req.originalUrl + ' not found'})
});