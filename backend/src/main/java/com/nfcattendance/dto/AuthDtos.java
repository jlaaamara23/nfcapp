package com.nfcattendance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.util.Set;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {}

    @Builder
    public record AuthResponse(
            String accessToken,
            String tokenType,
            Long userId,
            String username,
            String email,
            Set<String> roles,
            Long employeeId,
            String fullName
    ) {}

    @Builder
    public record MeResponse(
            Long userId,
            String username,
            String email,
            Set<String> roles,
            Long employeeId,
            String fullName,
            String employeeNumber,
            String department,
            String jobTitle,
            String employmentStatus
    ) {}
}
