package com.examportal.exam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamDTO {
    private Long id;
    private String title;
    private String description;
    private Long blueprintId;
    private ExamStatus status;
    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;
    private Integer durationMinutes;
    private Integer totalMarks;
}
