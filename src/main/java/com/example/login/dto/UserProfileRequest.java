package com.example.login.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserProfileRequest {
    @NotBlank
    private String name;
    @NotBlank @Pattern(regexp = "\\d{10,15}") private String phone;
    @NotBlank @Pattern(regexp = "Male|Female|Other") private String gender;
    @NotNull
    private Double height;
}
