package com.example.login.service;

import com.example.login.dto.*;
import com.example.login.model.*;
import com.example.login.repo.*;
import com.example.login.security.JwtBlacklistService;
import com.example.login.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Set<String> ADMIN_EMAILS = Set.of(
            "admin1@example.com",
            "admin2@example.com"
    );

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtBlacklistService jwtBlacklistService;
    private final AuditLogService auditLogService;

    // ✅ Register User
    public ResponseEntity<String> register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already registered");
        }

        Set<String> roles = new HashSet<>();
        if (ADMIN_EMAILS.contains(request.getEmail().toLowerCase())) {
            roles.add("ROLE_ADMIN");
        } else {
            roles.add("ROLE_USER");
        }
        System.out.println("Registering user with roles: " + roles);

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .gender(request.getGender())
                .phone(request.getPhone())
                .dob(LocalDate.parse(request.getDob()))
                .height(request.getHeight())
                .roles(roles)
                .enabled(false)
                .build();

        userRepository.save(user);
        // ✅ Log the registration event
        auditLogService.log(request.getEmail(), "REGISTER", "User registered");

        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = VerificationToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(15))
                .sentAt(LocalDateTime.now())
                .build();

        tokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(user.getEmail(), user.getName(), token);


        return ResponseEntity.ok("User registered. Check your email to verify.");
    }

    // ✅ Verify Email
    public ResponseEntity<String> verifyEmail(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElse(null);

        if (verificationToken == null || verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Invalid or expired token");
        }

        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);
        tokenRepository.delete(verificationToken);

        return ResponseEntity.ok("Email verified. You can now log in.");
    }

    // ✅ Login User
    public ResponseEntity<?> login(LoginRequest request) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.getEmail(),
                request.getPassword()
        ));

        // ✅ Find the user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        // ✅ Log login
        auditLogService.log(request.getEmail(), "LOGIN", "User logged in successfully");
        // ✅ Map roles to authorities
        Collection<GrantedAuthority> authorities = user.getRoles().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        System.out.println("Generating JWT for user: " + request.getEmail() + ", authorities: " + authorities);

        // ✅ Generate JWT with roles
        String accessToken = jwtUtil.generateToken(request.getEmail(), authorities);

        // ✅ Delete old refresh token
        refreshTokenRepository.deleteByUser(user);

        // ✅ Create new refresh token
        String refreshTokenString = UUID.randomUUID().toString();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(refreshTokenString);
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plus(Duration.ofDays(7))); // 7 days

        refreshTokenRepository.save(refreshToken);

        JwtResponse response = new JwtResponse(accessToken, refreshTokenString);
        return ResponseEntity.ok(response);
    }

    // ✅ Refresh Token
    public ResponseEntity<?> refreshToken(TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenRepository.findByToken(requestRefreshToken)
                .map(refreshToken -> {
                    if (refreshToken.isExpired()) {
                        refreshTokenRepository.delete(refreshToken);
                        return ResponseEntity.badRequest().body("Refresh token expired. Please login again.");
                    }
                    User user = refreshToken.getUser();

                    // ✅ Map roles again for refreshed token
                    Collection<GrantedAuthority> authorities = user.getRoles().stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    String newAccessToken = jwtUtil.generateToken(user.getEmail(), authorities);

                    return ResponseEntity.ok(new JwtResponse(newAccessToken, requestRefreshToken));
                })
                .orElseGet(() -> ResponseEntity.badRequest().body("Invalid refresh token."));
    }
    public ResponseEntity<String> logout(String refreshToken,String accessToken) {
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByToken(refreshToken);
        if (tokenOpt.isPresent()) {
            User user = tokenOpt.get().getUser();

            // ✅ Log logout event
            auditLogService.log(user.getEmail(), "LOGOUT", "User logged out successfully");
            // ✅ Invalidate refresh token
            refreshTokenRepository.delete(tokenOpt.get());

            // ✅ Extract expiration and blacklist the token
            Date expiryDate = jwtUtil.extractExpiration(accessToken);
            jwtBlacklistService.blacklistToken(accessToken, expiryDate.toInstant());
            return ResponseEntity.ok("Logged out successfully");
        } else {
            return ResponseEntity.badRequest().body("Invalid refresh token");
        }

    }

    public ResponseEntity<String> resendVerificationEmail(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        User user = userOpt.get();
        if (user.isEnabled()) {
            return ResponseEntity.badRequest().body("Account already verified");
        }

        Optional<VerificationToken> tokenOpt = tokenRepository.findByUser(user);
        VerificationToken verificationToken;

        if (tokenOpt.isPresent()) {
            verificationToken = tokenOpt.get();

            if (verificationToken.getSentAt() != null &&
                    verificationToken.getSentAt().isAfter(LocalDateTime.now().minusMinutes(5))) {
                return ResponseEntity.badRequest()
                        .body("Verification email already sent. Please wait before resending.");
            }

            // Update expiry and sentAt
            verificationToken.setExpiryDate(LocalDateTime.now().plusMinutes(15));
            verificationToken.setSentAt(LocalDateTime.now());
        } else {
            // create new token if not present
            String token = UUID.randomUUID().toString();
            verificationToken = VerificationToken.builder()
                    .token(token)
                    .user(user)
                    .expiryDate(LocalDateTime.now().plusMinutes(15))
                    .sentAt(LocalDateTime.now())
                    .build();
        }
        tokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(user.getEmail(), user.getName(), verificationToken.getToken());


        return ResponseEntity.ok("Verification email resent. Please check your inbox.");
    }



}
