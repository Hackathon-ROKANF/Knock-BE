# Cloudtype 배포 가이드

## 개요
이 프로젝트는 Cloudtype에서 Playwright를 사용한 웹 크롤링 서비스를 배포하기 위해 최적화되었습니다.

## Cloudtype 배포 설정

### 1. 환경변수 설정
Cloudtype 배포 시 다음 환경변수들을 설정하세요:

```bash
# Spring Boot 설정
SPRING_PROFILES_ACTIVE=prod
PORT=8080

# JVM 메모리 설정 (Cloudtype 플랜에 맞게 조정)
JAVA_OPTS=-Xmx512m -Xms256m -XX:MaxMetaspaceSize=128m

# Playwright 브라우저 추가 옵션 (선택사항)
PLAYWRIGHT_BROWSER_ARGS=--disable-dev-shm-usage,--no-sandbox,--disable-setuid-sandbox

# 로그 레벨
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_SPRINGBOOT_KNOCKBE=DEBUG
```

### 2. 빌드 설정
Cloudtype에서 다음 빌드 명령어를 사용하세요:

```bash
# 빌드 명령어
./mvnw clean package -DskipTests

# 실행 명령어
java $JAVA_OPTS -jar target/getprice-0.0.1-SNAPSHOT.jar
```

### 3. Docker 배포 (권장)
Dockerfile이 포함되어 있어 컨테이너 기반 배포가 가능합니다:

```bash
# Docker 빌드
docker build -t knock-be .

# Docker 실행
docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=prod knock-be
```

## 주요 최적화 사항

### Playwright 최적화
- Cloudtype 환경 자동 감지
- 메모리 제한 환경에 최적화된 브라우저 옵션
- 확장된 타임아웃 설정 (60초)
- 불필요한 리소스 비활성화 (이미지, JavaScript 등)

### 메모리 최적화
- JVM 힙 메모리 제한 설정
- Playwright 브라우저 프로세스 최적화
- 리소스 자동 정리

### 오류 처리
- 상세한 오류 로깅 및 진단
- 환경별 맞춤 오류 메시지
- Fallback 응답 제공

## API 엔드포인트

### 부동산 최저가 조회
```
GET /api/bds/lowest-price?address={주소}
```

예시:
```
GET /api/bds/lowest-price?address=신흥동3767
```

응답:
```json
{
  "address": "신흥동3767",
  "sourceUrl": "https://www.budongsan114.com/...",
  "saleLowestWon": 580000000,
  "jeonseLowestWon": 45000000,
  "wolseDepositLowestWon": null,
  "wolseMonthlyLowestWon": null
}
```

## 문제 해결

### Playwright 오류 발생 시
1. 환경변수 `PLAYWRIGHT_BROWSER_ARGS` 설정 확인
2. 메모리 제한 증대 고려
3. 로그에서 상세 오류 정보 확인

### 메모리 부족 시
1. `JAVA_OPTS` 환경변수로 힙 메모리 조정
2. Cloudtype 플랜 업그레이드 고려
3. 동시 요청 수 제한

### 타임아웃 오류 시
1. 네트워크 연결 상태 확인
2. 대상 사이트 응답 시간 확인
3. 타임아웃 설정 조정 (application-prod.properties)

## 모니터링
- 애플리케이션 로그를 통한 크롤링 성공/실패 추적
- 메모리 사용량 모니터링
- 응답 시간 측정

## 지원
문제 발생 시 로그 파일과 함께 이슈를 제기해 주세요.
