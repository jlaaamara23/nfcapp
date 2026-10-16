package com.nfcattendance.controller;

import com.nfcattendance.dto.ReportDtos;
import com.nfcattendance.service.ReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@Tag(name = "Admin Reports")
public class AdminReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard")
    public ReportDtos.DashboardStats dashboard() {
        return reportService.dashboard();
    }

    @GetMapping("/daily")
    public List<ReportDtos.DailyReportRow> daily(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return reportService.daily(date);
    }

    @GetMapping("/monthly")
    public List<ReportDtos.MonthlyReportRow> monthly(
            @RequestParam String month
    ) {
        return reportService.monthly(YearMonth.parse(month));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        byte[] data = reportService.export(format, from, to);
        boolean excel = "xlsx".equalsIgnoreCase(format) || "excel".equalsIgnoreCase(format);
        String filename = excel ? "attendance.xlsx" : "attendance.csv";
        MediaType mediaType = excel
                ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                : MediaType.parseMediaType("text/csv");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(mediaType)
                .body(data);
    }
}
