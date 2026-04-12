package com.examportal.proctor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ProctorStateDTO - FEATURE: Fullscreen Exam Fix
 *
 * Returned by GET /api/proctor/state/{attemptId}.
 * The frontend calls this whenever it needs to sync proctoring state —
 * e.g. after the student re-enters fullscreen following a FULLSCREEN_EXIT warning.
 *
 * Fields:
 *   fullscreenExitCount    - how many times fullscreen has been exited
 *   maxFullscreenExits     - threshold before auto-submit (from config)
 *   fullscreenWarningsLeft - how many more exits allowed before auto-submit
 *   violationCount         - non-fullscreen violations logged
 *   maxViolations          - threshold for non-fullscreen auto-submit
 *   autoSubmitted          - true if exam was already auto-submitted
 *   requiresFullscreen     - always true; frontend uses this to decide whether
 *                            to re-request fullscreen after a warning dismiss
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProctorStateDTO {
    private Long attemptId;
    private Integer fullscreenExitCount;
    private Integer maxFullscreenExits;
    private Integer fullscreenWarningsLeft;
    private Integer violationCount;
    private Integer maxViolations;
    private boolean autoSubmitted;
    private boolean requiresFullscreen;
}
