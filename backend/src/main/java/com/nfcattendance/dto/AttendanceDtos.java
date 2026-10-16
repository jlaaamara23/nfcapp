package com.nfcattendance.dto;

import com.nfcattendance.entity.AttendanceEventType;
import com.nfcattendance.entity.AttendanceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.Instant;

public final class AttendanceDtos {
    private AttendanceDtos() {}

    public record ClockRequest(
            @NotBlank String stationCode,
            String clientRequestId,
            String tagUid
    ) {}

    public record CorrectionRequest(
            @NotNull Instant newTimestamp,
            @NotBlank String reason
    ) {}

    @Builder
    public record AttendanceResponse(
            Long id,
            Long employeeId,
            String employeeNumber,
            String employeeName,
            String departmentName,
            Long stationId,
            String stationCode,
            String stationName,
            AttendanceEventType eventType,
            Instant serverTimestamp,
            Instant createdAt,
            boolean openSession,
            Integer workedMinutes,
            Integer lateMinutes,
            Integer earlyDepartureMinutes,
            AttendanceStatus status,
            String correctionReason,
            String notes,
            String workplaceTimezone
    ) {}

    @Builder
    public record TodaySummary(
            boolean clockedIn,
            Instant lastClockIn,
            Instant lastClockOut,
            Integer workedMinutesToday,
            String statusLabel
    ) {}
}
