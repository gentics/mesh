# Mesh Website

## Build Steps

* Clone mesh github wiki
* Transform adoc to HTML
* Generate RAML Examples
* Generate Search JSON Examples
* Build Site (gulp)
  * build RAML
  * build site

* Build registry.gentics.com/mesh-website docker image


## Development

* RAML/Site

Use ```gulp watch`` to start a server on localhost:4000 - The gulp task will automatically update the website and raml when changes are detected.

* AsciiDocs

Use ```mvn org.asciidoctor:asciidoctor-maven-plugin:process-asciidoc@process-asciidocs``` to invoke asciidoc transformation.