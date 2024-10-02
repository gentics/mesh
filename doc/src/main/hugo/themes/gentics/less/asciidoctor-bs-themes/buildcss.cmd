@echo off

set styles=default default_themed amelia cerulean cosmo cyborg darkly flatly journal lumen united readable simplex slate spacelab superhero yeti

FOR %%A IN (%styles%) DO (
   echo creating css\bootstrap_%%A.css
   call lessc --global-var="bsw-root='https://raw.githubusercontent.com/thomaspark/bootswatch/gh-pages'" src\less\styles\%%A\styles.less css\bootstrap_%%A.css
   call cleancss -o css\bootstrap_%%A.min.css css\bootstrap_%%A.css
)
