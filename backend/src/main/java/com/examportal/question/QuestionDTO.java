package com.examportal.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * QuestionDTO - Transfer object for question data.
 * correctOptionIndex is null when served to students during an active exam.
 * It is only populated for admin/teacher views and result pages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDTO {
    private Long id;
    private Long subjectId;
    private String subjectName;
    private String questionText;
    private List<String> options;
    private Integer correctOptionIndex;
    private Difficulty difficulty;
    private Integer marks;
    private Double negativeMarks;
}
