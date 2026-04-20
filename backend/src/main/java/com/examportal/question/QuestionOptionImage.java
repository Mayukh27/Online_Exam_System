package com.examportal.question;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * QuestionOptionImage - Stores the image for a single option slot of a Question.
 *
 * Each row represents one option image:
 *   question_id  → FK to questions.id
 *   option_index → 0-based, matches the slot in question.options[]
 *   image_data   → raw bytes (BYTEA)
 *   image_type   → MIME type e.g. "image/png"
 *
 * A question may have 0-4 option images. Missing rows mean text-only for that slot.
 */
@Entity
@Table(name = "question_option_images",
        uniqueConstraints = @UniqueConstraint(columnNames = {"question_id", "option_index"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionOptionImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "option_index", nullable = false)
    private Integer optionIndex;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "image_data", columnDefinition = "BYTEA", nullable = false)
    private byte[] imageData;

    @Column(name = "image_type", length = 50, nullable = false)
    private String imageType;
}