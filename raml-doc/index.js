var raml2html = require('raml2html');
var mkdirp = require('mkdirp');
var fs = require('fs');
var outputDir = "target/raml2html/";
var config = raml2html.getDefaultConfig(false);

mkdirp(outputDir, function(err) { 
	if(err) {
		console.log(err);
		process.exit(1); 
	} else {
		console.log("Create output directory " + outputDir);
	}});


var source = "src/raml/rest-spec-core.raml";
var onSuccess = function(html) {
	console.log("OK");
	fs.writeFile(outputDir + "index.html", html, function(err) {
		if(err) {
			console.log(err);
			process.exit(1); 
		} else {
			console.log("The file was saved!");
		}
	});
};

var onError = function(e) {
	console.log("Error: " + e.message);
	process.exit(10); 
}

var buildSource = "target/raml2html/spec.raml";
fs.createReadStream(source).pipe(fs.createWriteStream(buildSource));
raml2html.render(buildSource, config, onSuccess, onError);