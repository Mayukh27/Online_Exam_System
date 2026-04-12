package com.examportal.attempt;

import com.examportal.question.QuestionDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * ExamSessionDTO - Payload returned when a student starts or resumes an exam.
 *
 * CHANGE: sections is now keyed by sectionName (from blueprint), NOT subjectName.
 *   A section may contain questions from multiple subjects.
 *   sectionOrder preserves the ordered list of section names for the UI tab bar.
 *   Questions no longer expose their subject in the exam UI.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamSessionDTO {
    private Long attemptId;
    private Long examId;
    private String examTitle;
    private Integer durationMinutes;
    private LocalDateTime serverStartTime;
    private Long timeRemainingSeconds;
    private List<QuestionDTO> questions;
    /** sectionName -> [0-based indices into questions list] */
    private Map<String, List<Integer>> sections;
    /** Ordered list of section names for the UI tab bar */
    private List<String> sectionOrder;
    /** questionId (String) -> selected shuffled option index, -1 if skipped */
    private Map<String, Integer> savedAnswers;
    private List<Long> markedForReview;
    private AttemptStatus status;
}
