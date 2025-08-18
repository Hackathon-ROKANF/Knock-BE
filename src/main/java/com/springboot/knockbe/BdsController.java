package com.springboot.knockbe;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;


@RestController
@RequestMapping("/api/bds")
public class BdsController {

    private static final Logger log = LoggerFactory.getLogger(BdsController.class);
    private final BdsService service;

    public BdsController(BdsService service) {
        this.service = service;
    }

    @GetMapping("/lowest")
    public LowestPriceDto lowest(@RequestParam String address) {
        log.info("요청받은 주소 (원본): {}", address);

        String processedAddress = address;

        try {
            // 1차: URL 디코딩 시도
            String decodedAddress = URLDecoder.decode(address, StandardCharsets.UTF_8);
            log.info("URL 디코딩 결과: {}", decodedAddress);

            // 2차: 여전히 인코딩된 문자가 있는지 확인하여 재디코딩
            if (decodedAddress.contains("%")) {
                String doubleDecoded = URLDecoder.decode(decodedAddress, StandardCharsets.UTF_8);
                log.info("이중 디코딩 결과: {}", doubleDecoded);
                processedAddress = doubleDecoded;
            } else {
                processedAddress = decodedAddress;
            }

        } catch (Exception e) {
            log.warn("URL 디코딩 실패, 원본 주소 사용: {}", e.getMessage());
            processedAddress = address;
        }

        log.info("최종 처리된 주소: {}", processedAddress);
        return service.getLowest(processedAddress);
    }
}