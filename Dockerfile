# ---------- Stage 1: Build ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy parent + module poms first (better caching)
COPY pom.xml .
COPY rss-service/pom.xml rss-service/

# Download dependencies
RUN mvn -B -ntp dependency:go-offline

# Copy source
COPY rss-service/src rss-service/src

# Build only module
RUN mvn -pl rss-service -am clean package -DskipTests


# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Install curl for healthcheck
RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

COPY --from=build /app/rss-service/target/*.jar app.jar

RUN mkdir -p /data/lucene-index

RUN useradd -m rssuser
USER rssuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

ENTRYPOINT ["java", "-XX:+UseG1GC", "-Xms512m", "-Xmx1g", "-jar", "app.jar"]
