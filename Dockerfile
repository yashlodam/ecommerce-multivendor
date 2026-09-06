# ==============================================================================
# Multi-Stage Dockerfile for ShopSphere Spring Boot Application
# Target Deployment: Render (Docker Runtime)
# Base Runtime: Eclipse Temurin Java 21 Alpine
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build & Package the Application
# ------------------------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Pre-fetch Maven dependencies for efficient layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy application source code
COPY src ./src

# Compile and package Spring Boot application into executable JAR
RUN mvn clean package -DskipTests -B

# ------------------------------------------------------------------------------
# Stage 2: Minimal Production Runtime
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Install curl for health-check probes and ca-certificates for secure HTTPS outbound calls
RUN apk add --no-cache curl ca-certificates tzdata

# Security best practice: Run as a non-privileged user
RUN addgroup -S shopsphere && adduser -S shopsphere -G shopsphere

# Copy the built artifact from builder stage
COPY --from=builder /build/target/ecommerce_multivendor-*.jar app.jar

# Set file permissions
RUN chown -R shopsphere:shopsphere /app

USER shopsphere

# Expose default port (Render will dynamically supply PORT environment variable)
EXPOSE 8080

# JVM container-aware memory flags optimized for Render free/starter tier (512MB RAM)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

# Start the Spring Boot application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
