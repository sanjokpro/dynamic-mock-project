package com.dynamicmock.application.service;

import com.dynamicmock.adapter.in.web.dto.LoginRequest;
import com.dynamicmock.adapter.in.web.dto.LoginResponse;
import com.dynamicmock.adapter.in.web.dto.RegisterRequest;
import com.dynamicmock.adapter.in.web.dto.UserResponse;
import com.dynamicmock.domain.entity.User;
import com.dynamicmock.domain.port.out.UserRepository;
import com.dynamicmock.infrastructure.security.JwtTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    @Value("${app.auth.allow-registration:false}")
    private boolean allowRegistration;

    public LoginResponse login(LoginRequest request) {
        String normalizedUsername = request.getUsername() != null ? request.getUsername().toLowerCase().trim() : "";
        User user = userRepository.findByUsername(normalizedUsername).orElse(null);

        // Uniform authentication check to prevent user enumeration
        if (user == null || Boolean.FALSE.equals(user.getActive()) ||
                !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Failed login attempt for username '{}'", request.getUsername());
            throw new BadCredentialsException("Invalid username or password");
        }

        String token = jwtTokenService.generateToken(user.getId(), user.getUsername());
        log.info("User '{}' successfully authenticated", user.getUsername());

        return LoginResponse.builder()
                .token(token)
                .expiresInSeconds(jwtTokenService.getExpirySeconds())
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }

    public UserResponse register(RegisterRequest request) {
        if (!allowRegistration) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Registration is disabled. Set APP_AUTH_ALLOW_REGISTRATION=true to enable.");
        }

        String normalizedUsername = request.getUsername().toLowerCase().trim();
        String normalizedEmail = request.getEmail().toLowerCase().trim();

        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .username(normalizedUsername)
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User saved = userRepository.save(user);
        log.info("Registered new user '{}' ({})", saved.getUsername(), saved.getId());
        return UserResponse.from(saved);
    }

    public UserResponse getCurrentUser(String username) {
        String normalizedUsername = username != null ? username.toLowerCase().trim() : "";
        User user = userRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return UserResponse.from(user);
    }
}
