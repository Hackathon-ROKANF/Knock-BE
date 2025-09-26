package com.springboot.knockbe.controller;

import com.springboot.knockbe.dto.AnalysisResultResponse;
import com.springboot.knockbe.entity.AnalysisResult;
import com.springboot.knockbe.repository.AnalysisResultRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisResultController {

    private final AnalysisResultRepository analysisResultRepository;

    public AnalysisResultController(AnalysisResultRepository analysisResultRepository) {
        this.analysisResultRepository = analysisResultRepository;
    }

    @PostMapping("/recent") // 엔드포인트 이름은 유지 (요구사항 변경 최소화)
    public ResponseEntity<?> listAllForUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return ResponseEntity.status(401).body(Map.of("message", "인증이 필요합니다."));
        }
        Long userId = (Long) auth.getPrincipal();

        List<AnalysisResult> results = analysisResultRepository.findByUserIdOrderByCreatedAtDesc(userId);

        if (results.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "분석한 결과가 없습니다."));
        }

        List<AnalysisResultResponse> body = results.stream()
                .map(r -> new AnalysisResultResponse(r.getId(), r.getCreatedAt(), r.getUserId()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }
}
