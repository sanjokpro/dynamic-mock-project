package com.dynamicmock.infrastructure.config;

import com.dynamicmock.domain.entity.User;
import com.dynamicmock.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class UserBootstrapRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.auth.admin-username:admin}")
    private String adminUsername;

    @Value("${app.auth.admin-password:admin}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() == 0) {
            String normalizedUsername = adminUsername.toLowerCase().trim();
            if ("admin".equals(adminPassword)) {
                log.warn("*** DEFAULT ADMIN CREDENTIALS CREATED (admin/admin). Set ADMIN_PASSWORD before production deployment! ***");
            }

            User admin = User.builder()
                    .id(UUID.randomUUID().toString())
                    .username(normalizedUsername)
                    .email(normalizedUsername + "@dynamicmock.local")
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            userRepository.save(admin);
            log.info("Bootstrapped initial administrator user: '{}' (ID: {})", admin.getUsername(), admin.getId());
        }
    }
}
