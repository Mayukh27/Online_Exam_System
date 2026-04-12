package com.examportal.user;

/**
 * Role - Three-tier role system.
 * ADMIN: Platform administrator; approves teachers, creates subjects/blueprints/exams.
 * TEACHER: Uploads questions; views subject-level analytics.
 * STUDENT: Takes exams; views results.
 */
public enum Role {
    ADMIN,
    TEACHER,
    STUDENT
}
