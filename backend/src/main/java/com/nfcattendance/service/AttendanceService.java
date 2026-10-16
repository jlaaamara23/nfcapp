package com.nfcattendance.service;

import com.nfcattendance.config.AppProperties;
import com.nfcattendance.dto.AttendanceDtos;
import com.nfcattendance.entity.*;
import com.nfcattendance.exception.ApiException;
import com.nfcattendance.repository.AttendanceRecordRepository;
import com.nfcattendance.repository.AttendanceRulesRepository;
import com.nfcattendance.repository.NfcStationRepository;
import com.nfcattendance.repository.UserAccountRepository;
import com.nfcattendance.repository.WorkScheduleRepository;
import com.nfcattendance.security.UserPrincipal;
import com.nfcattendance.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final NfcStationRepository nfcStationRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final AttendanceRulesRepository attendanceRulesRepository;
    private final EmployeeService employeeService;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;
    private final AppProperties appProperties;

    @Transactional
    public AttendanceDtos.AttendanceResponse clockIn(AttendanceDtos.ClockRequest request) {
        return clock(AttendanceEventType.CLOCK_IN, request);
    }

    @Transactional
    public AttendanceDtos.AttendanceResponse clockOut(AttendanceDtos.ClockRequest request) {
        return clock(AttendanceEventType.CLOCK_OUT, request);
    }

    private AttendanceDtos.AttendanceResponse clock(AttendanceEventType eventType, AttendanceDtos.ClockRequest request) {
        UserPrincipal principal = SecurityUtils.currentUser();
        Employee employee = employeeService.requireActiveEmployeeForUser(principal.getId());

        if (request.clientRequestId() != null && !request.clientRequestId().isBlank()) {
            var existing = attendanceRecordRepository.findByEmployeeIdAndClientRequestId(employee.getId(), request.clientRequestId());
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }

        NfcStation station = nfcStationRepository.findByStationCode(request.stationCode().trim())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "תחנת NFC לא מוכרת"));
        if (station.getStatus() != NfcStationStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "תחנת NFC אינה פעילה");
        }

        Instant now = Instant.now();

        AttendanceRecord open = attendanceRecordRepository
                .findByEmployeeIdAndOpenSessionTrueAndEventType(employee.getId(), AttendanceEventType.CLOCK_IN)
                .orElse(null);

        if (eventType == AttendanceEventType.CLOCK_IN) {
            if (open != null) {
                throw new ApiException(HttpStatus.CONFLICT, "כבר בוצעה כניסה פתוחה. יש לבצע יציאה תחילה");
            }
        }

        Instant duplicateFrom = now.minusSeconds(resolveDuplicateWindow(station.getWorkplace()));
        if (attendanceRecordRepository.countRecentEvents(employee.getId(), eventType, duplicateFrom) > 0) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "בקשת נוכחות כפולה בחלון זמן קצר");
        }

        if (eventType == AttendanceEventType.CLOCK_IN) {
            AttendanceRecord record = AttendanceRecord.builder()
                    .employee(employee)
                    .nfcStation(station)
                    .eventType(AttendanceEventType.CLOCK_IN)
                    .serverTimestamp(now)
                    .openSession(true)
                    .status(AttendanceStatus.VALID)
                    .clientRequestId(blankToNull(request.clientRequestId()))
                    .lateMinutes(calculateLateMinutes(employee, now))
                    .build();
            if (record.getLateMinutes() != null && record.getLateMinutes() > 0) {
                record.setStatus(AttendanceStatus.LATE);
            }
            record = attendanceRecordRepository.save(record);
            auditService.log("CLOCK_IN", "AttendanceRecord", String.valueOf(record.getId()),
                    "station=" + station.getStationCode());
            return toResponse(record);
        }

        if (open == null) {
            throw new ApiException(HttpStatus.CONFLICT, "אין כניסה פתוחה לביצוע יציאה");
        }

        long worked = Duration.between(open.getServerTimestamp(), now).toMinutes();
        if (worked < 0) {
            worked = 0;
        }
        Integer early = calculateEarlyDeparture(employee, now);

        AttendanceRecord out = AttendanceRecord.builder()
                .employee(employee)
                .nfcStation(station)
                .eventType(AttendanceEventType.CLOCK_OUT)
                .serverTimestamp(now)
                .openSession(false)
                .pairedRecord(open)
                .workedMinutes((int) worked)
                .earlyDepartureMinutes(early)
                .status(early != null && early > 0 ? AttendanceStatus.EARLY_DEPARTURE : AttendanceStatus.VALID)
                .clientRequestId(blankToNull(request.clientRequestId()))
                .build();
        out = attendanceRecordRepository.save(out);

        open.setOpenSession(false);
        open.setPairedRecord(out);
        open.setWorkedMinutes((int) worked);
        attendanceRecordRepository.save(open);

        auditService.log("CLOCK_OUT", "AttendanceRecord", String.valueOf(out.getId()),
                "station=" + station.getStationCode() + ", workedMinutes=" + worked);
        return toResponse(out);
    }

    @Transactional(readOnly = true)
    public List<AttendanceDtos.AttendanceResponse> myAttendance(Instant from, Instant to) {
        UserPrincipal principal = SecurityUtils.currentUser();
        Employee employee = employeeService.requireActiveEmployeeForUser(principal.getId());
        Instant fromTs = from != null ? from : Instant.now().minus(Duration.ofDays(30));
        Instant toTs = to != null ? to : Instant.now().plus(Duration.ofMinutes(1));
        return attendanceRecordRepository.findEmployeeRecordsBetween(employee.getId(), fromTs, toTs)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AttendanceDtos.TodaySummary todaySummary() {
        UserPrincipal principal = SecurityUtils.currentUser();
        Employee employee = employeeService.requireActiveEmployeeForUser(principal.getId());
        ZoneId zone = zoneFor(employee);
        ZonedDateTime start = LocalDate.now(zone).atStartOfDay(zone);
        ZonedDateTime end = start.plusDays(1);
        List<AttendanceRecord> today = attendanceRecordRepository.findEmployeeRecordsBetween(
                employee.getId(), start.toInstant(), end.toInstant());

        boolean clockedIn = attendanceRecordRepository
                .findByEmployeeIdAndOpenSessionTrueAndEventType(employee.getId(), AttendanceEventType.CLOCK_IN)
                .isPresent();
        Instant lastIn = today.stream().filter(r -> r.getEventType() == AttendanceEventType.CLOCK_IN)
                .map(AttendanceRecord::getServerTimestamp).findFirst().orElse(null);
        Instant lastOut = today.stream().filter(r -> r.getEventType() == AttendanceEventType.CLOCK_OUT)
                .map(AttendanceRecord::getServerTimestamp).findFirst().orElse(null);
        int worked = today.stream()
                .filter(r -> r.getEventType() == AttendanceEventType.CLOCK_OUT && r.getWorkedMinutes() != null)
                .mapToInt(AttendanceRecord::getWorkedMinutes)
                .sum();

        String label = clockedIn ? "במשמרת" : (lastOut != null ? "סיים משמרת" : "לא נכנס היום");
        return AttendanceDtos.TodaySummary.builder()
                .clockedIn(clockedIn)
                .lastClockIn(lastIn)
                .lastClockOut(lastOut)
                .workedMinutesToday(worked)
                .statusLabel(label)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AttendanceDtos.AttendanceResponse> adminSearch(Long employeeId, Long departmentId,
                                                               Instant from, Instant to, AttendanceStatus status) {
        Specification<AttendanceRecord> spec = Specification.where(null);
        if (employeeId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("employee").get("id"), employeeId));
        }
        if (departmentId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("employee").get("department").get("id"), departmentId));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("serverTimestamp"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("serverTimestamp"), to));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return attendanceRecordRepository.findAll(spec).stream()
                .sorted((a, b) -> b.getServerTimestamp().compareTo(a.getServerTimestamp()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AttendanceDtos.AttendanceResponse correct(Long id, AttendanceDtos.CorrectionRequest request) {
        AttendanceRecord record = attendanceRecordRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "רשומת נוכחות לא נמצאה"));
        if (request.reason() == null || request.reason().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "חובה לציין סיבת תיקון");
        }
        UserPrincipal principal = SecurityUtils.currentUser();
        UserAccount admin = userAccountRepository.findById(principal.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "לא מחובר"));

        Instant original = record.getOriginalTimestamp() != null ? record.getOriginalTimestamp() : record.getServerTimestamp();
        record.setOriginalTimestamp(original);
        record.setServerTimestamp(request.newTimestamp());
        record.setCorrectionReason(request.reason().trim());
        record.setCorrectedBy(admin);
        record.setCorrectedAt(Instant.now());
        record.setStatus(AttendanceStatus.CORRECTED);

        if (record.getEventType() == AttendanceEventType.CLOCK_OUT && record.getPairedRecord() != null) {
            long minutes = Duration.between(record.getPairedRecord().getServerTimestamp(), request.newTimestamp()).toMinutes();
            record.setWorkedMinutes((int) Math.max(minutes, 0));
            record.getPairedRecord().setWorkedMinutes(record.getWorkedMinutes());
            attendanceRecordRepository.save(record.getPairedRecord());
        }

        record = attendanceRecordRepository.save(record);
        auditService.log("ATTENDANCE_CORRECT", "AttendanceRecord", String.valueOf(id),
                "reason=" + request.reason() + ", newTimestamp=" + request.newTimestamp());
        return toResponse(record);
    }

    public AttendanceDtos.AttendanceResponse toResponse(AttendanceRecord r) {
        Employee e = r.getEmployee();
        NfcStation s = r.getNfcStation();
        String tz = e.getWorkplace() != null ? e.getWorkplace().getTimezone() : appProperties.attendance().defaultTimezone();
        return AttendanceDtos.AttendanceResponse.builder()
                .id(r.getId())
                .employeeId(e.getId())
                .employeeNumber(e.getEmployeeNumber())
                .employeeName(e.getFullName())
                .departmentName(e.getDepartment() != null ? e.getDepartment().getName() : null)
                .stationId(s != null ? s.getId() : null)
                .stationCode(s != null ? s.getStationCode() : null)
                .stationName(s != null ? s.getName() : null)
                .eventType(r.getEventType())
                .serverTimestamp(r.getServerTimestamp())
                .createdAt(r.getCreatedAt())
                .openSession(r.isOpenSession())
                .workedMinutes(r.getWorkedMinutes())
                .lateMinutes(r.getLateMinutes())
                .earlyDepartureMinutes(r.getEarlyDepartureMinutes())
                .status(r.getStatus())
                .correctionReason(r.getCorrectionReason())
                .notes(r.getNotes())
                .workplaceTimezone(tz)
                .build();
    }

    private int resolveDuplicateWindow(Workplace workplace) {
        return attendanceRulesRepository.findByWorkplaceId(workplace.getId())
                .map(AttendanceRules::getDuplicateWindowSeconds)
                .orElse(appProperties.attendance().duplicateWindowSeconds());
    }

    private Integer calculateLateMinutes(Employee employee, Instant now) {
        ZoneId zone = zoneFor(employee);
        int day = now.atZone(zone).getDayOfWeek().getValue();
        return workScheduleRepository.findByEmployeeIdAndDayOfWeekAndActiveTrue(employee.getId(), day)
                .map(schedule -> {
                    LocalDate date = now.atZone(zone).toLocalDate();
                    ZonedDateTime expected = date.atTime(schedule.getStartTime()).atZone(zone);
                    long late = Duration.between(expected, now.atZone(zone)).toMinutes() - schedule.getGraceMinutes();
                    return late > 0 ? (int) late : 0;
                })
                .orElse(null);
    }

    private Integer calculateEarlyDeparture(Employee employee, Instant now) {
        ZoneId zone = zoneFor(employee);
        int day = now.atZone(zone).getDayOfWeek().getValue();
        return workScheduleRepository.findByEmployeeIdAndDayOfWeekAndActiveTrue(employee.getId(), day)
                .map(schedule -> {
                    LocalDate date = now.atZone(zone).toLocalDate();
                    LocalTime end = schedule.getEndTime();
                    ZonedDateTime expectedEnd = date.atTime(end).atZone(zone);
                    if (schedule.isOvernight() && end.isBefore(schedule.getStartTime())) {
                        expectedEnd = expectedEnd.plusDays(1);
                    }
                    long early = Duration.between(now.atZone(zone), expectedEnd).toMinutes() - schedule.getGraceMinutes();
                    return early > 0 ? (int) early : 0;
                })
                .orElse(null);
    }

    private ZoneId zoneFor(Employee employee) {
        String tz = employee.getWorkplace() != null
                ? employee.getWorkplace().getTimezone()
                : appProperties.attendance().defaultTimezone();
        return ZoneId.of(tz);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
