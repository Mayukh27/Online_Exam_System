package com.examportal.question;

import com.examportal.subject.Subject;
import com.examportal.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Question - A single question in the central question bank.
 * options are stored as an ordered ElementCollection.
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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "question_options", joinColumns = @JoinColumn(name = "question_id"))
    @OrderColumn(name = "option_index")
    @Column(name = "option_text", columnDefinition = "TEXT")
    private List<String> options;

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
