package com.examportal.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * QuestionDTO - Transfer object for question data.
 *
 * IMAGE FIELDS:
 *   hasQuestionImage  : true if the question has an image stored in DB.
 *                       Clients fetch the actual bytes via GET /api/questions/{id}/image
 *   optionHasImage    : parallel boolean list (size 4). optionHasImage[i]=true means
 *                       option i has an image; fetch via GET /api/questions/{id}/option-image/{i}
 *
 * Raw image bytes are NOT included in the DTO — they are served as separate binary
 * endpoints to keep JSON payloads small and allow browser-level caching.
 *
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

    // ── Image metadata (no raw bytes in DTO) ─────────────────────────────
    /** True if this question has an image; fetch from /api/questions/{id}/image */
    private boolean hasQuestionImage;

    /** True if this question has a combined option illustration image. */
    private boolean hasCombinedOptionImage;

    /**
     * Per-option image flag. Index matches options[]. Null-safe: if shorter than
     * options list, treat missing entries as false.
     */
    private List<Boolean> optionHasImage;
}