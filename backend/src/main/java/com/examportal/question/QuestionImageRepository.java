package com.examportal.question;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuestionImageRepository extends JpaRepository<QuestionImage, Long> {
    Optional<QuestionImage> findByQuestionId(Long questionId);
}
