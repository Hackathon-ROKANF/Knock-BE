package com.springboot.knockbe.dto;

import java.time.LocalDateTime;

public class AnalysisResultResponse {
    private Long id;
    private LocalDateTime createdAt;
    private Long userId;
    private String address; // 추가
    private String prediction; // 추가

    public AnalysisResultResponse(Long id, LocalDateTime createdAt, Long userId, String address, String prediction) {
        this.id = id;
        this.createdAt = createdAt;
        this.userId = userId;
        this.address = address;
        this.prediction = prediction; // 추가
    }

    public Long getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getUserId() { return userId; }
    public String getAddress() { return address; }
    public String getPrediction() { return prediction; } // 추가
}
