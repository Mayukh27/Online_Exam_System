package com.examportal.question;

import com.examportal.subject.Subject;
import com.examportal.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Question - A single question in the central question bank.
 *
 * IMAGE STORAGE (via separate tables):
 *   - questionImage / combinedOptionImage -> question_images table (QuestionImage entity, @OneToOne)
 *   - per-option images                  -> question_option_images table (QuestionOptionImage, @OneToMany)
 *
 * The inline BYTEA columns (question_image, question_image_type, combined_option_image,
 * combined_option_image_type) previously on this entity have been removed.
 * They are now stored exclusively in question_images.
 * The schema migration backfills existing data and drops the legacy columns.
 *
 * correctOptionIndex is zero-based; server-side evaluation compares against this.
 * The correct index is NEVER sent to the client during an active exam session.
 */
@Entity
@Table(name = "questions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText;

    // ── Question & combined-option images (question_images table) ─────────
    @OneToOne(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private QuestionImage image;

    // ── Option text ───────────────────────────────────────────────────────
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "question_options", joinColumns = @JoinColumn(name = "question_id"))
    @OrderColumn(name = "option_index")
    @Column(name = "option_text", columnDefinition = "TEXT")
    private List<String> options;

    // ── Per-option images (question_option_images table) ──────────────────
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("optionIndex ASC")
    private List<QuestionOptionImage> optionImages;

    /** Zero-based index into options list identifying the correct answer */
    @Column(nullable = false)
    private Integer correctOptionIndex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Difficulty difficulty = Difficulty.MEDIUM;

    @Builder.Default
    private Integer marks = 1;

    @Builder.Default
    private Double negativeMarks = 0.25;

    @Builder.Default
    private boolean active = true;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}