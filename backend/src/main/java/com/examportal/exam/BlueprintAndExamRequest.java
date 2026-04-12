package com.examportal.exam;

import lombok.Data;
import java.util.List;

@Data
public class BlueprintAndExamRequest {

    private String title;
    private String description;
    private Integer durationMinutes;

    private List<BlueprintEntryDTO> entries;

}