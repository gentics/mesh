# ─────────────────────────────────────────────────────────────────────────────
# Gentics Mesh MCP Server – Runtime Image
#
# Die Fat-JAR wird NICHT mehr hier gebaut, sondern vom Maven-Reactor erzeugt
# (Stage "Maven Build" in Jenkinsfile.split) und hier nur noch übernommen.
# Voraussetzung: target/mesh-mcp-server.jar existiert bereits.
#
# Build (lokal):
#   mvn -pl mcp-server -am package -DskipTests
#   docker build --build-arg VERSION=<version> -t mesh-mcp-server:dev mcp-server
#
# Run (stdio):
#   docker run -i --rm \
#     -e MESH_BASE_URL=http://mesh:8080 \
#     -e MESH_USERNAME=admin -e MESH_PASSWORD=admin \
#     -e MESH_ALLOWED_PROJECTS=demo \
#     repo.gentics.com/mesh/mcp-server:<version>
# ─────────────────────────────────────────────────────────────────────────────

FROM eclipse-temurin:17-jre-alpine AS runtime

ARG VERSION=dev

# OCI Labels
LABEL org.opencontainers.image.title="mesh-mcp-server" \
      org.opencontainers.image.description="MCP adapter for Gentics Mesh CMS" \
      org.opencontainers.image.vendor="Gentics" \
      org.opencontainers.image.version="${VERSION}"

# Non-root user
ARG APP_UID=10001
ARG APP_GID=10001
RUN addgroup -S -g ${APP_GID} app \
 && adduser  -S -u ${APP_UID} -G app \
             -h /app -s /sbin/nologin app

WORKDIR /app

# Vorgebaute Fat-JAR aus dem Maven-Build übernehmen
COPY --chown=app:app target/mesh-mcp-server.jar /app/app.jar

# JVM-Defaults – per K8s via JAVA_OPTS überschreibbar
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError" \
    LOG_LEVEL=info \
    MCP_VERSION=${VERSION}

USER app

# stdio: kein EXPOSE, kein Init-Wrapper (würde stdio-Pipes brechen).
# Im HTTP-Modus liest das Helm-Chart von mesh-enterprise MCP_HTTP_PORT.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar \"$@\"", "--"]
