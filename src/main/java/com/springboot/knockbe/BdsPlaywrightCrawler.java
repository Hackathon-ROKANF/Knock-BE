package com.springboot.knockbe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class BdsPlaywrightCrawler {

    private static final Logger log = LoggerFactory.getLogger(BdsPlaywrightCrawler.class);

    @Value("${bds.base-url}")
    private String baseUrl;

    @Value("${bds.search-input-selectors:}")
    private String searchSelectorsProp;

    // 개선된 검색 셀렉터
    private final List<String> searchSelectors = new ArrayList<>(List.of(
            "input[placeholder*=\"주소\"]",
            "input[placeholder*=\"검색\"]",
            "input[placeholder*=\"지하철\"]",
            "input[placeholder*=\"단지\"]",
            "input[type=\"search\"]",
            "input[type=\"text\"]",
            "#searchInput",
            ".search-input",
            "[data-testid=\"search-input\"]"
    ));

    @Value("${bds.response-url-contains}")
    private String respContains;

    @Value("${bds.response-ends-with}")
    private String respEndsWith;

    @Value("${bds.timeout.page:20000}")
    private int pageTimeout;

    @Value("${bds.timeout.network-idle:12000}")
    private int networkIdleWait;

    private final ObjectMapper om = new ObjectMapper();

    @PostConstruct
    void initSelectors() {
        if (searchSelectorsProp != null && !searchSelectorsProp.isBlank()) {
            for (String p : searchSelectorsProp.split(",")) {
                String s = p.trim();
                if (!s.isEmpty() && !searchSelectors.contains(s)) searchSelectors.add(s);
            }
        }
    }

    public LowestPriceDto fetchLowestByAddress(String address) {
        log.info("크롤링 시작: {}", address);

        // Cloudtype 환경 감지
        boolean isCloudtype = isCloudtypeEnvironment();
        if (isCloudtype) {
            log.info("Cloudtype 환경 감지됨 - 최적화된 설정 적용");
        }

        Playwright pw = null;
        Browser browser = null;
        BrowserContext context = null;
        Page page = null;

        try {
            // Playwright 생성 시 더 강력한 예외 처리
            try {
                log.info("Playwright 초기화 시작...");

                // 배포 환경에서는 시스템 속성을 강제로 설정
                if (isCloudtype) {
                    log.info("배포 환경 감지 - 시스템 Playwright 사용 설정");
                    // JAR 드라이버 사용 비활성화
                    System.setProperty("playwright.driver.impl", "shell");
                    System.setProperty("playwright.cli.executable", "/usr/local/bin/playwright");
                    // 브라우저 경로 설정
                    System.setProperty("playwright.browsers.download.dir", "/ms-playwright");
                    System.setProperty("PLAYWRIGHT_BROWSERS_PATH", "/ms-playwright");
                }

                // 환경 변수 확인 및 로깅
                String playwrightPath = System.getenv("PLAYWRIGHT_BROWSERS_PATH");
                String skipDownload = System.getenv("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD");
                log.info("PLAYWRIGHT_BROWSERS_PATH: {}", playwrightPath);
                log.info("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD: {}", skipDownload);
                log.info("playwright.driver.impl: {}", System.getProperty("playwright.driver.impl"));

                // Playwright 생성
                pw = Playwright.create();
                log.info("Playwright 초기화 성공");
            } catch (Exception e) {
                log.error("Playwright 생성 실패: {}", e.getMessage());
                log.error("예외 상세:", e);

                // 배포 환경에서의 대안 처리
                if (isCloudtype) {
                    log.warn("배포 환경에서 Playwright 초기화 실패 - 시스템 명령어로 대체");
                    throw new RuntimeException("Playwright 초기화 실패 - 시스템에서 브라우저를 찾을 수 없습니다", e);
                } else {
                    throw new RuntimeException("Playwright 초기화 실패 - 브라우저 바이너리를 찾을 수 없습니다", e);
                }
            }

            // 배포 환경을 위한 더 강력한 브라우저 옵션
            List<String> args = new ArrayList<>(List.of(
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--disable-features=VizDisplayCompositor",
                "--disable-web-security",
                "--disable-background-timer-throttling",
                "--disable-backgrounding-occluded-windows",
                "--disable-renderer-backgrounding",
                "--disable-extensions",
                "--disable-plugins",
                "--disable-images",
                "--disable-java",
                "--single-process",
                "--no-zygote",
                "--memory-pressure-off",
                "--max_old_space_size=4096",
                "--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            ));

            // Cloudtype 환경에서 추가 최적화 옵션
            if (isCloudtype) {
                args.addAll(List.of(
                    "--disable-setuid-sandbox",
                    "--disable-accelerated-2d-canvas",
                    "--no-first-run",
                    "--disable-default-apps",
                    "--disable-sync",
                    "--disable-translate",
                    "--hide-scrollbars",
                    "--metrics-recording-only",
                    "--mute-audio",
                    "--no-default-browser-check",
                    "--no-pings",
                    "--password-store=basic",
                    "--use-mock-keychain"
                ));
            }

            // 환경변수로 추가 옵션 제어 가능
            String additionalArgs = System.getenv("PLAYWRIGHT_BROWSER_ARGS");
            if (additionalArgs != null && !additionalArgs.trim().isEmpty()) {
                args.addAll(Arrays.asList(additionalArgs.split(",")));
            }

            log.info("브라우저 실행 시도 - 헤드리스 모드, 추가 옵션: {}", args.size());

            // Cloudtype 환경에서는 더 긴 타임아웃 사용
            int launchTimeout = isCloudtype ? 60000 : 30000;

            browser = pw.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(args)
                    .setTimeout(launchTimeout)); // 환경에 따른 타임아웃 조정

            context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .setViewportSize(1920, 1080));

            page = context.newPage();
            page.setDefaultTimeout(8000);

            // 네트워크 응답 모니터링
            List<String> jsonResponses = Collections.synchronizedList(new ArrayList<>());
            page.onResponse(response -> {
                String url = response.url();
                if (url.contains("realprice") || url.contains("api") || url.contains("data")) {
                    try {
                        String body = response.text();
                        if (body != null && (body.contains("{") || body.contains("["))) {
                            jsonResponses.add(body);
                        }
                    } catch (Exception e) {
                        // 네트워크 응답 수집 실패는 무시
                    }
                }
            });

            // 1. 사이트 접속
            page.navigate(baseUrl + "/main.ytp", new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(8000));
            safeWait(page, 500);

            // 2. 검색 실행
            if (!performSearch(page, address)) {
                return createFallbackResponse(address, "검색 실행 실패");
            }

            // 3. URL 분석 및 매매/전월세 URL 생성
            String currentUrl = page.url();
            log.info("검색 완료 URL: {}", currentUrl);

            UrlPair urlPair = generateUrlPair(currentUrl);
            if (urlPair == null) {
                return createFallbackResponse(address, "URL 패턴 분석 실패");
            }

            // 4. 매매 데이터 추출
            Long saleLowest = extractPriceData(page, urlPair.saleUrl, "매매", jsonResponses);

            // 5. 전월세 데이터 추출
            Long rentLowest = extractPriceData(page, urlPair.rentUrl, "전월세", jsonResponses);

            // 결과 DTO 생성
            LowestPriceDto dto = new LowestPriceDto();
            dto.setAddress(address);
            dto.setSourceUrl(urlPair.saleUrl);
            dto.setSaleLowestWon(saleLowest);
            dto.setJeonseLowestWon(rentLowest);
            dto.setWolseDepositLowestWon(null);
            dto.setWolseMonthlyLowestWon(null);

            log.info("크롤링 완료 - 매매: {}, 전월세: {}", saleLowest, rentLowest);
            return dto;

        } catch (Exception e) {
            log.error("크롤링 오류: {}", e.getMessage(), e);
            log.error("크롤링 오류 상세 정보 - 클래스: {}, 메시지: {}", e.getClass().getSimpleName(), e.getMessage());

            // 특정 오류에 대한 추가 정보 제공
            if (e.getMessage() != null) {
                if (e.getMessage().contains("Failed to create driver")) {
                    log.error("브라우저 드라이버 생성 실패 - Playwright 설치 상태를 확인하세요");
                } else if (e.getMessage().contains("chromium")) {
                    log.error("Chromium 브라우저 실행 실패 - 배포 환경에서 브라우저 의존성을 확인하세요");
                } else if (e.getMessage().contains("timeout")) {
                    log.error("타임아웃 오류 - 네트워크 연결 또는 서버 응답 시간을 확인하세요");
                }
            }

            return createFallbackResponse(address, "크롤링 오류: " + e.getMessage());
        } finally {
            cleanupResources(page, context, browser, pw);
        }
    }

    /**
     * 검색 실행
     */
    private boolean performSearch(Page page, String address) {
        try {
            Locator input = findSearchInput(page);
            if (input == null) {
                log.error("검색 인풋을 찾지 못했습니다.");
                return false;
            }

            input.fill("");
            safeWait(page, 200);
            input.fill(address);
            safeWait(page, 300);

            // 검색 실행
            input.press("Enter");
            safeWait(page, 200);

            // 자동완성 선택
            page.keyboard().press("ArrowDown");
            safeWait(page, 200);
            page.keyboard().press("Enter");

            // 페이지 로딩 대기
            safeWait(page, 1200);

            return true;
        } catch (Exception e) {
            log.error("검색 실행 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * URL 패턴 분석 및 매매/전월세 URL 생성
     */
    private UrlPair generateUrlPair(String currentUrl) {
        try {
            // URL 패턴: /map/realprice_map/{encoded_address}/N/{type}/{tab_number}/{price}.ytp
            Pattern pattern = Pattern.compile("(/map/realprice_map/[^/]+/N/[ABC]/)([12])/([^/]+\\.ytp)");
            Matcher matcher = pattern.matcher(currentUrl);

            if (matcher.find()) {
                String basePattern = matcher.group(1);
                String suffix = matcher.group(3);

                String saleUrl = baseUrl + basePattern + "1/" + suffix;
                String rentUrl = baseUrl + basePattern + "2/" + suffix;

                return new UrlPair(saleUrl, rentUrl);
            }

            log.warn("예상하지 못한 URL 패턴: {}", currentUrl);
            return null;

        } catch (Exception e) {
            log.error("URL 생성 오류: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 개선된 가격 데이터 추출
     */
    private Long extractPriceData(Page page, String targetUrl, String tabType, List<String> jsonResponses) {
        try {
            // 해당 탭 URL로 이동
            page.navigate(targetUrl, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(8000));
            safeWait(page, 1000);

            // 1순위: JSON 응답에서 추출
            Long priceFromJson = extractPriceFromJson(jsonResponses, tabType);
            if (priceFromJson != null && priceFromJson > 0) {
                log.info("{} 가격 추출 성공 (JSON): {}", tabType, priceFromJson);
                return priceFromJson;
            }

            // 2순위: DOM에서 추출
            Long priceFromDom = extractPriceFromDOM(page, tabType);
            if (priceFromDom != null && priceFromDom > 0) {
                log.info("{} 가격 추출 성공 (DOM): {}", tabType, priceFromDom);
                return priceFromDom;
            }

            log.warn("{} 탭에서 가격을 찾지 못했습니다", tabType);
            return null;

        } catch (Exception e) {
            log.error("{} 데이터 추출 오류: {}", tabType, e.getMessage());
            return null;
        }
    }

    /**
     * JSON 응답에서 가격 추출
     */
    private Long extractPriceFromJson(List<String> jsonResponses, String tabType) {
        String targetType = getTabNumberFromTabType(tabType);

        for (String json : jsonResponses) {
            try {
                JsonNode root = om.readTree(json);
                Long price = scanJsonForPrice(root, targetType, tabType);
                if (price != null && price > 0) {
                    return price;
                }
            } catch (Exception e) {
                // JSON 파싱 실패는 무시하고 다음 시도
            }
        }

        return null;
    }

    /**
     * JSON에서 가격 스캔
     */
    private Long scanJsonForPrice(JsonNode node, String targetType, String tabType) {
        if (node == null || node.isNull()) return null;

        if (node.isArray()) {
            for (JsonNode item : node) {
                Long result = scanJsonForPrice(item, targetType, tabType);
                if (result != null) return result;
            }
        } else if (node.isObject()) {
            // t_type 확인
            JsonNode tTypeNode = node.get("t_type");
            boolean typeMatches = (targetType == null) ||
                                 (tTypeNode != null && targetType.equals(tTypeNode.asText()));

            if (typeMatches) {
                // 가격 필드 탐색
                String[] priceFields = getPriceFieldsForTabType(tabType);
                for (String field : priceFields) {
                    JsonNode priceNode = findFieldIgnoreCase(node, field);
                    if (priceNode != null && !priceNode.isNull()) {
                        Long price = parsePrice(priceNode);
                        if (price != null && price > 0) {
                            return price;
                        }
                    }
                }
            }

            // 중첩 객체 탐색
            Iterator<JsonNode> elements = node.elements();
            while (elements.hasNext()) {
                Long result = scanJsonForPrice(elements.next(), targetType, tabType);
                if (result != null) return result;
            }
        }

        return null;
    }

    /**
     * DOM 가격 추출
     */
    private Long extractPriceFromDOM(Page page, String tabType) {
        try {
            safeWait(page, 500);

            // 방법 1: "매물 최저가" 라벨 기준으로 추출
            Long price = extractByLowestPriceLabel(page);
            if (price != null && price > 0) {
                return price;
            }

            // 방법 2: price-info-area에서 추출
            price = extractFromPriceInfoArea(page);
            if (price != null && price > 0) {
                return price;
            }

            // 방법 3: 화면의 모든 가격 중 유효한 첫 번째 선택
            price = extractAnyValidPrice(page);
            if (price != null && price > 0) {
                return price;
            }

            return null;

        } catch (Exception e) {
            log.error("DOM 가격 추출 오류: {}", e.getMessage());
            return null;
        }
    }

    /**
     * "매물 최저가" 라벨 기준 추출
     */
    private Long extractByLowestPriceLabel(Page page) {
        try {
            Locator label = page.locator("*:has-text('매물 최저가')").first();
            if (label.count() > 0) {
                Locator priceArea = label.locator(".. .price-info-area .price-area .txt, ../.. .price-info-area .price-area .txt");

                if (priceArea.count() > 0) {
                    String priceText = priceArea.first().textContent().trim();
                    if (!priceText.isEmpty() && (priceText.contains("억") || priceText.contains("만"))) {
                        return MoneyParser.toWon(priceText);
                    }
                }
            }
        } catch (Exception e) {
            // 실패 시 다음 방법 시도
        }
        return null;
    }

    /**
     * price-info-area에서 추출
     */
    private Long extractFromPriceInfoArea(Page page) {
        try {
            Locator priceElements = page.locator(".price-info-area .price-area .txt:visible");
            int count = priceElements.count();

            for (int i = 0; i < count; i++) {
                String priceText = priceElements.nth(i).textContent().trim();
                if (!priceText.isEmpty() && (priceText.contains("억") || priceText.contains("만"))) {
                    Long price = MoneyParser.toWon(priceText);
                    if (price != null && price > 0) {
                        return price;
                    }
                }
            }
        } catch (Exception e) {
            // 실패 시 다음 방법 시도
        }
        return null;
    }

    /**
     * 화면의 모든 유효한 가격 중 첫 번째 선택
     */
    private Long extractAnyValidPrice(Page page) {
        try {
            String[] selectors = {
                ".price-area .txt",
                ".price .txt",
                "*:has-text('억')",
                "span:has-text('억')",
                "div:has-text('억')"
            };

            for (String selector : selectors) {
                try {
                    Locator elements = page.locator(selector);
                    int count = Math.min(elements.count(), 5);

                    for (int i = 0; i < count; i++) {
                        String text = elements.nth(i).textContent().trim();
                        if (text.contains("억") && !text.contains("조")) {
                            Long price = MoneyParser.toWon(text);
                            if (price != null && price > 0) {
                                return price;
                            }
                        }
                    }
                } catch (Exception e) {
                    // 해당 셀렉터 실패 시 다음 시도
                }
            }
        } catch (Exception e) {
            // 모든 방법 실패
        }
        return null;
    }

    // 유틸리티 메서드들
    private String getTabNumberFromTabType(String tabType) {
        return "매매".equals(tabType) ? "1" : "2";
    }

    private String[] getPriceFieldsForTabType(String tabType) {
        if ("매매".equals(tabType)) {
            return new String[]{"trade_price_min", "sale_price_min", "price_min"};
        } else {
            return new String[]{"trade_price_min", "charter_price_min", "jeonse_price_min", "rent_deposit_min", "price_min"};
        }
    }

    private JsonNode findFieldIgnoreCase(JsonNode node, String fieldName) {
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (field.getKey().toLowerCase().contains(fieldName.toLowerCase())) {
                return field.getValue();
            }
        }
        return null;
    }

    private Long parsePrice(JsonNode priceNode) {
        try {
            if (priceNode.isNumber()) {
                return priceNode.asLong() * 10_000L; // 만원 단위를 원 단위로
            } else if (priceNode.isTextual()) {
                return MoneyParser.toWon(priceNode.asText());
            }
        } catch (Exception e) {
            // 파싱 실패 시 null 반환
        }
        return null;
    }

    private void safeWait(Page page, long millis) {
        try {
            if (page != null && !page.isClosed()) {
                page.waitForTimeout(millis);
            }
        } catch (Exception e) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private LowestPriceDto createFallbackResponse(String address, String errorMessage) {
        log.warn("크롤링 실패: {}", errorMessage);
        LowestPriceDto dto = new LowestPriceDto();
        dto.setAddress(address);
        dto.setSourceUrl("크롤링 실패: " + errorMessage);
        dto.setSaleLowestWon(null);
        dto.setJeonseLowestWon(null);
        dto.setWolseDepositLowestWon(null);
        dto.setWolseMonthlyLowestWon(null);
        return dto;
    }

    private void cleanupResources(Page page, BrowserContext context, Browser browser, Playwright pw) {
        try { if (page != null && !page.isClosed()) page.close(); } catch (Exception e) {}
        try { if (context != null) context.close(); } catch (Exception e) {}
        try { if (browser != null) browser.close(); } catch (Exception e) {}
        try { if (pw != null) pw.close(); } catch (Exception e) {}
    }

    private Locator findSearchInput(Page page) {
        // 1) 메인 프레임 후보 셀렉터
        Locator found = trySelectorsOn(page);
        if (found != null) return found;

        // 2) placeholder 기반 검색
        try {
            Locator byPlaceholder = page.getByPlaceholder(Pattern.compile("주소|검색|지하철|단지"));
            if (byPlaceholder.count() > 0) return byPlaceholder.first();
        } catch (Exception e) {}

        // 3) role 기반 검색
        try {
            Locator byRole = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName(Pattern.compile("주소|검색|지하철|단지")));
            if (byRole.count() > 0) return byRole.first();
        } catch (Exception e) {}

        return null;
    }

    private Locator trySelectorsOn(Page page) {
        for (String sel : searchSelectors) {
            try {
                Locator loc = page.locator(sel);
                if (loc.count() > 0) {
                    Locator first = loc.first();
                    try {
                        first.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED).setTimeout(3000));
                    } catch (Exception ignore) {}
                    if (first.isVisible()) return first;
                }
            } catch (Exception ignore) {}
        }
        return null;
    }

    // URL 쌍을 담는 내부 클래스
    private static class UrlPair {
        final String saleUrl;
        final String rentUrl;

        UrlPair(String saleUrl, String rentUrl) {
            this.saleUrl = saleUrl;
            this.rentUrl = rentUrl;
        }
    }

    /**
     * Cloudtype 환경 감지
     */
    private boolean isCloudtypeEnvironment() {
        // 환경변수 기반 감지
        String port = System.getenv("PORT");
        String hostname = System.getenv("HOSTNAME");
        String cloudtypeApp = System.getenv("CLOUDTYPE_APP_NAME");

        // Cloudtype 특징적인 환경변수들 확인
        return cloudtypeApp != null ||
               (port != null && hostname != null && hostname.contains("cloudtype")) ||
               System.getProperty("java.io.tmpdir", "").contains("cloudtype");
    }
}
