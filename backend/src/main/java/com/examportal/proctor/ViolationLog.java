package com.examportal.proctor;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ViolationLog - One recorded proctoring violation event.
 * Each new violation increments ExamAttempt.violationCount.
 * When count reaches maxViolations, the exam is auto-submitted.
 */
@Entity
@Table(name = "violation_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViolationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long attemptId;

    @Column(nullable = false)
    private Long studentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ViolationType violationType;

    private String details;

    @Column(updatable = false)
    private LocalDateTime occurredAt;

    @PrePersist
    protected void onCreate() { occurredAt = LocalDateTime.now(); }
}
