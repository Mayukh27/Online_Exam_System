package com.examportal.evaluation;

import com.examportal.common.ApiResponse;
import com.examportal.exam.ExamService;
import com.examportal.exam.ExamDTO;
import com.examportal.reporting.ReportService;
import com.examportal.user.UserRepository;
import com.examportal.user.UserService;
import com.examportal.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ResultController
 * CHANGE: Added:
 *   GET /results/exam/{examId}/students  — all students' results for one exam (Admin/Teacher)
 *   GET /results/exams                   — list all exams that have results (Admin/Teacher)
 *   GET /results/my                      — student's own result history
 *   GET /results/pdf/{attemptId}         — download result as PDF
 */
@RestController
@RequestMapping("/results")
@RequiredArgsConstructor
public class ResultController {

    private final EvaluationService evaluationService;
    private final ExamService examService;
    private final ReportService reportService;
    private final UserService userService;
    private final UserRepository userRepository;

    /** Individual result — accessible to the student themselves, admin, teacher */
    @GetMapping("/{attemptId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<EvaluationResult>> getResult(@PathVariable Long attemptId) {
        return ResponseEntity.ok(ApiResponse.success(evaluationService.findByAttemptId(attemptId)));
    }

    /**
     * CHANGE: All students' results for one exam — for Admin/Teacher results table.
     * Navigation: Results → Select Exam → Student list → Expand row → Subject breakdown + violations
     */
    @GetMapping("/exam/{examId}/students")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<ExamResultDetailDTO>>> getExamStudentResults(
            @PathVariable Long examId) {
        return ResponseEntity.ok(ApiResponse.success(evaluationService.getExamResultsForAdmin(examId)));
    }

    /**
     * CHANGE: List all exams that have at least one evaluated result.
     * Used for the Results → "Select Exam" dropdown in Admin/Teacher dashboards.
     */
    @GetMapping("/exams")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<ExamDTO>>> getExamsWithResults() {
        List<ExamDTO> allExams = examService.findAll();
        return ResponseEntity.ok(ApiResponse.success(allExams));
    }

    /**
     * CHANGE: Student's own result history — all exams they have completed.
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<EvaluationResult>>> getMyResults(
            @AuthenticationPrincipal UserDetails userDetails) {
        com.examportal.user.User student = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(evaluationService.findByStudent(student.getId())));
    }

    /**
     * CHANGE: Download result as PDF.
     * PDF generation is LIVE (features.pdf=true in application.yml).
     * Returns the PDF bytes as application/pdf attachment.
     */
    @GetMapping("/pdf/{attemptId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'TEACHER')")
    public ResponseEntity<byte[]> downloadResultPdf(@PathVariable Long attemptId) {
        byte[] pdfBytes = reportService.generateResultPdf(attemptId);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=result_" + attemptId + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdfBytes);
    }
}
