FROM    java:openjdk-8-jre-alpine

EXPOSE 8080

RUN adduser -D -u 1000 -h /mesh mesh
USER mesh
WORKDIR /mesh
ADD ./target/mesh-demo*jar /mesh/meshdemo.jar

USER root
RUN mkdir /data
RUN chown mesh: /data -R
RUN ln -s /data /mesh/data

USER mesh
VOLUME /data
CMD [ "java", "-jar" , "meshdemo.jar" ]
