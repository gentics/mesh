var raml2html = require('raml2html');
var mkdirp = require('mkdirp');
var fs = require('fs');

var sourceDir = __dirname + "/src/raml/";
var outputDir = __dirname + "/target/raml2html/";

var source = sourceDir + "rest-spec-core.raml";
var templateFile = sourceDir + "template.handlebars";
var resourceTemplate = sourceDir + "resource.handlebars";
var buildSource = outputDir + "spec.raml";

mkdirp(outputDir, function(err) {
	if (err) {
		console.log(err);
		process.exit(1);
	} else {
		console.log("Create output directory " + outputDir);
	}
});

var onSuccess = function(html) {
	var outputFile = outputDir + "index.html";
	console.log("Rendering completed. Writing output to {" + outputFile + "}");
	fs.writeFile(outputFile, html, function(err) {
		if (err) {
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

var config = raml2html.getDefaultConfig(false);

// TODO cleanup this code
var resourceTemplateSource = "";
console.log("Reading resource template file {" + resourceTemplate + "}");
fs.readFile(resourceTemplate, 'utf8', function(err, data) {
	resourceTemplateSource = data;
});

console.log("Reading template file {" + templateFile + "}");
fs.readFile(templateFile, 'utf8', function(err, data) {
	config['template'] = data;
	config['partials']['resource'] = resourceTemplateSource;
	fs.createReadStream(source).pipe(fs.createWriteStream(buildSource));
	raml2html.render(buildSource, config, onSuccess, onError);
});
