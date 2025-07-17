package com.example.login.controller;
import com.example.login.dto.UserProfileRequest;
import com.example.login.exception.GlobalExceptionHandler;
import com.example.login.service.AuditLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;

import com.example.login.model.AuditLog;
import com.example.login.model.User;
import com.example.login.repo.AuditLogRepository;
import com.example.login.repo.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/secure")
public class SecureController {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditLogService auditLogService;

    public SecureController(UserRepository userRepository, AuditLogRepository auditLogRepository, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("✅ You are authenticated!");
    }

    // Admin-only endpoint to list all registered users
    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/admin/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        return ResponseEntity.ok(auditLogRepository.findAll());
    }



    @PutMapping("/profile")
    public ResponseEntity<String> updateProfile(@Valid @RequestBody UserProfileRequest req,
                                                Authentication auth) {
        User user = findUser(auth);
        if (!user.canUpdateProfile()) {
            return ResponseEntity.badRequest()
                    .body("Cannot update profile within 1 month of last change");
        }
        updateUserFields(user, req);
        user.setLastProfileUpdate(LocalDateTime.now());
        userRepository.save(user);
        auditLogService.log(user.getEmail(), "PROFILE_UPDATE", "User updated their own profile");
        return ResponseEntity.ok("Profile updated");
    }

    @PutMapping("/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> adminUpdateProfile(@PathVariable Long id,
                                                     @Valid @RequestBody UserProfileRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
        updateUserFields(user, req);
        user.setLastProfileUpdate(LocalDateTime.now());
        userRepository.save(user);
        auditLogService.log(user.getEmail(), "PROFILE_UPDATE_BY_ADMIN", "Admin updated user's profile");
        return ResponseEntity.ok("User profile updated");
    }

    // Helpers
    private User findUser(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow();
    }
    private void updateUserFields(User user, UserProfileRequest req) {
        user.setName(req.getName());
        user.setPhone(req.getPhone());
        user.setGender(req.getGender());
        user.setHeight(req.getHeight());
    }

}
