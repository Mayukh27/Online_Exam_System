package com.examportal.exam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * BlueprintEntryDTO - one subject entry in a blueprint.
 * sectionName is optional. If all entries have blank sectionName
 * the exam UI shows a flat question list with no tabs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlueprintEntryDTO {
    /** Optional section label. Blank = no section tab in exam UI. */
    private String sectionName;
    /** The subject to draw questions from */
    private Long subjectId;
    private Integer questionCount;
    private Integer marksPerQuestion;
    private Double negativeMarks;
}