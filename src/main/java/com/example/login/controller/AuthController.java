package com.example.login.controller;

import com.example.login.dto.TokenRefreshRequest;
import com.example.login.service.AuthService;
import com.example.login.dto.LoginRequest;
import com.example.login.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verify(@Valid @RequestParam String token) {
        return authService.verifyEmail(token);
    }
    @PostMapping("/resend-verification")
    public ResponseEntity<String> resendVerificationEmail(@RequestParam String email) {
        return authService.resendVerificationEmail(email);
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        return authService.refreshToken(request);
    }
//    @PostMapping("/logout")
//    public ResponseEntity<String> logout(@RequestBody TokenRefreshRequest request) {
//        return authService.logout(request.getRefreshToken());
//    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String authHeader,
                                         @RequestBody TokenRefreshRequest request) {
        String accessToken = authHeader.replace("Bearer ", "").trim();
        return authService.logout(request.getRefreshToken(), accessToken);
    }



}
