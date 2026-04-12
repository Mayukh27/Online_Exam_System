package com.examportal.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIAnalysisResult {
    private Long attemptId;
    private String message;
    private List<String> strengths;
    private List<String> weakAreas;
    private List<String> recommendations;
    private String summary;
}
