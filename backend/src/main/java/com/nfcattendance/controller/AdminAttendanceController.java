package com.nfcattendance.controller;

import com.nfcattendance.dto.AttendanceDtos;
import com.nfcattendance.entity.AttendanceStatus;
import com.nfcattendance.service.AttendanceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/admin/attendance")
@RequiredArgsConstructor
@Tag(name = "Admin Attendance")
public class AdminAttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping
    public List<AttendanceDtos.AttendanceResponse> search(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) AttendanceStatus status
    ) {
        return attendanceService.adminSearch(employeeId, departmentId, from, to, status);
    }

    @PostMapping("/{id}/correct")
    public AttendanceDtos.AttendanceResponse correct(@PathVariable Long id,
                                                     @Valid @RequestBody AttendanceDtos.CorrectionRequest request) {
        return attendanceService.correct(id, request);
    }
}
