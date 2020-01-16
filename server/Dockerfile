FROM adoptopenjdk/openjdk11:x86_64-alpine-jre-11.0.3_7

ENV ESVERSION 6.8.0
ENV MESH_AUTH_KEYSTORE_PATH=/keystore/keystore.jks
ENV MESH_GRAPH_BACKUP_DIRECTORY=/backups
ENV MESH_GRAPH_DB_DIRECTORY=/graphdb
ENV MESH_PLUGIN_DIR=/plugins
ENV MESH_BINARY_DIR=/uploads
ENV JAVA_TOOL_OPTIONS="-Xms512m -Xmx512m -XX:MaxDirectMemorySize=256m -Dstorage.diskCache.bufferSize=256"

EXPOSE 8080
EXPOSE 8081

RUN adduser -D -u 1000 -G root -h /mesh mesh
USER mesh
WORKDIR /mesh
ADD ./target/mesh-server*jar /mesh/mesh.jar

USER root
RUN mkdir /graphdb   && chown mesh:root /graphdb  -R && chmod 770 /graphdb \
    mkdir /uploads   && chown mesh:root /uploads  -R && chmod 770 /uploads \
    mkdir /backups   && chown mesh:root /backups  -R && chmod 770 /backups \
    mkdir /plugins   && chown mesh:root /plugins  -R && chmod 770 /plugins \
    mkdir /keystore  && chown mesh:root /keystore -R && chmod 770 /keystore \
    mkdir /config    && chown mesh:root /config   -R && chmod 770 /config && ln -s /config /mesh/config && \
    mkdir /mesh/data && chown mesh:root /mesh     -R && chmod 770 /mesh -R

ADD https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-oss-$ESVERSION.tar.gz /es.tar.gz

WORKDIR /
RUN apk --update --no-cache add tar \
    && tar xfz /es.tar.gz && apk del tar \
    && mv elasticsearch* elasticsearch \
    && rm /es.tar.gz \
    && ln -s /elasticsearch /mesh/elasticsearch \
    && mkdir -p /elasticsearch/data \
    && chown mesh:root /elasticsearch -R
    && chmod 770 /elasticsearch

USER mesh
WORKDIR /mesh

VOLUME /graphdb
VOLUME /uploads
VOLUME /backups
VOLUME /plugins
VOLUME /keystore
VOLUME /config

VOLUME /elasticsearch/data
VOLUME /elasticsearch/config

CMD [ "java", "-jar" , "mesh.jar" ]

