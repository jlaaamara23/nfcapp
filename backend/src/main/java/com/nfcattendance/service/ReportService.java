package com.nfcattendance.service;

import com.nfcattendance.dto.AttendanceDtos;
import com.nfcattendance.dto.ReportDtos;
import com.nfcattendance.entity.*;
import com.nfcattendance.repository.AttendanceRecordRepository;
import com.nfcattendance.repository.EmployeeRepository;
import com.nfcattendance.repository.NfcStationRepository;
import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final NfcStationRepository nfcStationRepository;
    private final AttendanceService attendanceService;

    @Transactional(readOnly = true)
    public ReportDtos.DashboardStats dashboard() {
        ZoneId zone = ZoneId.of("Asia/Jerusalem");
        Instant dayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant();
        Instant dayEnd = dayStart.plus(Duration.ofDays(1));
        List<AttendanceRecord> today = attendanceRecordRepository.findBetween(dayStart, dayEnd);

        long clockedInNow = attendanceRecordRepository.findAll().stream()
                .filter(AttendanceRecord::isOpenSession)
                .filter(r -> r.getEventType() == AttendanceEventType.CLOCK_IN)
                .count();
        long lateToday = today.stream().filter(r -> r.getStatus() == AttendanceStatus.LATE).count();

        Set<Long> clockedInEmployees = today.stream()
                .filter(r -> r.getEventType() == AttendanceEventType.CLOCK_IN)
                .map(r -> r.getEmployee().getId())
                .collect(Collectors.toSet());
        long activeEmployees = employeeRepository.countByEmploymentStatus(EmploymentStatus.ACTIVE);
        long missing = Math.max(0, activeEmployees - clockedInEmployees.size());

        List<AttendanceDtos.AttendanceResponse> recent = today.stream()
                .sorted((a, b) -> b.getServerTimestamp().compareTo(a.getServerTimestamp()))
                .limit(10)
                .map(attendanceService::toResponse)
                .toList();

        return ReportDtos.DashboardStats.builder()
                .activeEmployees(activeEmployees)
                .inactiveEmployees(employeeRepository.countByEmploymentStatus(EmploymentStatus.INACTIVE))
                .clockedInNow(clockedInNow)
                .lateToday(lateToday)
                .missingClockInsToday(missing)
                .totalStationsActive(nfcStationRepository.findByStatus(NfcStationStatus.ACTIVE).size())
                .recentEvents(recent)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ReportDtos.DailyReportRow> daily(LocalDate date) {
        ZoneId zone = ZoneId.of("Asia/Jerusalem");
        Instant from = date.atStartOfDay(zone).toInstant();
        Instant to = from.plus(Duration.ofDays(1));
        List<AttendanceRecord> records = attendanceRecordRepository.findBetween(from, to);
        Map<Long, List<AttendanceRecord>> byEmployee = records.stream()
                .collect(Collectors.groupingBy(r -> r.getEmployee().getId()));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm").withZone(zone);
        List<ReportDtos.DailyReportRow> rows = new ArrayList<>();
        for (Employee employee : employeeRepository.findAll()) {
            if (employee.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
                continue;
            }
            List<AttendanceRecord> empRecords = byEmployee.getOrDefault(employee.getId(), List.of());
            Instant firstIn = empRecords.stream().filter(r -> r.getEventType() == AttendanceEventType.CLOCK_IN)
                    .map(AttendanceRecord::getServerTimestamp).min(Instant::compareTo).orElse(null);
            Instant lastOut = empRecords.stream().filter(r -> r.getEventType() == AttendanceEventType.CLOCK_OUT)
                    .map(AttendanceRecord::getServerTimestamp).max(Instant::compareTo).orElse(null);
            int worked = empRecords.stream()
                    .filter(r -> r.getWorkedMinutes() != null)
                    .mapToInt(AttendanceRecord::getWorkedMinutes).sum();
            boolean late = empRecords.stream().anyMatch(r -> r.getStatus() == AttendanceStatus.LATE
                    || (r.getLateMinutes() != null && r.getLateMinutes() > 0));
            boolean early = empRecords.stream().anyMatch(r -> r.getStatus() == AttendanceStatus.EARLY_DEPARTURE
                    || (r.getEarlyDepartureMinutes() != null && r.getEarlyDepartureMinutes() > 0));
            rows.add(ReportDtos.DailyReportRow.builder()
                    .employeeNumber(employee.getEmployeeNumber())
                    .employeeName(employee.getFullName())
                    .departmentName(employee.getDepartment() != null ? employee.getDepartment().getName() : "")
                    .firstClockIn(firstIn != null ? fmt.format(firstIn) : "")
                    .lastClockOut(lastOut != null ? fmt.format(lastOut) : "")
                    .workedMinutes(worked)
                    .late(late)
                    .missingClockIn(firstIn == null)
                    .earlyDeparture(early)
                    .build());
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public List<ReportDtos.MonthlyReportRow> monthly(YearMonth month) {
        ZoneId zone = ZoneId.of("Asia/Jerusalem");
        Instant from = month.atDay(1).atStartOfDay(zone).toInstant();
        Instant to = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();
        List<AttendanceRecord> records = attendanceRecordRepository.findBetween(from, to);
        Map<Long, List<AttendanceRecord>> byEmployee = records.stream()
                .collect(Collectors.groupingBy(r -> r.getEmployee().getId()));

        List<ReportDtos.MonthlyReportRow> rows = new ArrayList<>();
        for (Employee employee : employeeRepository.findAll()) {
            List<AttendanceRecord> empRecords = byEmployee.getOrDefault(employee.getId(), List.of());
            Set<LocalDate> days = empRecords.stream()
                    .filter(r -> r.getEventType() == AttendanceEventType.CLOCK_IN)
                    .map(r -> r.getServerTimestamp().atZone(zone).toLocalDate())
                    .collect(Collectors.toSet());
            int worked = empRecords.stream().filter(r -> r.getWorkedMinutes() != null)
                    .mapToInt(AttendanceRecord::getWorkedMinutes).sum();
            int late = (int) empRecords.stream().filter(r -> r.getStatus() == AttendanceStatus.LATE
                    || (r.getLateMinutes() != null && r.getLateMinutes() > 0)).count();
            int early = (int) empRecords.stream().filter(r -> r.getStatus() == AttendanceStatus.EARLY_DEPARTURE
                    || (r.getEarlyDepartureMinutes() != null && r.getEarlyDepartureMinutes() > 0)).count();
            rows.add(ReportDtos.MonthlyReportRow.builder()
                    .employeeNumber(employee.getEmployeeNumber())
                    .employeeName(employee.getFullName())
                    .departmentName(employee.getDepartment() != null ? employee.getDepartment().getName() : "")
                    .daysPresent(days.size())
                    .totalWorkedMinutes(worked)
                    .lateCount(late)
                    .earlyDepartureCount(early)
                    .build());
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public byte[] export(String format, LocalDate from, LocalDate to) {
        ZoneId zone = ZoneId.of("Asia/Jerusalem");
        Instant fromTs = from.atStartOfDay(zone).toInstant();
        Instant toTs = to.plusDays(1).atStartOfDay(zone).toInstant();
        List<AttendanceDtos.AttendanceResponse> rows = attendanceRecordRepository.findBetween(fromTs, toTs).stream()
                .map(attendanceService::toResponse)
                .toList();
        if ("xlsx".equalsIgnoreCase(format) || "excel".equalsIgnoreCase(format)) {
            return toExcel(rows);
        }
        return toCsv(rows);
    }

    private byte[] toCsv(List<AttendanceDtos.AttendanceResponse> rows) {
        try (StringWriter sw = new StringWriter(); CSVWriter writer = new CSVWriter(sw)) {
            writer.writeNext(new String[]{
                    "id", "employeeNumber", "employeeName", "department", "eventType",
                    "serverTimestamp", "stationCode", "workedMinutes", "status", "correctionReason"
            });
            for (AttendanceDtos.AttendanceResponse r : rows) {
                writer.writeNext(new String[]{
                        String.valueOf(r.id()),
                        r.employeeNumber(),
                        r.employeeName(),
                        r.departmentName() != null ? r.departmentName() : "",
                        r.eventType().name(),
                        r.serverTimestamp().toString(),
                        r.stationCode() != null ? r.stationCode() : "",
                        r.workedMinutes() != null ? String.valueOf(r.workedMinutes()) : "",
                        r.status().name(),
                        r.correctionReason() != null ? r.correctionReason() : ""
                });
            }
            return sw.toString().getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to export CSV", e);
        }
    }

    private byte[] toExcel(List<AttendanceDtos.AttendanceResponse> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("attendance");
            Row header = sheet.createRow(0);
            String[] cols = {"ID", "Employee #", "Name", "Department", "Event", "Timestamp", "Station", "Minutes", "Status", "Correction"};
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }
            int idx = 1;
            for (AttendanceDtos.AttendanceResponse r : rows) {
                Row row = sheet.createRow(idx++);
                row.createCell(0).setCellValue(r.id());
                row.createCell(1).setCellValue(r.employeeNumber());
                row.createCell(2).setCellValue(r.employeeName());
                row.createCell(3).setCellValue(r.departmentName() != null ? r.departmentName() : "");
                row.createCell(4).setCellValue(r.eventType().name());
                row.createCell(5).setCellValue(r.serverTimestamp().toString());
                row.createCell(6).setCellValue(r.stationCode() != null ? r.stationCode() : "");
                row.createCell(7).setCellValue(r.workedMinutes() != null ? r.workedMinutes() : 0);
                row.createCell(8).setCellValue(r.status().name());
                row.createCell(9).setCellValue(r.correctionReason() != null ? r.correctionReason() : "");
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to export Excel", e);
        }
    }
}
