package com.examportal.analytics;

import com.examportal.common.ApiResponse;
import com.examportal.common.FeatureDisabledException;
import com.examportal.config.FeatureFlags;
import com.examportal.evaluation.EvaluationRepository;
import com.examportal.evaluation.EvaluationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AnalyticsController - Exam and subject-level analytics (ADMIN and TEACHER).
 * GET /api/analytics/exam/{examId}         - All results for an exam.
 * GET /api/analytics/exam/{examId}/summary - Aggregate statistics.
 */
@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final EvaluationRepository evaluationRepository;
    private final FeatureFlags featureFlags;

    @GetMapping("/exam/{examId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<EvaluationResult>>> getExamResults(@PathVariable Long examId) {
        if (!featureFlags.isAnalytics()) throw new FeatureDisabledException("analytics");
        return ResponseEntity.ok(ApiResponse.success(evaluationRepository.findByExamId(examId)));
    }

    @GetMapping("/exam/{examId}/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExamSummary(@PathVariable Long examId) {
        if (!featureFlags.isAnalytics()) throw new FeatureDisabledException("analytics");
        List<EvaluationResult> results = evaluationRepository.findByExamId(examId);
        if (results.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(Map.of("message", "No results yet", "totalAttempts", 0)));
        }
        double avg = results.stream().mapToDouble(r -> r.getTotalScore() != null ? r.getTotalScore() : 0).average().orElse(0);
        double max = results.stream().mapToDouble(r -> r.getTotalScore() != null ? r.getTotalScore() : 0).max().orElse(0);
        double min = results.stream().mapToDouble(r -> r.getTotalScore() != null ? r.getTotalScore() : 0).min().orElse(0);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "totalAttempts", results.size(),
            "averageScore", Math.round(avg * 100.0) / 100.0,
            "maxScore", max, "minScore", min
        )));
    }
}
