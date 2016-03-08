#FROM     java:8u66
FROM    java:openjdk-8-jre

EXPOSE 8080
EXPOSE 7474

ADD ./maven /mesh
WORKDIR /mesh
CMD [ "java", "-jar" , "mesh.jar" ]
