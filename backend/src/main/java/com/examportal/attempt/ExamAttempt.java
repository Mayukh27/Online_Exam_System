package com.examportal.attempt;

import com.examportal.exam.Exam;
import com.examportal.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ExamAttempt - One student's session for one exam.
 *
 * questionOrder      : JSON array of question IDs in this student's shuffled sequence.
 * optionOrder        : JSON map { questionId: {displayIdx: originalIdx} }.
 * answers            : JSON map { questionId: selectedShuffledOptionIndex }. -1 = skipped.
 * markedForReview    : JSON array of question IDs flagged by student.
 * violationCount     : count of non-fullscreen violations (tab switch, blur, etc.)
 *                      Hitting max-violations triggers auto-submit.
 * fullscreenExitCount: FEATURE: Fullscreen Exam Fix
 *                      Tracks fullscreen exits separately so they produce warnings
 *                      without immediately triggering auto-submit like other violations.
 *                      Auto-submit only after max-fullscreen-exits threshold.
 *
 * serverStartTime is immutable after creation and drives timer enforcement.
 * UNIQUE constraint on (student_id, exam_id) prevents duplicate attempts.
 */
@Entity
@Table(name = "exam_attempts",
    uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "exam_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @Column(columnDefinition = "TEXT")
    private String questionOrder;

    @Column(columnDefinition = "TEXT")
    private String optionOrder;

    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private String answers = "{}";

    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private String markedForReview = "[]";

    private LocalDateTime serverStartTime;
    private LocalDateTime submittedAt;

    private Double score;

    /** Count of non-fullscreen violations. Auto-submit threshold: exam.proctor.max-violations */
    @Builder.Default
    private Integer violationCount = 0;

    /**
     * FEATURE: Fullscreen Exam Fix
     * Count of fullscreen exits. Tracked separately so the frontend receives a
     * warning/grace period instead of an immediate auto-submit.
     * Auto-submit threshold: exam.proctor.max-fullscreen-exits
     */
    @Builder.Default
    @Column(name = "fullscreen_exit_count")
    private Integer fullscreenExitCount = 0;
}
