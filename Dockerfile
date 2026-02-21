# ---------- Stage 1: Build ----------
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy poms first for layer caching
COPY pom.xml .
COPY rss-service/pom.xml rss-service/
RUN mvn -B -ntp dependency:go-offline

# Copy source and build
COPY rss-service/src rss-service/src
RUN mvn -pl rss-service -am clean package -DskipTests


# ---------- Stage 2: Runtime ----------
# This JRE is ~160MB total (much smaller than the 400MB+ Jammy version)
# It includes all standard Java modules like HTTP, XML, and SQL.
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install curl for healthcheck and gcompat for Lucene's performance
RUN apk add --no-cache curl gcompat

# Copy the fat jar from the build stage
COPY --from=build /app/rss-service/target/*.jar app.jar

# Setup Lucene data directory and non-root user
RUN mkdir -p /data/lucene-index && \
    addgroup -S rssgroup && adduser -S rssuser -G rssgroup && \
    chown -R rssuser:rssgroup /data/lucene-index

USER rssuser
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

# Tracing pinned threads is low-effort/high-value for monitoring your Sync issue
ENTRYPOINT ["java", "-XX:+UseG1GC", "-Xms512m", "-Xmx1g", "-Djdk.tracePinnedThreads=short", "-jar", "app.jar"]