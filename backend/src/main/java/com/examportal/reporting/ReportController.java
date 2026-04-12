package com.examportal.reporting;

import com.examportal.common.ApiResponse;
import com.examportal.common.FeatureDisabledException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * ReportController - PDF download endpoint.
 * GET /api/reports/result/{attemptId} - Download result as PDF.
 * Returns 503 when features.pdf=false.
 */
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/result/{attemptId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<?> downloadResult(@PathVariable Long attemptId) {
        try {
            byte[] pdfBytes = reportService.generateResultPdf(attemptId);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=result_" + attemptId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
        } catch (FeatureDisabledException e) {
            return ResponseEntity.status(503).body(ApiResponse.error(e.getMessage()));
        }
    }
}
