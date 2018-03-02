FROM    openjdk:10-slim

ENV ESVERSION 6.1.2

EXPOSE 8080

RUN adduser -D -u 1000 -h /mesh mesh
USER mesh
WORKDIR /mesh
ADD ./target/mesh-demo*jar /mesh/meshdemo.jar

USER root
RUN mkdir /data   && chown mesh: /data -R   && ln -s /data /mesh/data
RUN mkdir /config && chown mesh: /config -R && ln -s /config /mesh/config

ADD https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-$ESVERSION.zip /es.zip

WORKDIR /
RUN apk --update --no-cache add unzip \
    && unzip /es.zip && apk del unzip \
    && mv elasticsearch* elasticsearch \
    && rm /es.zip \
    && ln -s /elasticsearch /mesh/elasticsearch \
    && mkdir -p /elasticsearch/data \
    && chown mesh: /elasticsearch -R

USER mesh
WORKDIR /mesh
VOLUME /data
VOLUME /config
VOLUME /elasticsearch/data
VOLUME /elasticsearch/config

CMD [ "java", "-jar" , "meshdemo.jar" ]
