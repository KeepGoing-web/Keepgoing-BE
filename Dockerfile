FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace

# Pre-built JARs from CI (optional — .docker/ always exists with at least .gitkeep)
COPY .docker/ /workspace/.docker/

COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle

COPY keepgoing-api ./keepgoing-api
COPY keepgoing-worker ./keepgoing-worker
COPY keepgoing-common ./keepgoing-common

RUN chmod +x gradlew

RUN if [ -f /workspace/.docker/api.jar ] && [ -f /workspace/.docker/worker.jar ]; then \
      cp /workspace/.docker/api.jar /workspace/api.jar && \
      cp /workspace/.docker/worker.jar /workspace/worker.jar && \
      echo "Using pre-built JARs from CI"; \
    else \
      ./gradlew :keepgoing-api:bootJar :keepgoing-worker:bootJar --no-daemon && \
      cp /workspace/keepgoing-api/build/libs/*.jar /workspace/api.jar && \
      cp /workspace/keepgoing-worker/build/libs/*.jar /workspace/worker.jar && \
      echo "Built JARs from source"; \
    fi


# ---- API runtime ----
FROM eclipse-temurin:21-jre-jammy AS api-runtime

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system appuser \
    && useradd --system --gid appuser --create-home --home-dir /app appuser

WORKDIR /app

COPY --from=builder /workspace/api.jar /app/app.jar

ENV TZ=Asia/Seoul \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xms256m -XX:MaxRAMPercentage=70.0 -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

EXPOSE 8080

USER appuser

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]


# ---- Worker runtime ----
FROM eclipse-temurin:21-jre-jammy AS worker-runtime

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system appuser \
    && useradd --system --gid appuser --create-home --home-dir /app appuser

WORKDIR /app

COPY --from=builder /workspace/worker.jar /app/app.jar

ENV TZ=Asia/Seoul \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xms128m -XX:MaxRAMPercentage=65.0 -XX:+UseG1GC"

EXPOSE 8081

USER appuser

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
