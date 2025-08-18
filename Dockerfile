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

# Stage 2: Runtime stage with Playwright
FROM mcr.microsoft.com/playwright/java:v1.45.0-jammy

WORKDIR /app

# Install additional dependencies for Playwright
RUN apt-get update && apt-get install -y \
    wget \
    ca-certificates \
    fonts-liberation \
    libasound2 \
    libatk-bridge2.0-0 \
    libdrm2 \
    libxcomposite1 \
    libxdamage1 \
    libxrandr2 \
    libgbm1 \
    libxss1 \
    libgconf-2-4 \
    && rm -rf /var/lib/apt/lists/*

# Set Playwright environment variables
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=false

# Copy the built JAR from build stage
COPY --from=build /app/target/getprice-0.0.1-SNAPSHOT.jar app.jar

# Install Playwright browsers explicitly
RUN playwright install chromium

# Expose port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]
