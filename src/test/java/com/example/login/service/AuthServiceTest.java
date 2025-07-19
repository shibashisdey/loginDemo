package com.example.login.service;

import com.example.login.dto.*;
import com.example.login.model.*;
import com.example.login.repo.*;
import com.example.login.security.JwtBlacklistService;
import com.example.login.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private VerificationTokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private EmailService emailService;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private JwtBlacklistService jwtBlacklistService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest sampleRegisterRequest;

    @BeforeEach
    void setUp() {
        sampleRegisterRequest = new RegisterRequest();
        sampleRegisterRequest.setName("John Doe");
        sampleRegisterRequest.setEmail("johndoe@example.com");
        sampleRegisterRequest.setPassword("secure123");
        sampleRegisterRequest.setGender("Male");
        sampleRegisterRequest.setPhone("1234567890");
        sampleRegisterRequest.setDob("2000-01-01");
        sampleRegisterRequest.setHeight(175.0);
    }

    @Test
    void testRegister_successful() {
        when(userRepository.findByEmail("johndoe@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secure123")).thenReturn("hashedPassword");

        ResponseEntity<String> response = authService.register(sampleRegisterRequest);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("User registered"));

        verify(userRepository, times(1)).save(any(User.class));
        verify(tokenRepository, times(1)).save(any(VerificationToken.class));

        // Updated to match new method signature
        verify(emailService, times(1))
                .sendVerificationEmail(eq("johndoe@example.com"), eq("John Doe"), anyString());

        verify(auditLogService, times(1))
                .log(eq("johndoe@example.com"), eq("REGISTER"), anyString());
    }

    @Test
    void testLogin_successful() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("johndoe@example.com");
        loginRequest.setPassword("secure123");

        User user = User.builder()
                .email("johndoe@example.com")
                .password("hashedPassword")
                .roles(Set.of("ROLE_USER"))
                .enabled(true)
                .build();

        when(userRepository.findByEmail("johndoe@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(eq("johndoe@example.com"), anyCollection())).thenReturn("mocked-jwt");

        // No exception from authManager.authenticate
        doNothing().when(authenticationManager).authenticate(any());

        ResponseEntity<?> response = authService.login(loginRequest);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("accessToken"));

        verify(refreshTokenRepository).deleteByUser(user);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(auditLogService).log(eq("johndoe@example.com"), eq("LOGIN"), anyString());
    }



    @Test
    void testLogout_successful() {
        User user = User.builder().email("johndoe@example.com").build();
        RefreshToken token = new RefreshToken();
        token.setToken("refresh-token-123");
        token.setUser(user);

        String accessToken = "access-token-abc";

        when(refreshTokenRepository.findByToken("refresh-token-123")).thenReturn(Optional.of(token));

        // Mock jwtUtil.extractExpiration to return some Date in the future
        Date expiryDate = Date.from(Instant.now().plusSeconds(3600)); // 1 hour later
        when(jwtUtil.extractExpiration(accessToken)).thenReturn(expiryDate);

        // Call logout with both tokens
        ResponseEntity<String> response = authService.logout("refresh-token-123", accessToken);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Logged out successfully", response.getBody());

        verify(refreshTokenRepository).delete(token);
        verify(auditLogService).log(eq("johndoe@example.com"), eq("LOGOUT"), anyString());
        verify(jwtBlacklistService).blacklistToken(accessToken, expiryDate.toInstant());
    }



}
