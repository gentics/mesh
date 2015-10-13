# Mesh Website

## Build Steps

* Clone mesh github wiki
* Transform adoc to swig source files (via maven)
* Generate RAML Examples
* Generate Search JSON Examples
* Build Site (gulp)
  * build RAML
  * build docs (transform swig files to html)
  * build site

* Build registry.gentics.com/mesh-website docker image

## Development

### RAML/Site/Docs

Use ```gulp watch`` to start a server on [http://localhost:4000](http://localhost:4000) - The gulp task will automatically update the website and RAML and docs when changes are detected.

Open [wiki.raw](http://localhost:4000/wiki.raw) to access the raw adoc files.

You can directly view the adoc files in your browser by installing [Asciidoctor.js Live Preview](https://chrome.google.com/webstore/detail/asciidoctorjs-live-previe/iaalpfgpbocpdfblpnhhgllgbdbchmia?hl=en)

### AsciiDocs to swig templates 

Use ```mvn org.asciidoctor:asciidoctor-maven-plugin:process-asciidoc@process-asciidocs``` to invoke asciidoc transformation.

