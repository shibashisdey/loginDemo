package com.example.login.controller;

import com.example.login.dto.JwtResponse;
import com.example.login.dto.LoginRequest;
import com.example.login.dto.RegisterRequest;
import com.example.login.dto.TokenRefreshRequest;
import com.example.login.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.ResponseEntity;


import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testRegister() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("password123");
        request.setGender("Male");
        request.setPhone("1234567890");
        request.setDob("1990-01-01");
        request.setHeight(175.0);

        Mockito.when(authService.register(any(RegisterRequest.class)))
                .thenReturn(ResponseEntity.ok("User registered. Check your email to verify."));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered. Check your email to verify."));
    }

    @Test
    void testLogin() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("password123");

        JwtResponse jwtResponse = new JwtResponse("accessToken123", "refreshToken123");
        ResponseEntity<JwtResponse> responseEntity = ResponseEntity.ok(jwtResponse);

        Mockito.<ResponseEntity<?>>when(authService.login(any(LoginRequest.class)))
                .thenReturn(responseEntity);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("accessToken123"))
                .andExpect(jsonPath("$.refreshToken").value("refreshToken123"));
    }



    @Test
    void testLogout() throws Exception {
        TokenRefreshRequest request = new TokenRefreshRequest("refreshToken123");

        Mockito.when(authService.logout("refreshToken123"))
                .thenReturn(ResponseEntity.ok("Logged out successfully"));

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Logged out successfully"));
    }
}
