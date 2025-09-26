package com.springboot.knockbe.repository;

import com.springboot.knockbe.entity.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {
    // 사용자 전체 결과 최신순
    List<AnalysisResult> findByUserIdOrderByCreatedAtDesc(Long userId);
}
