# Multi-stage Dockerfile for Playwright Java application

# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy Maven files
COPY mvnw .
COPY mvnw.cmd .
COPY pom.xml .
COPY .mvn .mvn

# Download dependencies (for caching)
RUN ./mvnw dependency:resolve

# Copy source code
COPY src src

# Build application
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime stage - Use Ubuntu with JDK 17
FROM ubuntu:22.04

# Install JDK 17, Node.js and required packages
RUN apt-get update && \
    apt-get install -y openjdk-17-jdk curl wget gnupg && \
    curl -fsSL https://deb.nodesource.com/setup_18.x | bash - && \
    apt-get install -y nodejs && \
    # Install additional dependencies for Playwright
    apt-get install -y \
        libnss3 \
        libnspr4 \
        libatk-bridge2.0-0 \
        libdrm2 \
        libxkbcommon0 \
        libgtk-3-0 \
        libgbm1 \
        libasound2 && \
    # Clean up
    rm -rf /var/lib/apt/lists/*

# Set JAVA_HOME
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH="$JAVA_HOME/bin:$PATH"

# Install Playwright CLI and browsers
RUN npm install -g playwright@1.45.0 && \
    playwright install chromium --with-deps

# Set working directory
WORKDIR /app

# Set Playwright environment variables
ENV PLAYWRIGHT_BROWSERS_PATH=/root/.cache/ms-playwright
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=false

# Copy the built JAR from build stage
COPY --from=build /app/target/getprice-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]
