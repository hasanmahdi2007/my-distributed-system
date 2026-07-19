package com.hasan.gateway.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegistrationRequest(
    
    @NotBlank(message = "Company name is required and cannot be blank.")
    String companyName,

    @NotBlank(message = "Email is required.")
    @Email(message = "Must be a valid email format (e.g., admin@company.com).")
    String email,

    @NotBlank(message = "Tier type is required.")
    @Pattern(regexp = "^(FREE|PRO)$", message = "Tier type must be exactly 'FREE' or 'PRO'.")
    String tierType
) {}