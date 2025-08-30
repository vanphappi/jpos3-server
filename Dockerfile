FROM eclipse-temurin:23-jdk-alpine AS builder

WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY gradle.properties .

# Copy source code
COPY src src
COPY cfg cfg
COPY deploy deploy

# Build application
RUN chmod +x gradlew && \
    ./gradlew clean build -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:23-jre-alpine

# Add security updates
RUN apk update && apk add --no-cache curl && \
    addgroup -g 1001 jpos && \
    adduser -u 1001 -G jpos -s /bin/sh -D jpos

WORKDIR /app

# Copy application
COPY --from=builder /app/build/libs/*.jar app.jar
COPY --from=builder /app/cfg cfg
COPY --from=builder /app/deploy deploy

# Create directories
RUN mkdir -p logs data && \
    chown -R jpos:jpos /app

USER jpos

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

EXPOSE 8080 8120

ENTRYPOINT ["java", \
    "--enable-preview", \
    "-Xmx2g", \
    "-XX:+UseZGC", \
    "-XX:+UnlockExperimentalVMOptions", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]