package com.springboot.knockbe;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pw")
public class PlaywrightController {

    @Autowired
    private PlaywrightService playwrightService;

    @GetMapping("/title")
    public ResponseEntity<String> getPageTitle(@RequestParam String url) {
        try {
            String title = playwrightService.getPageTitle(url);
            return ResponseEntity.ok(title);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("오류: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("서버 오류: " + e.getMessage());
        }
    }
}
