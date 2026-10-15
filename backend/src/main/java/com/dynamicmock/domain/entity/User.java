package com.dynamicmock.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Domain entity representing an authenticated user account.
 * Strict Clean Architecture compliance: zero framework/persistence annotations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String id;
    private String username;     // unique, lowercase
    private String email;        // unique
    private String passwordHash; // BCrypt hash - never plain text
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
