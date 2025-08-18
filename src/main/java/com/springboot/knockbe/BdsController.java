package com.springboot.knockbe;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/bds")
public class BdsController {

    private final BdsService service;

    public BdsController(BdsService service) {
        this.service = service;
    }

    @GetMapping("/lowest")
    public LowestPriceDto lowest(@RequestParam String address) {
        return service.getLowest(address);
    }
}