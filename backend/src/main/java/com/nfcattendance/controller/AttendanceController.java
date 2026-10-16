package com.nfcattendance.controller;

import com.nfcattendance.dto.AttendanceDtos;
import com.nfcattendance.service.AttendanceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/clock-in")
    public AttendanceDtos.AttendanceResponse clockIn(@Valid @RequestBody AttendanceDtos.ClockRequest request) {
        return attendanceService.clockIn(request);
    }

    @PostMapping("/clock-out")
    public AttendanceDtos.AttendanceResponse clockOut(@Valid @RequestBody AttendanceDtos.ClockRequest request) {
        return attendanceService.clockOut(request);
    }

    @GetMapping("/me")
    public List<AttendanceDtos.AttendanceResponse> me(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        return attendanceService.myAttendance(from, to);
    }

    @GetMapping("/me/today")
    public AttendanceDtos.TodaySummary today() {
        return attendanceService.todaySummary();
    }
}
