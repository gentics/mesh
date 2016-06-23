FROM    java:openjdk-8-jre-alpine

EXPOSE 8080

RUN mkdir /mesh
WORKDIR /mesh
ADD ./target/mesh-server*jar /mesh/mesh.jar


RUN mkdir /data
RUN ln -s /data /mesh/data
VOLUME /data

CMD [ "java", "-jar" , "mesh.jar" ]
