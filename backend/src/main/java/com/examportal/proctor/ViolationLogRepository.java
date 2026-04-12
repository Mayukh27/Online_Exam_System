package com.examportal.proctor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ViolationLogRepository extends JpaRepository<ViolationLog, Long> {
    List<ViolationLog> findByAttemptId(Long attemptId);
    long countByAttemptId(Long attemptId);
}
