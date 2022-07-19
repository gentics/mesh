FROM adoptopenjdk/openjdk11:x86_64-alpine-jre-11.0.12_7

ENV MESH_AUTH_KEYSTORE_PATH=/keystore/keystore.jks
ENV MESH_GRAPH_BACKUP_DIRECTORY=/backups
ENV MESH_GRAPH_DB_DIRECTORY=/graphdb
ENV MESH_PLUGIN_DIR=/plugins
ENV MESH_BINARY_DIR=/uploads
ENV MESH_TEMP_DIR=/tmp
ENV HOME=/mesh
ENV JAVA_TOOL_OPTIONS="-Xms256m -Xmx256m -XX:MaxDirectMemorySize=128m -Dstorage.diskCache.bufferSize=128"

EXPOSE 8080
EXPOSE 8443
EXPOSE 8081

USER root
ADD ./target/mesh-demo*jar /mesh/meshdemo.jar
ADD ./target/dump/data/binaryFiles /uploads
ADD ./target/dump/data/graphdb /graphdb
RUN adduser -D -u 1000 -G root -h /mesh mesh && \
    mkdir -p /graphdb   && chown 1000:0 /graphdb  -R && chmod 770 /graphdb  && \
    mkdir -p /uploads   && chown 1000:0 /uploads  -R && chmod 770 /uploads  && \
    mkdir -p /backups   && chown 1000:0 /backups  -R && chmod 770 /backups  && \
    mkdir -p /plugins   && chown 1000:0 /plugins  -R && chmod 770 /plugins  && \
    mkdir -p /keystore  && chown 1000:0 /keystore -R && chmod 770 /keystore && \
    mkdir -p /config    && chown 1000:0 /config   -R && chmod 770 /config && ln -s /config /mesh/config && \
    mkdir -p /mesh/data && \
    mkdir -p /mesh/elasticsearch/data && \
    chown 1000:0 /mesh -R && \
    chmod 770 /mesh -R

USER mesh
WORKDIR /mesh
VOLUME /mesh/elasticsearch/data
VOLUME /graphdb
VOLUME /uploads
VOLUME /backups
VOLUME /plugins
VOLUME /keystore
VOLUME /config

VOLUME /elasticsearch/data
VOLUME /elasticsearch/config

CMD [ "java", "-Djna.tmpdir=/tmp/.jna", "-Duser.dir=/mesh", "-jar" , "meshdemo.jar"]
