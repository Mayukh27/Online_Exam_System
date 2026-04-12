package com.examportal.exam;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    List<Exam> findByStatus(ExamStatus status);

    /** CHANGE: Used by deleteBlueprint() to guard referential integrity */
    List<Exam> findByBlueprintId(Long blueprintId);

    @Query("SELECT e FROM Exam e WHERE e.status = 'PUBLISHED' AND e.scheduledStart <= :now AND e.scheduledEnd >= :now")
    List<Exam> findActiveExams(LocalDateTime now);

    @Query("SELECT e FROM Exam e WHERE e.status = 'PUBLISHED' AND e.scheduledStart > :now")
    List<Exam> findUpcomingExams(LocalDateTime now);

    @Query("SELECT e FROM Exam e WHERE e.status = 'PUBLISHED' AND e.scheduledEnd < :now")
    List<Exam> findExpiredExams(LocalDateTime now);
}
