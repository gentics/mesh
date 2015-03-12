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



var source   = "src/raml/rest-spec-core.raml";
var template = "src/raml/template.handlebars";
var resourceTemplate = "src/raml/resource.handlebars";
var buildSource = "target/raml2html/spec.raml";

//TODO cleanup this code
var resourceTemplateSource = ""; 
fs.readFile(resourceTemplate, 'utf8', function(err, data) {
  resourceTemplateSource = data;
});

fs.readFile(template, 'utf8', function(err, data) {
  config['template'] = data;
  config['partials']['resource'] = resourceTemplateSource;
  fs.createReadStream(source).pipe(fs.createWriteStream(buildSource));
  raml2html.render(buildSource, config, onSuccess, onError);
});

