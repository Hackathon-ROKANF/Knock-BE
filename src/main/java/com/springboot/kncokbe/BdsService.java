package com.springboot.kncokbe;

import org.springframework.stereotype.Service;

@Service
public class BdsService {
    private final BdsPlaywrightCrawler crawler;

    // Lombok 대신 명시적 생성자 주입 (annotation processing 문제 있어도 안전)
    public BdsService(BdsPlaywrightCrawler crawler) {
        this.crawler = crawler;
    }

    public LowestPriceDto getLowest(String address) {
        return crawler.fetchLowestByAddress(address);
    }
}