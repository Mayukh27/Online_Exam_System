package com.examportal.ai;

import com.examportal.config.FeatureFlags;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * AIAnswerCheckerService - AI-powered performance analysis.
 * PURPOSE: Analyzes student exam responses to produce:
 *   - Subject-wise strength/weakness breakdown
 *   - Recommended study focus areas
 *   - Performance trend summary
 * STATUS: FULLY IMPLEMENTED STRUCTURE BUT CORE LOGIC COMMENTED OUT
 * FEATURE FLAG: features.ai=true in application.yml
 * TO ENABLE:
 *   1. Set features.ai=true
 *   2. Add LLM SDK dependency to pom.xml (e.g. openai-java or Anthropic SDK)
 *   3. Add API key to application.yml
 *   4. Uncomment the implementation block below
 */
@Service
@RequiredArgsConstructor
public class AIAnswerCheckerService {

    private final FeatureFlags featureFlags;

    public AIAnalysisResult analyze(Long attemptId) {
        if (!featureFlags.isAi()) {
            throw new RuntimeException("AI feature is disabled. Set features.ai=true in application.yml.");
        }

        // ================================================================
        // AI IMPLEMENTATION - UNCOMMENT AFTER CONFIGURING LLM API KEY
        // ================================================================
        //
        // // Step 1: Load evaluation result
        // EvaluationResult result = evaluationRepository.findByAttemptId(attemptId)
        //     .orElseThrow(() -> new IllegalArgumentException("Result not found"));
        //
        // // Step 2: Parse subject breakdown
        // Map<String, Object> breakdown = objectMapper.readValue(
        //     result.getSubjectWiseBreakdown(), new TypeReference<Map<String, Object>>() {});
        //
        // // Step 3: Build analysis prompt
        // String prompt = "Analyze the following student exam performance data and provide:"
        //     + "1. Identified strength areas"
        //     + "2. Weak areas needing improvement"
        //     + "3. Specific actionable study recommendation"
        //     + "4. Brief overall performance summary"
        //     + "Subject results: " + breakdown.toString() + ""
        //     + "Total score: " + result.getTotalScore() + ""
        //     + "Correct: " + result.getCorrect() + ""
        //     + "Wrong: " + result.getWrong();
        //
        // // Step 4: Call LLM (OpenAI example)
        // OpenAIClient client = OpenAIClient.builder().apiKey(apiKey).build();
        // ChatCompletion response = client.chat().completions().create(
        //     ChatCompletionCreateParams.builder()
        //         .model("gpt-4o-mini")
        //         .addUserMessage(prompt)
        //         .maxTokens(1000)
        //         .build()
        // );
        // String aiText = response.choices().get(0).message().content().orElse("");
        //
        // // Step 5: Parse and structure response
        // return AIAnalysisResult.builder()
        //     .attemptId(attemptId)
        //     .summary(extractSection(aiText, "Overall"))
        //     .strengths(extractList(aiText, "Strength"))
        //     .weakAreas(extractList(aiText, "Weak"))
        //     .recommendations(extractList(aiText, "Recommendation"))
        //     .build();
        //
        // ================================================================

        return AIAnalysisResult.builder()
            .message("AI analysis is disabled. Enable features.ai in application.yml.")
            .build();
    }
}
