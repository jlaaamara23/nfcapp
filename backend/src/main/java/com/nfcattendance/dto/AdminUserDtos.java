package com.nfcattendance.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.Set;

public final class AdminUserDtos {
    private AdminUserDtos() {}

    public record CreateAdminRequest(
            @NotBlank String username,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, message = "הסיסמה חייבת להכיל לפחות 8 תווים") String password
    ) {}

    @Builder
    public record AdminUserResponse(
            Long id,
            String username,
            String email,
            boolean enabled,
            boolean locked,
            Set<String> roles
    ) {}
}
