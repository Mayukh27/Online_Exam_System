package com.examportal.exam;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Exam
 * CHANGE: Added masterSectionMap — stores { sectionName: [questionIds] }
 *   Generated at publish time alongside masterQuestionIds.
 *   Used by AttemptService to reconstruct section groupings per student.
 */
@Entity
@Table(name = "exams")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blueprint_id")
    private ExamBlueprint blueprint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ExamStatus status = ExamStatus.DRAFT;

    @Column(nullable = false)
    private LocalDateTime scheduledStart;

    @Column(nullable = false)
    private LocalDateTime scheduledEnd;

    @Column(nullable = false)
    private Integer durationMinutes;

    /**
     * Flat JSON array of all master question IDs across all sections.
     * Used for per-student shuffle (AttemptService shuffles this list).
     */
    @Column(columnDefinition = "TEXT")
    private String masterQuestionIds;

    /**
     * CHANGE: JSON map { sectionName: [questionIds] } preserving section structure.
     * e.g. {"Section 1 – MCQ": [42,17,88], "Section 2 – Numerical": [3,55]}
     * Set at publish time; immutable thereafter.
     */
    @Column(columnDefinition = "TEXT")
    private String masterSectionMap;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
