package com.examportal.proctor;

import lombok.Data;

/**
 * ViolationRequest - Payload sent by the frontend when a proctoring event is detected.
 *
 * FEATURE: Fullscreen Exam Fix
 * violationType = FULLSCREEN_EXIT is now handled separately in ProctorService:
 *   it increments fullscreenExitCount (not violationCount) and returns a warning
 *   with a grace period for the student to re-enter fullscreen.
 */
@Data
public class ViolationRequest {
    private Long attemptId;
    private ViolationType violationType;
    private String details;
}
