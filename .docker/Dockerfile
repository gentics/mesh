ARG MESH_REGISTRY_HOST=docker.gentics.com
ARG MESH_VERSION
ARG OTEL_JAVAAGENT_VERSION=2.27.0

FROM maven:3.8-eclipse-temurin-17 AS build-env

ARG MVN_USERNAME
ARG MVN_PASSWORD

ARG MESH_CONNECTOR
ARG MESH_ARTIFACTS_REPO=maven-snapshots

ENV GENTICS_REPO_USER $MVN_USERNAME
ENV GENTICS_REPO_PASS $MVN_PASSWORD

RUN mkdir -p /builder ~/.m2
COPY settings.xml /root/.m2/settings.xml
COPY *mesh-connector.jar /builder/
WORKDIR /builder

RUN if [ -n "$MESH_CONNECTOR" ] ; \
    then \
        mvn dependency:get --batch-mode -Dartifact=${MESH_CONNECTOR} -Ddest=/builder/mesh-connector.jar -DoverWrite=true -Dtransitive=false -DremoteRepositories=gentics.nexus.snapshots::default::https://repo.gentics.com/repository/${MESH_ARTIFACTS_REPO}  ; \
    fi

RUN mvn dependency:get --batch-mode \
      -Dartifact=io.opentelemetry.javaagent:opentelemetry-javaagent:${OTEL_JAVAAGENT_VERSION} \
      -Ddest=/builder/opentelemetry-javaagent.jar \
      -DoverWrite=true -Dtransitive=false

FROM $MESH_REGISTRY_HOST/gentics/mesh-enterprise:$MESH_VERSION

# Revoke the root permissions again
USER mesh

COPY --chown=mesh:root --from=build-env /builder/mesh-connector.jar /mesh/database/mesh-connector.jar
COPY --chown=mesh:root --from=build-env /builder/opentelemetry-javaagent.jar /mesh/opentelemetry-javaagent.jar

ENV MESH_DB_CONNECTOR_CLASSPATH /mesh/database

ENTRYPOINT ["/mesh/entrypoint.sh"]
