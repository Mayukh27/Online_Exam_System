package com.examportal.question;

import com.examportal.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * QuestionController - Question bank endpoints.
 * GET /api/questions/subject/{id} - All questions for a subject (ADMIN/TEACHER only, includes answers).
 * POST /api/questions              - Upload a new question (TEACHER or ADMIN).
 */
@RestController
@RequestMapping("/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping("/subject/{subjectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<QuestionDTO>>> getBySubject(@PathVariable Long subjectId) {
        return ResponseEntity.ok(ApiResponse.success(questionService.findBySubject(subjectId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<QuestionDTO>> upload(
            @Valid @RequestBody QuestionDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Question uploaded",
            questionService.upload(dto, userDetails.getUsername())));
    }

    @PostMapping("/excel")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<String>> uploadExcel(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {

        questionService.uploadQuestionsFromExcel(file, userDetails.getUsername());

        return ResponseEntity.ok(ApiResponse.success("Questions uploaded successfully"));
    }

}
