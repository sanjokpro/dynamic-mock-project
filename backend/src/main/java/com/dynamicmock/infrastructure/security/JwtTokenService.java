package com.dynamicmock.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Slf4j
@Service
public class JwtTokenService {

    @Value("${app.auth.jwt-secret:local-dev-insecure-secret-must-be-changed-in-prod-minimum-32-chars}")
    private String jwtSecret;

    @Value("${app.auth.jwt-expiry-hours:8}")
    private long jwtExpiryHours;

    private SecretKey signingKey;

    @PostConstruct
    public void validateAndInit() {
        if (jwtSecret == null || jwtSecret.trim().length() < 32) {
            throw new IllegalStateException(
                "APP_AUTH_JWT_SECRET must be at least 32 characters for HS256.");
        }
        if (jwtSecret.contains("local-dev-insecure-secret")) {
            log.warn("*** INSECURE JWT SECRET IN USE. Set APP_AUTH_JWT_SECRET before production deployment. ***");
        }
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String userId, String username) {
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtExpiryHours, ChronoUnit.HOURS);

        return Jwts.builder()
                .subject(userId)
                .claim("username", username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public String extractUserId(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractUsername(String token) {
        return extractClaims(token).get("username", String.class);
    }

    public Date extractExpiration(String token) {
        return extractClaims(token).getExpiration();
    }

    public long getExpirySeconds() {
        return jwtExpiryHours * 3600;
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
