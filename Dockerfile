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

# Playwright 브라우저 의존성을 위한 추가 패키지 설치
RUN apt-get update && apt-get install -y \
    libnss3-dev \
    libatk-bridge2.0-dev \
    libdrm2 \
    libxkbcommon0 \
    libgbm1 \
    libxss1 \
    libasound2 \
    && rm -rf /var/lib/apt/lists/*

# Playwright 환경 변수 설정 - 자동 다운로드 허용
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=false
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright

# 브라우저가 설치될 디렉토리 생성
RUN mkdir -p /ms-playwright

# 애플리케이션 실행
EXPOSE 8080
CMD ["java", "-jar", "target/getprice-0.0.1-SNAPSHOT.jar"]
