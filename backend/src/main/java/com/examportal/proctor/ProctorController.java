package com.examportal.proctor;

import com.examportal.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ProctorController - Proctoring event ingestion and state query.
 *
 * POST /api/proctor/violation          - Log a browser-detected violation.
 * GET  /api/proctor/state/{attemptId}  - FEATURE: Fullscreen Exam Fix
 *                                        Query current proctor state (fullscreen exits,
 *                                        violation counts, auto-submit status).
 *                                        Called by frontend on session resume and after
 *                                        student dismisses fullscreen warning modal.
 */
@RestController
@RequestMapping("/proctor")
@RequiredArgsConstructor
public class ProctorController {

    private final ProctorService proctorService;

    @PostMapping("/violation")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> logViolation(
            @RequestBody ViolationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
            proctorService.logViolation(request, userDetails.getUsername())));
    }

    /**
     * FEATURE: Fullscreen Exam Fix
     *
     * GET /api/proctor/state/{attemptId}
     *
     * Returns the current proctoring state for the given attempt.
     * Frontend use cases:
     *   1. On exam page load: restore warning state from previous session
     *   2. After student re-enters fullscreen: confirm no auto-submit triggered
     *   3. Heartbeat: detect server-side state changes (e.g. admin force-submit)
     */
    @GetMapping("/state/{attemptId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<ProctorStateDTO>> getState(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
            proctorService.getState(attemptId, userDetails.getUsername())));
    }
}
