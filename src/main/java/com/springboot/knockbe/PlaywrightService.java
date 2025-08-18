package com.springboot.knockbe;

import com.microsoft.playwright.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlaywrightService {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightService.class);

    public String getPageTitle(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL은 필수입니다");
        }

        Playwright playwright = null;
        Browser browser = null;
        BrowserContext context = null;
        Page page = null;

        try {
            log.info("Playwright 초기화 시작 - URL: {}", url);

            playwright = Playwright.create();

            // Chromium 브라우저 실행 옵션
            List<String> args = List.of(
                "--no-sandbox",
                "--disable-dev-shm-usage"
            );

            browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setArgs(args));

            context = browser.newContext();
            page = context.newPage();

            log.info("페이지 이동 중: {}", url);
            page.navigate(url);

            String title = page.title();
            log.info("페이지 제목 추출 완료: {}", title);

            return title;

        } catch (Exception e) {
            log.error("페이지 제목 추출 실패 - URL: {}, 오류: {}", url, e.getMessage(), e);
            throw new RuntimeException("페이지 제목을 가져오는데 실패했습니다: " + e.getMessage(), e);
        } finally {
            // 리소스 정리
            try { if (page != null) page.close(); } catch (Exception e) {}
            try { if (context != null) context.close(); } catch (Exception e) {}
            try { if (browser != null) browser.close(); } catch (Exception e) {}
            try { if (playwright != null) playwright.close(); } catch (Exception e) {}
        }
    }
}
