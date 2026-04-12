package com.examportal.auth;

import com.examportal.user.Role;
import com.examportal.user.User;
import com.examportal.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * AuthService - Authentication business logic.
 * Register: hashes password, creates user. Teachers default to unapproved.
 * Login: delegates to Spring Security AuthenticationManager, then issues JWT.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }
        // Students and admins are auto-approved; teachers require admin approval
        boolean autoApproved = request.getRole() != Role.TEACHER;

        User user = User.builder()
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .fullName(request.getFullName())
            .role(request.getRole())
            .approved(autoApproved)
            .build();
        userRepository.save(user);

        return AuthResponse.builder()
            .token(jwtService.generateToken(user))
            .userId(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .role(user.getRole())
            .approved(user.isApproved())
            .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getRole() == Role.TEACHER && !user.isApproved()) {
            throw new IllegalStateException("Your teacher account is pending admin approval");
        }

        return AuthResponse.builder()
            .token(jwtService.generateToken(user))
            .userId(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .role(user.getRole())
            .approved(user.isApproved())
            .build();
    }
}