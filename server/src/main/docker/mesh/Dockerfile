FROM    java:openjdk-8-jre

RUN apt-get update --quiet --quiet \
    && apt-get install --quiet --quiet --no-install-recommends lsof \
    && rm -rf /var/lib/apt/lists/*

EXPOSE 8080 7474

ADD ./maven /mesh
WORKDIR /mesh

RUN mkdir /data
RUN ln -s /mesh/data /data
VOLUME /data

CMD [ "java", "-jar" , "mesh.jar" ]
