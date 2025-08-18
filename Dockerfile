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

# Stage 2: Runtime stage - Use Ubuntu base image instead of Playwright image
FROM ubuntu:22.04

# Install Java 17 and Node.js for Playwright
RUN apt-get update && apt-get install -y \
    openjdk-17-jre-headless \
    curl \
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
    libnss3 \
    libxkbcommon0 \
    libgtk-3-0 \
    libgdk-pixbuf2.0-0 \
    libxfixes3 \
    libxext6 \
    libx11-6 \
    libxrender1 \
    libxi6 \
    libxcomposite1 \
    libxcursor1 \
    libxtst6 \
    && rm -rf /var/lib/apt/lists/*

# Install Node.js 18.x (required for Playwright)
RUN curl -fsSL https://deb.nodesource.com/setup_18.x | bash - && \
    apt-get install -y nodejs

# Set JAVA_HOME
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

WORKDIR /app

# Install Playwright CLI
RUN npm install -g playwright@1.45.0

# Install Playwright browsers
RUN playwright install chromium --with-deps

# Set Playwright environment variables
ENV PLAYWRIGHT_BROWSERS_PATH=/root/.cache/ms-playwright
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=true

# Copy the built JAR from build stage
COPY --from=build /app/target/getprice-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]
