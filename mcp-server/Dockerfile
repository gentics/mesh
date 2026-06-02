# syntax=docker/dockerfile:1.6
# ─────────────────────────────────────────────────────────────────────────────
# Gentics Mesh MCP Server – Multi-Stage Build
#
# Stage 1 (build):   Maven + JDK 21 → Fat-JAR via maven-shade-plugin
# Stage 2 (runtime): Slim JRE 21 → führt die JAR als non-root user aus
#
# Build (Standalone-Test ohne mesh-enterprise-Parent funktioniert nur, wenn
# der Modul-POM auf <parent> verzichtet – im Reactor-Kontext baut Jenkins
# die JAR vorher und reicht sie per `mvn install` weiter):
#   docker build --build-arg VERSION=$(mvn -q help:evaluate \
#     -Dexpression=project.version -DforceStdout) -t mesh-mcp-server:dev .
#
# Run (stdio):
#   docker run -i --rm \
#     -e MESH_BASE_URL=http://mesh:8080 \
#     -e MESH_USERNAME=admin -e MESH_PASSWORD=admin \
#     -e MESH_ALLOWED_PROJECTS=demo \
#     repo.gentics.com/mesh/mcp-server:<version>
# ─────────────────────────────────────────────────────────────────────────────

ARG VERSION=dev

# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# Dependencies zuerst (besseres Layer-Caching)
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q dependency:go-offline || true

# Quellen kopieren und bauen
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q clean package -DskipTests

# Sanity-Check: die geshadete Fat-JAR muss existieren
RUN test -f target/mesh-mcp-server.jar


# ── Stage 2: Runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy AS runtime

ARG VERSION

# OCI Labels
LABEL org.opencontainers.image.title="mesh-mcp-server" \
      org.opencontainers.image.description="MCP adapter for Gentics Mesh CMS" \
      org.opencontainers.image.vendor="Gentics" \
      org.opencontainers.image.version="${VERSION}"

# Non-root user
ARG APP_UID=10001
ARG APP_GID=10001
RUN groupadd --system --gid ${APP_GID} app \
 && useradd  --system --uid ${APP_UID} --gid ${APP_GID} \
             --home-dir /app --shell /sbin/nologin app

WORKDIR /app

# JAR aus Build-Stage übernehmen
COPY --from=build --chown=app:app /workspace/target/mesh-mcp-server.jar /app/app.jar

# JVM-Defaults – per K8s via JAVA_OPTS überschreibbar
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError" \
    LOG_LEVEL=info \
    MCP_VERSION=${VERSION}

USER app

# stdio: kein EXPOSE, kein Init-Wrapper (würde stdio-Pipes brechen).
# Im HTTP-Modus liest das Helm-Chart von mesh-enterprise MCP_HTTP_PORT.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar \"$@\"", "--"]
