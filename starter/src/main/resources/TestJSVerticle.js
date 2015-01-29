var console = require("vertx-js/util/console");
var Router = require("vertx-apex-core-js/router");

exports.vertxStart = function() {
	console.log('Start');

	var router = Router.router(vertx);
	var route = router.route().path("/test").method("GET").handler(
			function(rc) {
				rc.response().end("yippiya");
			});

	var options = {
		'port' : 8081
	};

	var server = vertx.createHttpServer(options).requestHandler(router.accept);
	server.listen();
}

exports.vertxStop = function() {
	console.log('Stop');
}