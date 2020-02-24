FROM    java:openjdk-8-jre-alpine

USER root
RUN mkdir /mesh
RUN adduser -D -u %UID% -h /mesh mesh
RUN chown mesh: /mesh
RUN echo "mesh:mesh" | chpasswd

USER mesh
WORKDIR /mesh
ADD bin /mesh/bin

USER root
RUN mkdir /data && mkdir /config && ln -s /data /mesh/data && chown mesh: /data 
ADD mesh.yml /config/mesh.yml
RUN chown mesh: /config -R && ln -s /config /mesh/config

USER mesh
VOLUME /data
VOLUME /config

USER mesh
EXPOSE 8080
CMD %CMD% 
