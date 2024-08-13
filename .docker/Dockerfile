ARG MESH_REGISTRY_HOST=docker.apa-it.at
ARG MESH_VERSION

FROM maven:3.8-eclipse-temurin-17 AS build-env

ARG MVN_USERNAME
ARG MVN_PASSWORD

ARG MESH_CONNECTOR
ARG MESH_ARTIFACTS_REPO=gtx-maven-public-snapshots

ENV GENTICS_REPO_USER $MVN_USERNAME
ENV GENTICS_REPO_PASS $MVN_PASSWORD

RUN mkdir -p /builder ~/.m2
COPY settings.xml /root/.m2/settings.xml
COPY *mesh-connector.jar /builder/
WORKDIR /builder

RUN if [ -n "$MESH_CONNECTOR" ] ; \
    then \
        mvn dependency:get --batch-mode -Dartifact=${MESH_CONNECTOR} -Ddest=/builder/mesh-connector.jar -DoverWrite=true -Dtransitive=false -DremoteRepositories=gtx-commercial::default::https://repo.apa-it.at/artifactory/${MESH_ARTIFACTS_REPO}  ; \
    fi

FROM $MESH_REGISTRY_HOST/gentics/mesh:$MESH_VERSION

# Revoke the root permissions again
USER mesh

COPY --chown=mesh:root --from=build-env /builder/mesh-connector.jar /mesh/database/mesh-connector.jar

ENV MESH_DB_CONNECTOR_CLASSPATH /mesh/database

CMD [ "java", "-Djna.tmpdir=/tmp/.jna", "-Duser.dir=/mesh", "--add-opens", "java.base/java.util=ALL-UNNAMED", "-jar" , "mesh.jar"]
