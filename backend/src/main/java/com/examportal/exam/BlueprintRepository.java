package com.examportal.exam;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlueprintRepository extends JpaRepository<ExamBlueprint, Long> {
}
