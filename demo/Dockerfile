FROM    java:openjdk-8-jre-alpine

EXPOSE 8080

RUN mkdir /mesh
WORKDIR /mesh
ADD ./target/mesh-demo*jar /mesh/meshdemo.jar


RUN mkdir /data
RUN ln -s /mesh/data /data
VOLUME /data

CMD [ "java", "-jar" , "meshdemo.jar" ]
