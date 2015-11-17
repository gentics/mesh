FROM     java:8u66
MAINTAINER Johannes Schüth "j.schueth@gentics.com"

EXPOSE 8080
EXPOSE 7474

ADD ./maven /mesh
WORKDIR /mesh
CMD [ "java", "-jar" , "meshdemo.jar" ]
