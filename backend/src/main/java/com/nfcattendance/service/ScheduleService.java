package com.nfcattendance.service;

import com.nfcattendance.dto.ScheduleDtos;
import com.nfcattendance.entity.Employee;
import com.nfcattendance.entity.WorkSchedule;
import com.nfcattendance.entity.Workplace;
import com.nfcattendance.exception.ApiException;
import com.nfcattendance.repository.WorkScheduleRepository;
import com.nfcattendance.repository.WorkplaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final WorkScheduleRepository workScheduleRepository;
    private final EmployeeService employeeService;
    private final WorkplaceRepository workplaceRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<ScheduleDtos.ScheduleResponse> list(Long employeeId) {
        List<WorkSchedule> schedules = employeeId == null
                ? workScheduleRepository.findAll()
                : workScheduleRepository.findByEmployeeIdAndActiveTrue(employeeId);
        return schedules.stream().map(this::toResponse).toList();
    }

    @Transactional
    public ScheduleDtos.ScheduleResponse create(ScheduleDtos.ScheduleRequest request) {
        Employee employee = employeeService.findEmployee(request.employeeId());
        Workplace workplace = workplaceRepository.findById(request.workplaceId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "מיקום עבודה לא נמצא"));
        boolean overnight = Boolean.TRUE.equals(request.overnight())
                || request.endTime().isBefore(request.startTime());
        WorkSchedule schedule = WorkSchedule.builder()
                .employee(employee)
                .workplace(workplace)
                .name(request.name().trim())
                .dayOfWeek(request.dayOfWeek())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .graceMinutes(request.graceMinutes() != null ? request.graceMinutes() : 10)
                .overnight(overnight)
                .active(true)
                .build();
        schedule = workScheduleRepository.save(schedule);
        auditService.log("SCHEDULE_CREATE", "WorkSchedule", String.valueOf(schedule.getId()),
                "employee=" + employee.getEmployeeNumber());
        return toResponse(schedule);
    }

    @Transactional
    public void deactivate(Long id) {
        WorkSchedule schedule = workScheduleRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "לוח זמנים לא נמצא"));
        schedule.setActive(false);
        workScheduleRepository.save(schedule);
        auditService.log("SCHEDULE_DEACTIVATE", "WorkSchedule", String.valueOf(id), "בוטל");
    }

    private ScheduleDtos.ScheduleResponse toResponse(WorkSchedule s) {
        return ScheduleDtos.ScheduleResponse.builder()
                .id(s.getId())
                .employeeId(s.getEmployee().getId())
                .employeeName(s.getEmployee().getFullName())
                .workplaceId(s.getWorkplace().getId())
                .workplaceName(s.getWorkplace().getName())
                .name(s.getName())
                .dayOfWeek(s.getDayOfWeek())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .graceMinutes(s.getGraceMinutes())
                .overnight(s.isOvernight())
                .active(s.isActive())
                .build();
    }
}
