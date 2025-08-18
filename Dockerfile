# Cloudtype 배포를 위한 Dockerfile
FROM openjdk:17-jdk-slim

# 시스템 패키지 업데이트 및 필수 의존성 설치
RUN apt-get update && apt-get install -y \
    wget \
    gnupg \
    ca-certificates \
    fonts-liberation \
    libasound2 \
    libatk-bridge2.0-0 \
    libatk1.0-0 \
    libatspi2.0-0 \
    libcups2 \
    libdrm2 \
    libgtk-3-0 \
    libnspr4 \
    libnss3 \
    libxcomposite1 \
    libxdamage1 \
    libxfixes3 \
    libxrandr2 \
    libxss1 \
    libxtst6 \
    xdg-utils \
    libgbm1 \
    libxkbcommon0 \
    && rm -rf /var/lib/apt/lists/*

# 작업 디렉토리 설정
WORKDIR /app

# Maven wrapper와 pom.xml 복사
COPY mvnw .
COPY mvnw.cmd .
COPY pom.xml .
COPY .mvn .mvn

# 의존성 다운로드 (캐시 최적화)
RUN ./mvnw dependency:resolve

# 소스 코드 복사
COPY src src

# 애플리케이션 빌드
RUN ./mvnw clean package -DskipTests

# Java Playwright용 브라우저 설치
# 빌드된 JAR에서 Playwright 브라우저를 설치
RUN java -cp target/getprice-0.0.1-SNAPSHOT.jar com.microsoft.playwright.CLI install chromium --with-deps || true

# Playwright 환경 변수 설정
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=false

# 애플리케이션 실행
EXPOSE 8080
CMD ["java", "-jar", "target/getprice-0.0.1-SNAPSHOT.jar"]
