FROM    java:openjdk-8-jre-alpine

EXPOSE 8080

RUN adduser -D -u 1000 -h /mesh mesh
USER mesh
WORKDIR /mesh
ADD ./target/mesh-server*jar /mesh/mesh.jar

USER root
RUN mkdir /data   && chown mesh: /data -R   && ln -s /data /mesh/data
RUN mkdir /config && chown mesh: /config -R && ln -s /config /mesh/config

USER mesh
VOLUME /data
VOLUME /config
CMD [ "java", "-jar" , "mesh.jar" ]
