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

RUN adduser -D -u 1000 -h /mesh mesh
USER mesh
WORKDIR /mesh
ADD ./target/mesh-server*jar /mesh/mesh.jar

USER root
RUN mkdir /graphdb   && chown mesh: /graphdb    -R && \
    mkdir /uploads   && chown mesh: /uploads    -R && \
    mkdir /backups   && chown mesh: /backups    -R && \
    mkdir /plugins   && chown mesh: /plugins    -R && \
    mkdir /keystore  && chown mesh: /keystore   -R && \
    mkdir /config    && chown mesh: /config     -R && ln -s /config /mesh/config && \
    mkdir /mesh/data && chown mesh: /mesh/data  -R

ADD https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-oss-$ESVERSION.tar.gz /es.tar.gz

WORKDIR /
RUN apk --update --no-cache add tar \
    && tar xfz /es.tar.gz && apk del tar \
    && mv elasticsearch* elasticsearch \
    && rm /es.tar.gz \
    && ln -s /elasticsearch /mesh/elasticsearch \
    && mkdir -p /elasticsearch/data \
    && chown mesh: /elasticsearch -R

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

