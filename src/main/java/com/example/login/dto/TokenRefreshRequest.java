package com.example.login.dto;

import lombok.Data;

@Data
public class TokenRefreshRequest {
    private String refreshToken;
}
