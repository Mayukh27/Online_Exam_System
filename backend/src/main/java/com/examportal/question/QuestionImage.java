package com.examportal.question;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "question_images",
        uniqueConstraints = @UniqueConstraint(columnNames = {"question_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "question_image", columnDefinition = "BYTEA")
    private byte[] questionImage;

    @Column(name = "question_image_type", length = 50)
    private String questionImageType;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "combined_option_image", columnDefinition = "BYTEA")
    private byte[] combinedOptionImage;

    @Column(name = "combined_option_image_type", length = 50)
    private String combinedOptionImageType;
}
