################################
# Build stage
################################
FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace

COPY gradlew build.gradle ./
COPY gradle ./gradle

RUN chmod +x ./gradlew
RUN ./gradlew --no-daemon dependencies

COPY src ./src

RUN ./gradlew --no-daemon clean bootJar

# Copy fat jar (exclude *plain.jar if present)
RUN cp build/libs/*[^p].jar /workspace/app.jar || cp build/libs/*.jar /workspace/app.jar


################################
# Runtime stage
################################
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# --- Application ---
COPY --from=build /workspace/app.jar ./app.jar

# --- New Relic Agent ---
RUN mkdir -p /app/newrelic \
 && curl -L https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic.jar \
    -o /app/newrelic/newrelic.jar

# --- Optional: NR config (recommended) ---
COPY newrelic.yml /app/newrelic/newrelic.yml

EXPOSE 8080

# JVM tuning (keep yours)
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 \
 -XX:InitialRAMPercentage=50 \
 -XX:+UseG1GC \
 -XX:MaxGCPauseMillis=200 \
 -XX:+ExitOnOutOfMemoryError"

# New Relic config via env
ENV NEW_RELIC_APP_NAME="FareFlow Backend"
ENV NEW_RELIC_DISTRIBUTED_TRACING_ENABLED=true

# Extra JVM args hook (optional)
ENV JAVA_OPTS=""

# IMPORTANT: javaagent must come BEFORE -jar
ENTRYPOINT ["sh", "-c", "\
java \
-javaagent:/app/newrelic/newrelic.jar \
-Dnewrelic.config.file=/app/newrelic/newrelic.yml \
$JAVA_OPTS \
-jar /app/app.jar \
"]

