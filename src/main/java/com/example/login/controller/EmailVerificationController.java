package com.example.login.controller;

import com.example.login.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final AuthService authService;

    // Email verification endpoint that returns a Thymeleaf view
    @GetMapping("/verify")
    public String verifyEmail(@RequestParam String token, Model model) {
        ResponseEntity<String> response = authService.verifyEmail(token);
        model.addAttribute("message", response.getBody());
        model.addAttribute("success", response.getStatusCode().is2xxSuccessful());
        return "verification-result";
    }

    // Resend verification email API returning REST response
    @PostMapping("/resend-verification")
    @ResponseBody
    public ResponseEntity<String> resendVerificationEmail(@RequestParam @Valid String email) {
        return authService.resendVerificationEmail(email);
    }
}
