package com.examportal.auth;

import com.examportal.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.examportal.user.User;
import com.examportal.user.UserRepository;
import org.springframework.security.core.Authentication;

/**
 * AuthController - Public authentication endpoints.
 * POST /api/auth/register - Register a new user (STUDENT or TEACHER).
 * POST /api/auth/login    - Authenticate and receive a JWT token.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Registration successful", authService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Login successful", authService.login(request)));
    }
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            String email = jwtService.extractUsername(jwt);
            User user = userRepository.findByEmail(email)
                    .orElse(null);
            if (user != null) {
                user.setLoggedIn(false);
                user.setSessionToken(null);
                userRepository.save(user);
            }
        }
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }
}
