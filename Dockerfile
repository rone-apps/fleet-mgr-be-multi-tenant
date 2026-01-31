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

# ===== JVM Tuning =====
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 \
 -XX:InitialRAMPercentage=50 \
 -XX:+UseG1GC \
 -XX:MaxGCPauseMillis=200 \
 -XX:+ExitOnOutOfMemoryError"

# ===== New Relic Configuration =====
ENV NEW_RELIC_APP_NAME="FareFlow Backend"
ENV NEW_RELIC_DISTRIBUTED_TRACING_ENABLED=true

# ===== Extra JVM Args (Optional) =====
ENV JAVA_OPTS=""

# ===== Required Environment Variables (Pass via docker-compose or -e flags) =====
# Database:
#   - MYSQL_ROOT_PASSWORD (required)
#   - SPRING_DATASOURCE_PASSWORD (required, same as MYSQL_ROOT_PASSWORD)
#
# Email (Gmail SMTP):
#   - SPRING_MAIL_USERNAME (required, e.g., pooni.harjot@gmail.com)
#   - SPRING_MAIL_PASSWORD (required, Gmail App Password)
#   - SPRING_MAIL_SENDER_NAME (optional, defaults to "FareFlow - Yellow Cabs Newyork")
#
# JWT:
#   - JWT_SECRET (optional, defaults to embedded secret)
#   - JWT_EXPIRATION (optional, defaults to 86400000 ms = 24 hours)

# IMPORTANT: javaagent must come BEFORE -jar
ENTRYPOINT ["sh", "-c", "\
java \
-javaagent:/app/newrelic/newrelic.jar \
-Dnewrelic.config.file=/app/newrelic/newrelic.yml \
$JAVA_OPTS \
-jar /app/app.jar \
"]

