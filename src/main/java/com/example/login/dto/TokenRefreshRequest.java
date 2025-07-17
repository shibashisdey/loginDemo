package com.example.login.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TokenRefreshRequest {
    private String refreshToken;
}
