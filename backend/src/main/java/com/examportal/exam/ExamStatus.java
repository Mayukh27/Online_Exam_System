package com.examportal.exam;

public enum ExamStatus {
    DRAFT,       // Being configured by admin
    PUBLISHED,   // Active; students can start
    COMPLETED,   // All attempts submitted or time expired
    CANCELLED
}
