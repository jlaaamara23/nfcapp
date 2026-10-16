package com.nfcattendance.repository;

import com.nfcattendance.entity.WorkSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, Long> {
    List<WorkSchedule> findByEmployeeIdAndActiveTrue(Long employeeId);
    Optional<WorkSchedule> findByEmployeeIdAndDayOfWeekAndActiveTrue(Long employeeId, int dayOfWeek);
}
