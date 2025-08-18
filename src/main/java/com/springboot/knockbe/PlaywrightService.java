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

            // Cloudtype 환경 감지
            boolean isCloudtype = isCloudtypeEnvironment();
            if (isCloudtype) {
                log.info("Cloudtype 환경 감지됨 - 최적화된 설정 적용");
            }

            // Playwright 초기화 시 재시도 로직
            try {
                playwright = Playwright.create();
                log.info("Playwright 초기화 성공");
            } catch (Exception e) {
                log.error("Playwright 생성 실패: {}", e.getMessage());

                if (isCloudtype) {
                    log.warn("Cloudtype 환경에서 Playwright 초기화 실패 - 대안 방법 시도");
                    System.setProperty("playwright.browsers.download.dir", "/ms-playwright");
                    System.setProperty("playwright.skip_browser_download", "false");

                    try {
                        Thread.sleep(1000);
                        playwright = Playwright.create();
                        log.info("Playwright 재시도 초기화 성공");
                    } catch (Exception retryException) {
                        log.error("Playwright 재시도도 실패: {}", retryException.getMessage());
                        throw new RuntimeException("Playwright 초기화 실패 - 브라우저 바이너리를 찾을 수 없습니다", retryException);
                    }
                } else {
                    throw new RuntimeException("Playwright 초기화 실패", e);
                }
            }

            // Chromium 브라우저 실행 옵션 (배포 환경 최적화)
            List<String> args = List.of(
                    "--no-sandbox",
                    "--disable-dev-shm-usage",
                    "--disable-gpu",
                    "--disable-features=VizDisplayCompositor",
                    "--single-process",
                    "--no-zygote"
            );

            // 환경에 따른 타임아웃 설정
            int launchTimeout = isCloudtype ? 60000 : 30000;

            browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(args)
                    .setTimeout(launchTimeout));

            context = browser.newContext();
            page = context.newPage();
            page.setDefaultTimeout(8000);

            log.info("페이지 이동 중: {}", url);
            page.navigate(url);

            String title = page.title();
            log.info("페이지 제목 추출 완료: {}", title);

            return title;

        } catch (Exception e) {
            log.error("페이지 제목 추출 실패: {}", e.getMessage(), e);
            throw new RuntimeException("페이지 제목을 가져오는데 실패했습니다: " + e.getMessage(), e);
        } finally {
            // 리소스 정리
            try { if (page != null && !page.isClosed()) page.close(); } catch (Exception e) {}
            try { if (context != null) context.close(); } catch (Exception e) {}
            try { if (browser != null) browser.close(); } catch (Exception e) {}
            try { if (playwright != null) playwright.close(); } catch (Exception e) {}
        }
    }

    /**
     * Cloudtype 환경 감지
     */
    private boolean isCloudtypeEnvironment() {
        String port = System.getenv("PORT");
        String hostname = System.getenv("HOSTNAME");
        String cloudtypeApp = System.getenv("CLOUDTYPE_APP_NAME");

        return cloudtypeApp != null ||
                (port != null && hostname != null && hostname.contains("cloudtype")) ||
                System.getProperty("java.io.tmpdir", "").contains("cloudtype");
    }
}
