FROM    java:openjdk-8-jre

RUN apt-get update --quiet --quiet \
    && apt-get install --quiet --quiet --no-install-recommends lsof \
    && rm -rf /var/lib/apt/lists/*

EXPOSE 8080 7474

RUN mkdir /mesh
WORKDIR /mesh
ADD ./target/mesh-server*jar /mesh/mesh.jar


RUN mkdir /data
RUN ln -s /data /mesh/data
VOLUME /data

CMD [ "java", "-jar" , "mesh.jar" ]
