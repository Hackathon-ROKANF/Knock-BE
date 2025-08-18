package com.springboot.knockbe;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;


@RestController
@RequestMapping("/api/bds")
public class BdsController {

    private final BdsService service;

    public BdsController(BdsService service) {
        this.service = service;
    }

    @GetMapping("/lowest")
    public LowestPriceDto lowest(@RequestParam String address) {
        try {
            // URL 디코딩 처리 (이미 인코딩된 경우를 대비)
            String decodedAddress = URLDecoder.decode(address, StandardCharsets.UTF_8);
            return service.getLowest(decodedAddress);
        } catch (Exception e) {
            // 디코딩 실패 시 원본 주소 사용
            return service.getLowest(address);
        }
    }
}