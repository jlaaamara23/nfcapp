package com.nfcattendance.repository;

import com.nfcattendance.entity.Employee;
import com.nfcattendance.entity.EmploymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {
    Optional<Employee> findByUserId(Long userId);
    Optional<Employee> findByEmployeeNumber(String employeeNumber);
    boolean existsByEmployeeNumber(String employeeNumber);
    boolean existsByEmail(String email);
    long countByEmploymentStatus(EmploymentStatus status);
}
