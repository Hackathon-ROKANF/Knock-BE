package com.springboot.knockbe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Playwright Process 기반 크롤러 (JAR 파일 손상 문제 우회용)
 */
@Component
public class PlaywrightProcessCrawler {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightProcessCrawler.class);

    @Value("${bds.base-url}")
    private String baseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public LowestPriceDto fetchLowestByAddressViaProcess(String address) {
        log.info("Process 기반 크롤링 시작: {}", address);

        try {
            // JavaScript 스크립트 생성
            String script = createPlaywrightScript(address);
            Path scriptPath = Files.createTempFile("playwright-script", ".js");
            Files.write(scriptPath, script.getBytes());

            // Node.js 경로 찾기
            String[] nodeCommands = {"node", "/usr/bin/node", "/usr/local/bin/node"};
            String nodeCommand = "node";

            for (String cmd : nodeCommands) {
                try {
                    Process testProcess = new ProcessBuilder(cmd, "--version").start();
                    if (testProcess.waitFor(5, TimeUnit.SECONDS) && testProcess.exitValue() == 0) {
                        nodeCommand = cmd;
                        log.info("Node.js 발견: {}", cmd);
                        break;
                    }
                } catch (Exception e) {
                    log.debug("Node.js 경로 테스트 실패: {}", cmd);
                }
            }

            // Playwright 실행
            ProcessBuilder pb = new ProcessBuilder(nodeCommand, scriptPath.toString());
            pb.redirectErrorStream(true);

            // 환경변수 설정
            pb.environment().put("NODE_PATH", "/usr/lib/node_modules");
            pb.environment().put("PATH", System.getenv("PATH") + ":/usr/bin:/usr/local/bin");

            log.info("Playwright 프로세스 시작: {} {}", nodeCommand, scriptPath);
            Process process = pb.start();

            // 출력 읽기
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    log.debug("Playwright output: {}", line);
                }
            }

            // 프로세스 완료 대기 (최대 60초)
            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Playwright 프로세스 타임아웃");
            }

            int exitCode = process.exitValue();
            log.info("Playwright 프로세스 종료 코드: {}", exitCode);

            // 임시 파일 삭제
            Files.deleteIfExists(scriptPath);

            if (exitCode == 0) {
                return parseResult(output.toString(), address);
            } else {
                log.error("Playwright 프로세스 실행 실패. 출력: {}", output.toString());
                return createFallbackResponse(address, "Process 실행 실패");
            }

        } catch (Exception e) {
            log.error("Process 기반 크롤링 오류: {}", e.getMessage(), e);
            return createFallbackResponse(address, "Process 크롤링 오류: " + e.getMessage());
        }
    }

    private String createPlaywrightScript(String address) {
        return String.format("""
            const { chromium } = require('playwright');
            
            (async () => {
              let browser;
              try {
                browser = await chromium.launch({
                  headless: true,
                  args: [
                    '--no-sandbox',
                    '--disable-dev-shm-usage',
                    '--disable-gpu',
                    '--disable-features=VizDisplayCompositor',
                    '--single-process',
                    '--no-zygote'
                  ]
                });
                
                const context = await browser.newContext({
                  userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
                });
                
                const page = await context.newPage();
                page.setDefaultTimeout(8000);
                
                // 1. 사이트 접속
                await page.goto('%s/main.ytp', { waitUntil: 'domcontentloaded' });
                await page.waitForTimeout(500);
                
                // 2. 검색 입력
                const searchInput = await page.locator('input[placeholder*="주소"], input[type="search"], input[type="text"]').first();
                await searchInput.fill('%s');
                await page.waitForTimeout(300);
                
                // 3. 검색 실행
                await searchInput.press('Enter');
                await page.waitForTimeout(200);
                await page.keyboard.press('ArrowDown');
                await page.waitForTimeout(200);
                await page.keyboard.press('Enter');
                await page.waitForTimeout(1200);
                
                const currentUrl = page.url();
                console.log('CURRENT_URL:' + currentUrl);
                
                // 4. URL 분석 및 매매/전월세 페이지 방문
                const urlMatch = currentUrl.match(/(\\/map\\/realprice_map\\/[^\\/]+\\/N\\/[ABC]\\/)([12])\\/([^\\/]+\\.ytp)/);
                if (urlMatch) {
                  const basePattern = urlMatch[1];
                  const suffix = urlMatch[3];
                  
                  // 매매 페이지
                  const saleUrl = '%s' + basePattern + '1/' + suffix;
                  await page.goto(saleUrl, { waitUntil: 'domcontentloaded' });
                  await page.waitForTimeout(1000);
                  
                  const salePrice = await extractPrice(page);
                  console.log('SALE_PRICE:' + (salePrice || 'null'));
                  
                  // 전월세 페이지
                  const rentUrl = '%s' + basePattern + '2/' + suffix;
                  await page.goto(rentUrl, { waitUntil: 'domcontentloaded' });
                  await page.waitForTimeout(1000);
                  
                  const rentPrice = await extractPrice(page);
                  console.log('RENT_PRICE:' + (rentPrice || 'null'));
                }
                
                async function extractPrice(page) {
                  try {
                    // 가격 추출 로직
                    const priceSelectors = [
                      '.price-info-area .price-area .txt',
                      '.price .txt',
                      '*:has-text("억")'
                    ];
                    
                    for (const selector of priceSelectors) {
                      try {
                        const elements = await page.locator(selector).all();
                        for (const element of elements) {
                          const text = await element.textContent();
                          if (text && text.includes('억') && !text.includes('조')) {
                            // 간단한 가격 파싱 (억, 만원 단위)
                            const match = text.match(/(\\d+)억\\s*(\\d+)?/);
                            if (match) {
                              const eok = parseInt(match[1]) || 0;
                              const man = parseInt(match[2]) || 0;
                              return eok * 100000000 + man * 10000;
                            }
                          }
                        }
                      } catch (e) {
                        // 해당 셀렉터 실패 시 다음 시도
                      }
                    }
                  } catch (e) {
                    console.error('가격 추출 오류:', e.message);
                  }
                  return null;
                }
                
              } catch (error) {
                console.error('크롤링 오류:', error.message);
                process.exit(1);
              } finally {
                if (browser) {
                  await browser.close();
                }
              }
            })();
            """, baseUrl, address, baseUrl, baseUrl);
    }

    private LowestPriceDto parseResult(String output, String address) {
        LowestPriceDto dto = new LowestPriceDto();
        dto.setAddress(address);

        try {
            String[] lines = output.split("\n");
            for (String line : lines) {
                if (line.startsWith("CURRENT_URL:")) {
                    dto.setSourceUrl(line.substring("CURRENT_URL:".length()));
                } else if (line.startsWith("SALE_PRICE:")) {
                    String priceStr = line.substring("SALE_PRICE:".length());
                    if (!"null".equals(priceStr)) {
                        dto.setSaleLowestWon(Long.parseLong(priceStr));
                    }
                } else if (line.startsWith("RENT_PRICE:")) {
                    String priceStr = line.substring("RENT_PRICE:".length());
                    if (!"null".equals(priceStr)) {
                        dto.setJeonseLowestWon(Long.parseLong(priceStr));
                    }
                }
            }
        } catch (Exception e) {
            log.error("결과 파싱 오류: {}", e.getMessage());
        }

        return dto;
    }

    private LowestPriceDto createFallbackResponse(String address, String errorMessage) {
        log.warn("Process 크롤링 실패: {}", errorMessage);
        LowestPriceDto dto = new LowestPriceDto();
        dto.setAddress(address);
        dto.setSourceUrl("크롤링 실패: " + errorMessage);
        dto.setSaleLowestWon(null);
        dto.setJeonseLowestWon(null);
        dto.setWolseDepositLowestWon(null);
        dto.setWolseMonthlyLowestWon(null);
        return dto;
    }
}
