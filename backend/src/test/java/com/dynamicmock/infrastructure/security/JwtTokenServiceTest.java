package com.dynamicmock.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    private JwtTokenService tokenService;
    private static final String TEST_SECRET = "secure-test-jwt-secret-with-more-than-32-chars-long";

    @BeforeEach
    void setUp() {
        tokenService = new JwtTokenService();
        ReflectionTestUtils.setField(tokenService, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(tokenService, "jwtExpiryHours", 8L);
        tokenService.validateAndInit();
    }

    @Test
    @DisplayName("Should generate valid JWT token with claims")
    void testGenerateAndValidateToken_Success() {
        String token = tokenService.generateToken("user-123", "alice");

        assertThat(token).isNotBlank();
        assertThat(tokenService.validateToken(token)).isTrue();
        assertThat(tokenService.extractUserId(token)).isEqualTo("user-123");
        assertThat(tokenService.extractUsername(token)).isEqualTo("alice");
        assertThat(tokenService.extractExpiration(token)).isNotNull();
    }

    @Test
    @DisplayName("Should reject invalid or tampered JWT token")
    void testValidateToken_Tampered_ReturnsFalse() {
        String token = tokenService.generateToken("user-123", "alice");
        String tamperedToken = token + "xyz";

        assertThat(tokenService.validateToken(tamperedToken)).isFalse();
    }

    @Test
    @DisplayName("Should throw IllegalStateException if secret is less than 32 characters")
    void testWeakSecret_ThrowsException() {
        JwtTokenService weakService = new JwtTokenService();
        ReflectionTestUtils.setField(weakService, "jwtSecret", "short-secret");
        ReflectionTestUtils.setField(weakService, "jwtExpiryHours", 8L);

        assertThatThrownBy(weakService::validateAndInit)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 characters");
    }
}
