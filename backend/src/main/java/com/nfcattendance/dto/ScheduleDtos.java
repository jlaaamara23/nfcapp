package com.nfcattendance.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalTime;

public final class ScheduleDtos {
    private ScheduleDtos() {}

    public record ScheduleRequest(
            @NotNull Long employeeId,
            @NotNull Long workplaceId,
            @NotBlank String name,
            @Min(1) @Max(7) int dayOfWeek,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime,
            Integer graceMinutes,
            Boolean overnight
    ) {}

    @Builder
    public record ScheduleResponse(
            Long id,
            Long employeeId,
            String employeeName,
            Long workplaceId,
            String workplaceName,
            String name,
            int dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            int graceMinutes,
            boolean overnight,
            boolean active
    ) {}
}
