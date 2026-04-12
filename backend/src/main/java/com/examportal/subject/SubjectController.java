package com.examportal.subject;

import com.examportal.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SubjectController
 * CHANGE: Added DELETE /subjects/{id} — removes subject and cascades to questions.
 */
@RestController
@RequestMapping("/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SubjectDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(subjectService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubjectDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(subjectService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SubjectDTO>> create(@Valid @RequestBody SubjectDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Subject created", subjectService.create(dto)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SubjectDTO>> update(@PathVariable Long id, @Valid @RequestBody SubjectDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Subject updated", subjectService.update(id, dto)));
    }

    /** CHANGE: Delete subject — cascades to all questions belonging to this subject */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        subjectService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Subject deleted", null));
    }
}
