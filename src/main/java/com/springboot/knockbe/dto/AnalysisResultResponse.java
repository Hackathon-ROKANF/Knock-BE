package com.springboot.knockbe.dto;

import java.time.LocalDateTime;

public class AnalysisResultResponse {
    private Long id;
    private LocalDateTime createdAt;
    private Long userId;

    public AnalysisResultResponse(Long id, LocalDateTime createdAt, Long userId) {
        this.id = id;
        this.createdAt = createdAt;
        this.userId = userId;
    }

    public Long getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getUserId() { return userId; }
}

