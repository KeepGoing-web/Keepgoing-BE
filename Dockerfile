FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace

COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle

COPY keepgoing-api ./keepgoing-api
COPY keepgoing-worker ./keepgoing-worker
COPY keepgoing-common ./keepgoing-common

RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

RUN ./gradlew :keepgoing-api:bootJar --no-daemon \
    && cp /workspace/keepgoing-api/build/libs/*.jar /workspace/app.jar

FROM eclipse-temurin:21-jre-jammy AS runtime

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system appuser \
    && useradd --system --gid appuser --create-home --home-dir /app appuser

WORKDIR /app

COPY --from=builder /workspace/app.jar /app/app.jar

ENV TZ=Asia/Seoul \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xms256m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

EXPOSE 8080

USER appuser

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
