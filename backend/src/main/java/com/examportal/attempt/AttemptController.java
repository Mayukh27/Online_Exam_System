package com.examportal.attempt;

import com.examportal.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AttemptController - Student exam-taking session endpoints.
 *
 * POST /api/attempts/start/{examId}     - Start or resume exam session.
 * PUT  /api/attempts/{id}/answers       - Auto-save answers (called every 30s).
 * POST /api/attempts/{id}/submit        - Final submission.
 */
@RestController
@RequestMapping("/attempts")
@RequiredArgsConstructor
public class AttemptController {

    private final AttemptService attemptService;

    @PostMapping("/start/{examId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<ExamSessionDTO>> startExam(
            @PathVariable Long examId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
            attemptService.startExam(examId, userDetails.getUsername())));
    }

    @PutMapping("/{attemptId}/answers")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Map<String, String>>> saveAnswers(
            @PathVariable Long attemptId,
            @RequestBody SaveAnswerRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
            attemptService.saveAnswers(attemptId, request, userDetails.getUsername())));
    }

    @PostMapping("/{attemptId}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Void>> submit(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal UserDetails userDetails) {
        attemptService.submitExam(attemptId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Exam submitted successfully", null));
    }
}
