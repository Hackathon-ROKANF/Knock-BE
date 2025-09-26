package com.springboot.knockbe.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "analysis_results") // 실제 테이블명 (기존 잘못된 singular 수정)
@EntityListeners(AuditingEntityListener.class)
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // features_json 전체 저장 (주소만 추출해 응답에 사용)
    @Column(name = "features_json", columnDefinition = "TEXT")
    private String featuresJson;

    @Column(name = "prediction")
    private String prediction; // 추가

    public AnalysisResult() {}

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getFeaturesJson() { return featuresJson; }
    public String getPrediction() { return prediction; } // 추가

    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setFeaturesJson(String featuresJson) { this.featuresJson = featuresJson; }
    public void setPrediction(String prediction) { this.prediction = prediction; } // 추가
}
