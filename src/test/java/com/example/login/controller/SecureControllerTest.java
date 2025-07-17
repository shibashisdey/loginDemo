package com.example.login.controller;

import com.example.login.model.User;
import com.example.login.model.AuditLog;
import com.example.login.dto.UserProfileRequest;
import com.example.login.repo.UserRepository;
import com.example.login.repo.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SecureControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private SecureController secureController;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }


    @Test
    void getAllUsers_shouldReturnListOfUsers() {
        // Arrange
        User user1 = User.builder()
                .name("John")
                .email("john@example.com")
                .build();
        user1.setId(1L);

        User user2 = User.builder()
                .name("Jane")
                .email("jane@example.com")
                .build();
        user2.setId(2L);

        List<User> mockUsers = Arrays.asList(user1, user2);
        when(userRepository.findAll()).thenReturn(mockUsers);

        // Act
        ResponseEntity<List<User>> response = secureController.getAllUsers();

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(2, Objects.requireNonNull(response.getBody()).size());
        assertEquals("John", response.getBody().get(0).getName());
    }


    @Test
    void getAuditLogs_shouldReturnListOfAuditLogs() {
        // Arrange
        AuditLog log1 = AuditLog.builder()
                .id(1L)
                .email("user1@example.com")
                .action("LOGIN")
                .timestamp(LocalDateTime.now().minusHours(1))
                .details("User logged in")
                .build();

        AuditLog log2 = AuditLog.builder()
                .id(2L)
                .email("user2@example.com")
                .action("REGISTER")
                .timestamp(LocalDateTime.now().minusDays(1))
                .details("User registered")
                .build();

        List<AuditLog> mockLogs = Arrays.asList(log1, log2);
        when(auditLogRepository.findAll()).thenReturn(mockLogs);

        // Act
        ResponseEntity<List<AuditLog>> response = secureController.getAuditLogs();

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(2, Objects.requireNonNull(response.getBody()).size());
        assertEquals("LOGIN", response.getBody().get(0).getAction());
        assertEquals("REGISTER", response.getBody().get(1).getAction());
    }


    @Test
    void updateProfile_shouldUpdateWhenAllowed() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setName("Old Name");
        user.setPhone("1111111111");
        user.setGender("Male");
        user.setHeight(170.0);
        user.setLastProfileUpdate(LocalDateTime.now().minusMonths(2)); // eligible for update

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        UserProfileRequest req = new UserProfileRequest();
        req.setName("New Name");
        req.setPhone("9999999999");
        req.setGender("Female");
        req.setHeight(165.0);

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user@example.com");

        // Act
        ResponseEntity<String> response = secureController.updateProfile(req, auth);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Profile updated", response.getBody());

        verify(userRepository).save(user);
        assertEquals("New Name", user.getName());
        assertEquals("9999999999", user.getPhone());
        assertEquals("Female", user.getGender());
        assertEquals(165.0, user.getHeight());
    }


    @Test
    void updateProfile_shouldFailIfWithinOneMonth() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setName("Old Name");
        user.setPhone("1111111111");
        user.setGender("Male");
        user.setHeight(170.0);
        user.setLastProfileUpdate(LocalDateTime.now().minusDays(10)); // too soon to update again

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        UserProfileRequest req = new UserProfileRequest();
        req.setName("New Name");
        req.setPhone("9999999999");
        req.setGender("Female");
        req.setHeight(165.0);

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user@example.com");

        // Act
        ResponseEntity<String> response = secureController.updateProfile(req, auth);

        // Assert
        assertEquals(400, response.getStatusCodeValue());
        assertEquals("Cannot update profile within 1 month of last change", response.getBody());

        verify(userRepository, never()).save(any());
    }



    @Test
    void adminUpdateProfile_shouldUpdateUserProfile() {
        // Arrange
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setName("Old Name");
        user.setEmail("target@example.com");
        user.setPhone("1234567890");
        user.setGender("Male");
        user.setHeight(170.0);
        user.setLastProfileUpdate(LocalDateTime.now().minusDays(5)); // even though it's within 1 month, admin can override

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserProfileRequest req = new UserProfileRequest();
        req.setName("Updated Name");
        req.setPhone("9876543210");
        req.setGender("Other");
        req.setHeight(180.0);

        // Act
        ResponseEntity<String> response = secureController.adminUpdateProfile(userId, req);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("User profile updated", response.getBody());

        verify(userRepository).save(argThat(savedUser ->
                savedUser.getName().equals("Updated Name") &&
                        savedUser.getPhone().equals("9876543210") &&
                        savedUser.getGender().equals("Other") &&
                        savedUser.getHeight().equals(180.0)
        ));
    }


    // Tests will go here
}
