var console = require("vertx-js/util/console");

exports.vertxStart = function() {
	console.log('Start');
	//TODO test setup of http server .. routes .. 
}

exports.vertxStop = function() {
	console.log('Stop');
}