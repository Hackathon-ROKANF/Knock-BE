package com.springboot.knockbe.controller;

import com.springboot.knockbe.dto.AnalysisResultResponse;
import com.springboot.knockbe.dto.AnalysisResultDetailRequest; // 추가
import com.springboot.knockbe.entity.AnalysisResult;
import com.springboot.knockbe.repository.AnalysisResultRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*; // RequestBody 포함

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 추가 import
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.EmptyResultDataAccessException;
import com.fasterxml.jackson.databind.ObjectMapper; // 추가
import com.fasterxml.jackson.core.type.TypeReference; // 추가

@RestController
@RequestMapping("/api/analysis")
public class AnalysisResultController {

    private final AnalysisResultRepository analysisResultRepository;
    private final JdbcTemplate jdbcTemplate; // 추가
    private final ObjectMapper objectMapper = new ObjectMapper(); // features_json 파싱용

    public AnalysisResultController(AnalysisResultRepository analysisResultRepository, JdbcTemplate jdbcTemplate) {
        this.analysisResultRepository = analysisResultRepository;
        this.jdbcTemplate = jdbcTemplate;
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
                .map(r -> new AnalysisResultResponse(
                        r.getId(),
                        r.getCreatedAt(),
                        r.getUserId(),
                        extractAddress(r.getFeaturesJson()),
                        r.getPrediction() // prediction 추가
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    // features_json에서 "주소" 값 추출 (없으면 null)
    private String extractAddress(String featuresJson) {
        if (featuresJson == null || featuresJson.isBlank()) return null;
        try {
            Map<String, Object> map = objectMapper.readValue(featuresJson, new TypeReference<Map<String, Object>>(){});
            Object addr = map.get("주소");
            return addr == null ? null : addr.toString();
        } catch (Exception e) {
            return null; // 파싱 실패 시 null
        }
    }

    @PostMapping("/detail")
    public ResponseEntity<?> detail(@RequestBody AnalysisResultDetailRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return ResponseEntity.status(401).body(Map.of("message", "인증이 필요합니다."));
        }
        if (request == null || request.getId() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "id 값이 필요합니다."));
        }
        Long userId = (Long) auth.getPrincipal();

        // 모든 컬럼 그대로 반환 (LinkedHashMap 형태)
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT * FROM analysis_results WHERE id = ? AND user_id = ?", request.getId(), userId
            );
            // prediction 별도 키 (이미 row 안에 있을 수 있지만 명시적으로 포함)
            return ResponseEntity.ok(row);
        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity.status(404).body(Map.of(
                "message", "해당 결과가 없습니다.",
                "id", request.getId()
            ));
        }
    }
}
