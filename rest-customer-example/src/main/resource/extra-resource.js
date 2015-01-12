(function() {
	var Vertx = require("vertx-js/vertx");
//	var CaiLunResourceInfo = Java.type("com.gentics.vertx.cailun.starter.CaiLunResourceInfo")
//	var myResource = new CaiLunResourceInfo("helloworld2");

	var cailun = require('cailun');
	var app = cailun();

	app.get('/hellonubsi', function (req, res) {
	  res.send('Hello World!')
	});
	return app;
}());