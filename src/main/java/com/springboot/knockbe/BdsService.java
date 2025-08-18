package com.springboot.knockbe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BdsService {

    private static final Logger log = LoggerFactory.getLogger(BdsService.class);

    private final BdsPlaywrightCrawler crawler;
    private final PlaywrightProcessCrawler processCrawler;

    // Lombok 대신 명시적 생성자 주입 (annotation processing 문제 있어도 안전)
    public BdsService(BdsPlaywrightCrawler crawler, PlaywrightProcessCrawler processCrawler) {
        this.crawler = crawler;
        this.processCrawler = processCrawler;
    }

    public LowestPriceDto getLowest(String address) {
        // 환경 변수 로깅
        log.info("PORT: {}", System.getenv("PORT"));
        log.info("HOSTNAME: {}", System.getenv("HOSTNAME"));
        log.info("CLOUDTYPE_APP_NAME: {}", System.getenv("CLOUDTYPE_APP_NAME"));
        log.info("USER: {}", System.getenv("USER"));
        log.info("PWD: {}", System.getenv("PWD"));

        // 항상 Process 크롤러를 먼저 시도 (배포 환경에서)
        boolean tryProcessFirst = System.getenv("PORT") != null || // CloudType/Heroku 등
                                 System.getenv("HOSTNAME") != null ||
                                 !System.getProperty("user.name", "").equals("hwanghyeongjin"); // 로컬이 아닌 경우

        if (tryProcessFirst) {
            log.info("배포 환경 감지 - Process 기반 크롤링 우선 시도");
            try {
                LowestPriceDto result = processCrawler.fetchLowestByAddressViaProcess(address);
                if (result != null && !isFailureResult(result)) {
                    log.info("Process 크롤링 성공");
                    return result;
                }
                log.warn("Process 크롤링 실패, 기존 방법으로 fallback");
            } catch (Exception e) {
                log.error("Process 크롤링 오류, 기존 방법으로 fallback: {}", e.getMessage());
            }
        }

        // 기존 Playwright JAR 방식 시도
        log.info("JAR 기반 Playwright 크롤링 시도");
        return crawler.fetchLowestByAddress(address);
    }

    private boolean isFailureResult(LowestPriceDto result) {
        return result.getSourceUrl() != null && result.getSourceUrl().startsWith("크롤링 실패");
    }

    private boolean isCloudtypeEnvironment() {
        String port = System.getenv("PORT");
        String hostname = System.getenv("HOSTNAME");
        String cloudtypeApp = System.getenv("CLOUDTYPE_APP_NAME");

        return cloudtypeApp != null ||
               (port != null && hostname != null && hostname.contains("cloudtype")) ||
               System.getProperty("java.io.tmpdir", "").contains("cloudtype");
    }
}