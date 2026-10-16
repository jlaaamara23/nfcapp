package com.nfcattendance.repository;

import com.nfcattendance.entity.AttendanceRules;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AttendanceRulesRepository extends JpaRepository<AttendanceRules, Long> {
    Optional<AttendanceRules> findByWorkplaceId(Long workplaceId);
}
