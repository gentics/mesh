#!/bin/sh

. /__cacert_entrypoint.sh

java -Djna.tmpdir=/tmp/.jna -Duser.dir=/mesh --add-opens java.base/java.util=ALL-UNNAMED -jar mesh.jar
