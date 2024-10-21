@echo off

set styles=default default_themed amelia cerulean cosmo cyborg darkly flatly journal lumen united readable simplex slate spacelab superhero yeti

IF NOT EXIST output MD output

FOR %%A IN (%styles%) DO (
	rem asciidoctor -T ..\src\templates -a source-highlighter="coderay" -a linkcss  -a icons="font" -a stylesheet="../../css/bootstrap_%%A.css" -o output\test_%%A.html test.adoc
	
	asciidoctor -a stylesheet="../css/bootstrap_%%A.css" -o output\test_%%A.html test.adoc
)
