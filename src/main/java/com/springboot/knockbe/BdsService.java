package com.springboot.knockbe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BdsService {

    private static final Logger log = LoggerFactory.getLogger(BdsService.class);

    private final BdsPlaywrightCrawler crawler;

    // Lombok 대신 명시적 생성자 주입 (annotation processing 문제 있어도 안전)
    public BdsService(BdsPlaywrightCrawler crawler) {
        this.crawler = crawler;
    }

    public LowestPriceDto getLowest(String address) {
        // 환경 변수 로깅
        log.info("PORT: {}", System.getenv("PORT"));
        log.info("HOSTNAME: {}", System.getenv("HOSTNAME"));
        log.info("CLOUDTYPE_APP_NAME: {}", System.getenv("CLOUDTYPE_APP_NAME"));
        log.info("USER: {}", System.getenv("USER"));
        log.info("PWD: {}", System.getenv("PWD"));

        // Java Playwright 크롤링
        log.info("Java Playwright 크롤링 시작");
        return crawler.fetchLowestByAddress(address);
    }
}