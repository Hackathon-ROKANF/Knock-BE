# Multi-stage Dockerfile for Playwright Java application

# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy Maven wrapper and pom.xml
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

# Stage 2: Runtime stage - Use official Playwright Java image
FROM mcr.microsoft.com/playwright/java:v1.45.0-jammy

# Set working directory
WORKDIR /app

# Install Node.js and Playwright CLI (for system installation)
RUN apt-get update && apt-get install -y curl && \
    curl -fsSL https://deb.nodesource.com/setup_18.x | bash - && \
    apt-get install -y nodejs && \
    npm install -g playwright@1.45.0 && \
    playwright install chromium --with-deps && \
    rm -rf /var/lib/apt/lists/*

# Set Playwright environment variables
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=false
ENV NODE_PATH=/usr/local/lib/node_modules
ENV PATH=/usr/local/bin:$PATH

# Copy the built JAR from build stage
COPY --from=build /app/target/getprice-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]
