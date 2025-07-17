package com.example.login.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Gender is required")
    @Pattern(regexp = "Male|Female|Other", message = "Gender must be Male, Female or Other")
    private String gender;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "\\d{10,15}", message = "Phone must be between 10 to 15 digits")
    private String phone;

    @NotBlank(message = "Date of birth is required")
    // Optional: add date format validation or use LocalDate instead of String
    private String dob;

    // Assuming height in centimeters as Double, optional field so no NotBlank
    private Double height;
}
