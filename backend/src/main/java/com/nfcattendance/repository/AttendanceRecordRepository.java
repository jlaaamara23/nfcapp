package com.nfcattendance.repository;

import com.nfcattendance.entity.AttendanceEventType;
import com.nfcattendance.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long>, JpaSpecificationExecutor<AttendanceRecord> {

    Optional<AttendanceRecord> findByEmployeeIdAndOpenSessionTrueAndEventType(Long employeeId, AttendanceEventType eventType);

    Optional<AttendanceRecord> findByEmployeeIdAndClientRequestId(Long employeeId, String clientRequestId);

    List<AttendanceRecord> findByEmployeeIdOrderByServerTimestampDesc(Long employeeId);

    @Query("""
            SELECT a FROM AttendanceRecord a
            WHERE a.employee.id = :employeeId
              AND a.serverTimestamp >= :from
              AND a.serverTimestamp < :to
            ORDER BY a.serverTimestamp DESC
            """)
    List<AttendanceRecord> findEmployeeRecordsBetween(@Param("employeeId") Long employeeId,
                                                      @Param("from") Instant from,
                                                      @Param("to") Instant to);

    @Query("""
            SELECT COUNT(a) FROM AttendanceRecord a
            WHERE a.employee.id = :employeeId
              AND a.eventType = :eventType
              AND a.serverTimestamp >= :from
            """)
    long countRecentEvents(@Param("employeeId") Long employeeId,
                           @Param("eventType") AttendanceEventType eventType,
                           @Param("from") Instant from);

    @Query("""
            SELECT a FROM AttendanceRecord a
            WHERE a.serverTimestamp >= :from AND a.serverTimestamp < :to
            ORDER BY a.serverTimestamp DESC
            """)
    List<AttendanceRecord> findBetween(@Param("from") Instant from, @Param("to") Instant to);
}
