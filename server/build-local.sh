#!/bin/bash

# Developer script which can be used to create docker image using the local compiled files. No maven recompilation or bundeling will ocurre.

mkdir -p target/local/bin

rsync -a --exclude=target/local/bin --include=*/ --include='*/classes/**' --include='*/mavendependencies-sharedlibs/*' --exclude=* --prune-empty-dirs .. target/local/bin
rm -rf target/local/bin/server/target/local
cp Dockerfile.local target/local/Dockerfile

cd target/local
CLASSPATH=""
for cl in $(find bin  -name "classes") ; do 
  CLASSPATH="$CLASSPATH:$cl"
done

for lib in $(find bin/server/target/mavendependencies-sharedlibs) ; do 
 CLASSPATH="$CLASSPATH:$lib"
done

CLASSPATH=$(echo $CLASSPATH | cut -c 2-)

echo "#!/bin/sh" > run.sh
echo "java -cp $CLASSPATH com.gentics.mesh.server.ServerRunner" >> run.sh
chmod +x run.sh

docker build -t mesh-local .

