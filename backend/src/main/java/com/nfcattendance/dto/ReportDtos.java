package com.nfcattendance.dto;

import lombok.Builder;

import java.util.List;

public final class ReportDtos {
    private ReportDtos() {}

    @Builder
    public record DashboardStats(
            long activeEmployees,
            long inactiveEmployees,
            long clockedInNow,
            long lateToday,
            long missingClockInsToday,
            long totalStationsActive,
            List<AttendanceDtos.AttendanceResponse> recentEvents
    ) {}

    @Builder
    public record DailyReportRow(
            String employeeNumber,
            String employeeName,
            String departmentName,
            String firstClockIn,
            String lastClockOut,
            Integer workedMinutes,
            boolean late,
            boolean missingClockIn,
            boolean earlyDeparture
    ) {}

    @Builder
    public record MonthlyReportRow(
            String employeeNumber,
            String employeeName,
            String departmentName,
            int daysPresent,
            int totalWorkedMinutes,
            int lateCount,
            int earlyDepartureCount
    ) {}
}
