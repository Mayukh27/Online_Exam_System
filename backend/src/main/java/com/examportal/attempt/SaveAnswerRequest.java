package com.examportal.attempt;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class SaveAnswerRequest {

    private Map<String, Integer> answers;

    private List<Long> markedForReview;

}