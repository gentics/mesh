FROM adoptopenjdk/openjdk11:x86_64-debian-jre-11.0.11_9

ENV MESH_AUTH_KEYSTORE_PATH=/keystore/keystore.jks
ENV MESH_GRAPH_BACKUP_DIRECTORY=/backups
ENV MESH_GRAPH_DB_DIRECTORY=/graphdb
ENV MESH_PLUGIN_DIR=/plugins
ENV MESH_BINARY_DIR=/uploads
ENV MESH_TEMP_DIR=/tmp
ENV HOME=/mesh
ENV JAVA_TOOL_OPTIONS="-Xms512m -Xmx512m -XX:MaxDirectMemorySize=256m -Dstorage.diskCache.bufferSize=256"

EXPOSE 8080
EXPOSE 8443
EXPOSE 8081

USER root
RUN adduser --disabled-password --uid 1000 --ingroup sudo --home /mesh --gecos "Mesh Mesh,0,0,0,0" mesh && \
    mkdir /graphdb   && chown 1000:0 /graphdb  -R && chmod 770 /graphdb  && \
    mkdir /uploads   && chown 1000:0 /uploads  -R && chmod 770 /uploads  && \
    mkdir /backups   && chown 1000:0 /backups  -R && chmod 770 /backups  && \
    mkdir /plugins   && chown 1000:0 /plugins  -R && chmod 770 /plugins  && \
    mkdir /keystore  && chown 1000:0 /keystore -R && chmod 770 /keystore && \
    mkdir /config    && chown 1000:0 /config   -R && chmod 770 /config && ln -s /config /mesh/config && \
    mkdir /mesh/data && \
    mkdir -p /mesh/elasticsearch/data && \
    chown 1000:0 /mesh -R && \
    chmod 770 /mesh -R
ADD --chown=1000:0 ./target/mesh-server*jar /mesh/mesh.jar

USER mesh
WORKDIR /mesh
VOLUME /mesh/elasticsearch/data
VOLUME /graphdb
VOLUME /uploads
VOLUME /backups
VOLUME /plugins
VOLUME /keystore
VOLUME /config

CMD [ "java", "-Djna.tmpdir=/tmp/.jna", "-Duser.dir=/mesh", "-jar" , "mesh.jar"]
