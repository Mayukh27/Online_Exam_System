package com.examportal.attempt;

public enum AttemptStatus {
    IN_PROGRESS,
    SUBMITTED,
    AUTO_SUBMITTED,  // System submitted due to timer expiry or violation threshold
    EVALUATED
}
