# Multi-stage build for production-ready Java Spring Boot application
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Install Maven
RUN apk add --no-cache maven

# Copy pom.xml first for dependency caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install FFmpeg and FFprobe
RUN apk add --no-cache     ffmpeg     ffprobe     fontconfig     freetype     libass     ca-certificates     tzdata

# Create app directories
RUN mkdir -p /tmp/minitoon/temp /tmp/minitoon/output /app/logs

# Copy built JAR
COPY --from=builder /app/target/*.jar app.jar

# Environment variables (override at runtime)
ENV PORT=8080
ENV JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV SPRING_PROFILES_ACTIVE=prod

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3     CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/status/health || exit 1

# Expose port
EXPOSE 8080

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
