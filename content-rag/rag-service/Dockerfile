FROM eclipse-temurin:17-jre-alpine AS eclipse-temurin-upgraded

RUN apk upgrade --no-cache

FROM eclipse-temurin-upgraded

ARG VERSION=dev

LABEL org.opencontainers.image.title="mesh-content-rag" \
      org.opencontainers.image.description="Retrieval-Augmented-Generation service over Gentics Mesh content" \
      org.opencontainers.image.vendor="Gentics" \
      org.opencontainers.image.version="${VERSION}"

ARG APP_UID=10001
ARG APP_GID=10001
RUN addgroup -S -g ${APP_GID} app \
 && adduser  -S -u ${APP_UID} -G app \
             -h /app -s /sbin/nologin app

WORKDIR /app

COPY --chown=app:app target/mesh-content-rag-service.jar /app/app.jar

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError" \
    RAG_VERSION=${VERSION} \
    MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED=true

USER app

EXPOSE 8090

HEALTHCHECK --start-period=30s --interval=15s --timeout=5s --retries=3 \
  CMD wget -q -O- http://localhost:8090/actuator/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar \"$@\"", "--"]
