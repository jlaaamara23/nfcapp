package com.nfcattendance.dto;

import com.nfcattendance.entity.EmploymentStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

public final class EmployeeDtos {
    private EmployeeDtos() {}

    public record EmployeeRequest(
            @NotBlank String employeeNumber,
            @NotBlank String fullName,
            Long departmentId,
            String jobTitle,
            @NotBlank @Email String email,
            String phone,
            Long workplaceId,
            String username,
            String password,
            Boolean sendInvitation
    ) {}

    public record EmployeeUpdateRequest(
            @NotBlank String fullName,
            Long departmentId,
            String jobTitle,
            @NotBlank @Email String email,
            String phone,
            Long workplaceId
    ) {}

    public record StatusUpdateRequest(
            @NotNull EmploymentStatus status
    ) {}

    public record PasswordResetRequest(
            @NotBlank String newPassword
    ) {}

    @Builder
    public record EmployeeResponse(
            Long id,
            String employeeNumber,
            String fullName,
            Long departmentId,
            String departmentName,
            String jobTitle,
            String email,
            String phone,
            EmploymentStatus employmentStatus,
            Long workplaceId,
            String workplaceName,
            Long userId,
            String username,
            boolean hasLogin
    ) {}
}
