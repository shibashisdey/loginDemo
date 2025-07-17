package com.example.login.service;

import com.example.login.model.AuditLog;
import com.example.login.repo.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @Test
    void testLog_createsAndSavesAuditLog() {
        // Arrange
        String email = "test@example.com";
        String action = "LOGIN";
        String details = "User logged in successfully";

        // Act
        auditLogService.log(email, action, details);

        // Assert
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals(email, savedLog.getEmail());
        assertEquals(action, savedLog.getAction());
        assertEquals(details, savedLog.getDetails());
        assertNotNull(savedLog.getTimestamp());
        assertTrue(savedLog.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(2)));
    }
}
