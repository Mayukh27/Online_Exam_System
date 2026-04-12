package com.examportal.subject;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Subject
 * CHANGE: subjectCode is now the canonical external identifier.
 * "Subject ID = Subject Code" — the code field is unique and used as the
 * display/reference identifier throughout the UI and export features.
 * The database PK (Long id) is internal only.
 * DELETE cascade: removing a subject removes all related questions via DB FK.
 */
@Entity
@Table(name = "subjects")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    /** Subject Code = Subject ID (external reference, unique, e.g. "MATH", "PHY") */
    @Column(nullable = false, unique = true)
    private String code;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
