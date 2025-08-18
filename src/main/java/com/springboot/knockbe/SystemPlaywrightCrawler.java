package com.springboot.knockbe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SystemPlaywrightCrawler {

    private static final Logger log = LoggerFactory.getLogger(SystemPlaywrightCrawler.class);

    @Value("${bds.base-url}")
    private String baseUrl;

    private final ObjectMapper om = new ObjectMapper();

    public LowestPriceDto fetchLowestByAddress(String address) {
        log.info("시스템 Playwright 크롤링 시작: {}", address);

        try {
            // 1. JavaScript 크롤링 스크립트 생성
            String script = createCrawlingScript(address);
            Path scriptPath = saveScriptToFile(script);

            // 2. Playwright 명령어 실행
            String result = executePlaywrightScript(scriptPath);

            // 3. 결과 파싱
            return parseResult(address, result);

        } catch (Exception e) {
            log.error("시스템 Playwright 크롤링 오류: {}", e.getMessage(), e);
            return createFallbackResponse(address, "시스템 크롤링 오류: " + e.getMessage());
        }
    }

    private String createCrawlingScript(String address) {
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
            
                // 2. 검색 실행
                const searchInput = await page.locator('input[placeholder*="주소"], input[type="search"], input[type="text"]').first();
                if (await searchInput.count() > 0) {
                  await searchInput.fill('%s');
                  await page.waitForTimeout(300);
                  await searchInput.press('Enter');
                  await page.waitForTimeout(200);
                  await page.keyboard.press('ArrowDown');
                  await page.waitForTimeout(200);
                  await page.keyboard.press('Enter');
                  await page.waitForTimeout(1200);
                }
            
                // 3. URL 분석 및 가격 추출
                const currentUrl = page.url();
                const urlPattern = /(\\/map\\/realprice_map\\/[^\\/]+\\/N\\/[ABC]\\/)([12])\\/([^\\/]+\\.ytp)/;
                const matcher = currentUrl.match(urlPattern);
            
                if (matcher) {
                  const basePattern = matcher[1];
                  const suffix = matcher[3];
                  
                  // 매매 가격 추출
                  const saleUrl = '%s' + basePattern + '1/' + suffix;
                  await page.goto(saleUrl, { waitUntil: 'domcontentloaded' });
                  await page.waitForTimeout(1000);
                  
                  const salePrice = await extractPrice(page);
                  
                  // 전월세 가격 추출
                  const rentUrl = '%s' + basePattern + '2/' + suffix;
                  await page.goto(rentUrl, { waitUntil: 'domcontentloaded' });
                  await page.waitForTimeout(1000);
                  
                  const rentPrice = await extractPrice(page);
                  
                  // 결과 출력
                  console.log(JSON.stringify({
                    success: true,
                    address: '%s',
                    salePrice: salePrice,
                    rentPrice: rentPrice,
                    sourceUrl: saleUrl
                  }));
                } else {
                  console.log(JSON.stringify({
                    success: false,
                    error: 'URL 패턴 분석 실패',
                    address: '%s'
                  }));
                }
            
              } catch (error) {
                console.log(JSON.stringify({
                  success: false,
                  error: error.message,
                  address: '%s'
                }));
              } finally {
                if (browser) {
                  await browser.close();
                }
              }
            })();
            
            async function extractPrice(page) {
              try {
                // 방법 1: 매물 최저가 라벨 기준
                const labelLocator = page.locator('*:has-text("매물 최저가")').first();
                if (await labelLocator.count() > 0) {
                  const priceArea = labelLocator.locator('.. .price-info-area .price-area .txt');
                  if (await priceArea.count() > 0) {
                    const priceText = await priceArea.first().textContent();
                    if (priceText && (priceText.includes('억') || priceText.includes('만'))) {
                      return parsePrice(priceText.trim());
                    }
                  }
                }
            
                // 방법 2: price-info-area에서 추출
                const priceElements = page.locator('.price-info-area .price-area .txt:visible');
                const count = await priceElements.count();
                for (let i = 0; i < count; i++) {
                  const priceText = await priceElements.nth(i).textContent();
                  if (priceText && (priceText.includes('억') || priceText.includes('만'))) {
                    const price = parsePrice(priceText.trim());
                    if (price && price > 0) {
                      return price;
                    }
                  }
                }
            
                return null;
              } catch (error) {
                return null;
              }
            }
            
            function parsePrice(priceText) {
              if (!priceText) return null;
              
              try {
                // 억, 만원 단위 파싱
                const cleanText = priceText.replace(/[^0-9억만.,]/g, '');
                
                if (cleanText.includes('억')) {
                  const parts = cleanText.split('억');
                  let total = 0;
                  
                  if (parts[0]) {
                    total += parseFloat(parts[0].replace(/,/g, '')) * 100000000;
                  }
                  
                  if (parts[1] && parts[1].includes('만')) {
                    const manPart = parts[1].replace('만', '').replace(/,/g, '');
                    if (manPart) {
                      total += parseFloat(manPart) * 10000;
                    }
                  }
                  
                  return total;
                } else if (cleanText.includes('만')) {
                  const manValue = cleanText.replace('만', '').replace(/,/g, '');
                  return parseFloat(manValue) * 10000;
                }
                
                return null;
              } catch (error) {
                return null;
              }
            }
            """, baseUrl, address, baseUrl, baseUrl, address, address, address);
    }

    private Path saveScriptToFile(String script) throws IOException {
        Path scriptPath = Paths.get("/tmp/playwright_script.js");
        Files.write(scriptPath, script.getBytes());
        return scriptPath;
    }

    private String executePlaywrightScript(Path scriptPath) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("node", scriptPath.toString());
        pb.environment().put("NODE_PATH", "/usr/local/lib/node_modules");
        pb.environment().put("PLAYWRIGHT_BROWSERS_PATH", "/ms-playwright");

        Process process = pb.start();

        // 출력 읽기
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        // 에러 출력 읽기
        StringBuilder errorOutput = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
        }

        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Playwright 스크립트 실행 타임아웃");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            log.error("Playwright 스크립트 실행 실패. Exit code: {}, Error: {}", exitCode, errorOutput.toString());
            throw new RuntimeException("Playwright 스크립트 실행 실패: " + errorOutput.toString());
        }

        return output.toString().trim();
    }

    private LowestPriceDto parseResult(String address, String result) {
        try {
            JsonNode json = om.readTree(result);

            if (json.get("success").asBoolean()) {
                LowestPriceDto dto = new LowestPriceDto();
                dto.setAddress(address);
                dto.setSourceUrl(json.get("sourceUrl").asText());

                JsonNode salePrice = json.get("salePrice");
                JsonNode rentPrice = json.get("rentPrice");

                dto.setSaleLowestWon(salePrice != null && !salePrice.isNull() ? salePrice.asLong() : null);
                dto.setJeonseLowestWon(rentPrice != null && !rentPrice.isNull() ? rentPrice.asLong() : null);
                dto.setWolseDepositLowestWon(null);
                dto.setWolseMonthlyLowestWon(null);

                log.info("시스템 크롤링 성공 - 매매: {}, 전월세: {}", dto.getSaleLowestWon(), dto.getJeonseLowestWon());
                return dto;
            } else {
                String error = json.get("error").asText();
                log.warn("시스템 크롤링 실패: {}", error);
                return createFallbackResponse(address, error);
            }
        } catch (Exception e) {
            log.error("결과 파싱 오류: {}", e.getMessage());
            return createFallbackResponse(address, "결과 파싱 오류: " + e.getMessage());
        }
    }

    private LowestPriceDto createFallbackResponse(String address, String errorMessage) {
        LowestPriceDto dto = new LowestPriceDto();
        dto.setAddress(address);
        dto.setSourceUrl("시스템 크롤링 실패: " + errorMessage);
        dto.setSaleLowestWon(null);
        dto.setJeonseLowestWon(null);
        dto.setWolseDepositLowestWon(null);
        dto.setWolseMonthlyLowestWon(null);
        return dto;
    }
}
