package com.dynamicmock.controller;

import com.dynamicmock.adapter.in.web.AuthController;
import com.dynamicmock.adapter.in.web.dto.LoginRequest;
import com.dynamicmock.adapter.in.web.dto.LoginResponse;
import com.dynamicmock.adapter.in.web.dto.RegisterRequest;
import com.dynamicmock.adapter.in.web.dto.UserResponse;
import com.dynamicmock.application.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /api/auth/login should return 200 with token on valid credentials")
    void testLogin_Success() throws Exception {
        LoginRequest request = new LoginRequest("admin", "password123");
        LoginResponse response = LoginResponse.builder()
                .token("mock-jwt-token")
                .expiresInSeconds(28800)
                .userId("user-1")
                .username("admin")
                .email("admin@dynamicmock.local")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.userId").value("user-1"));
    }

    @Test
    @DisplayName("POST /api/auth/login should return 401 with uniform error on invalid credentials")
    void testLogin_InvalidCredentials_ReturnsUniform401() throws Exception {
        LoginRequest request = new LoginRequest("admin", "wrong-password");
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid username or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid username or password"));
    }

    @Test
    @DisplayName("POST /api/auth/register should return 201 on valid registration")
    void testRegister_Success() throws Exception {
        RegisterRequest request = new RegisterRequest("newuser", "newuser@example.com", "securePassword123");
        UserResponse response = UserResponse.builder()
                .id("new-id")
                .username("newuser")
                .email("newuser@example.com")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.id").value("new-id"));
    }

    @Test
    @DisplayName("GET /api/auth/me should return current user profile when authenticated")
    void testGetCurrentUser_Success() throws Exception {
        Principal principal = () -> "admin";
        UserResponse response = UserResponse.builder()
                .id("user-1")
                .username("admin")
                .email("admin@dynamicmock.local")
                .active(true)
                .build();

        when(authService.getCurrentUser("admin")).thenReturn(response);

        mockMvc.perform(get("/api/auth/me").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.id").value("user-1"));
    }

    @Test
    @DisplayName("GET /api/auth/me should return 401 when principal is missing")
    void testGetCurrentUser_NoPrincipal_Returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
