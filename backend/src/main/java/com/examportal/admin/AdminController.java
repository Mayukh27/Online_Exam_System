package com.examportal.admin;

import com.examportal.common.ApiResponse;
import com.examportal.common.FeatureDisabledException;
import com.examportal.config.FeatureFlags;
import com.examportal.user.Role;
import com.examportal.user.User;
import com.examportal.user.UserDTO;
import com.examportal.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AdminController - Platform administration. All routes require ADMIN role.
 *
 * GET  /api/admin/teachers/pending   - Unapproved teacher accounts.
 * GET  /api/admin/teachers           - All teachers.
 * PUT  /api/admin/teachers/{id}/approve - Approve a teacher.
 * GET  /api/admin/stats              - Platform statistics.
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final FeatureFlags featureFlags;

    @GetMapping("/teachers/pending")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getPendingTeachers() {
        if (!featureFlags.isAdmin()) throw new FeatureDisabledException("admin");
        List<UserDTO> pending = userRepository.findByRole(Role.TEACHER).stream()
            .filter(u -> !u.isApproved())
            .map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(pending));
    }

    @GetMapping("/teachers")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getAllTeachers() {
        if (!featureFlags.isAdmin()) throw new FeatureDisabledException("admin");
        List<UserDTO> teachers = userRepository.findByRole(Role.TEACHER)
            .stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(teachers));
    }

    @PutMapping("/teachers/{id}/approve")
    public ResponseEntity<ApiResponse<UserDTO>> approveTeacher(@PathVariable Long id) {
        if (!featureFlags.isAdmin()) throw new FeatureDisabledException("admin");
        User teacher = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        if (teacher.getRole() != Role.TEACHER) {
            throw new IllegalArgumentException("User is not a teacher");
        }
        teacher.setApproved(true);
        return ResponseEntity.ok(ApiResponse.success("Teacher approved", toDTO(userRepository.save(teacher))));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        if (!featureFlags.isAnalytics()) throw new FeatureDisabledException("analytics");
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "totalStudents", userRepository.countByRole(Role.STUDENT),
            "totalTeachers", userRepository.countByRole(Role.TEACHER),
            "totalAdmins", userRepository.countByRole(Role.ADMIN)
        )));
    }

    private UserDTO toDTO(User u) {
        return UserDTO.builder().id(u.getId()).email(u.getEmail())
            .fullName(u.getFullName()).role(u.getRole())
            .approved(u.isApproved()).createdAt(u.getCreatedAt()).build();
    }
}
