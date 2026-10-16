package com.nfcattendance.config;

import com.nfcattendance.entity.*;
import com.nfcattendance.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final WorkplaceRepository workplaceRepository;
    private final NfcStationRepository nfcStationRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final AttendanceRulesRepository attendanceRulesRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userAccountRepository.findByUsername("admin").isPresent()) {
            return;
        }
        if (roleRepository.findByName(RoleName.ADMIN).isEmpty()
                || departmentRepository.findByCode("PROD").isEmpty()
                || workplaceRepository.findByCode("TLV-HQ").isEmpty()) {
            log.info("Skipping seed — base reference data not present");
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
        Role employeeRole = roleRepository.findByName(RoleName.EMPLOYEE).orElseThrow();
        Department hr = departmentRepository.findByCode("HR").orElseThrow();
        Department prod = departmentRepository.findByCode("PROD").orElseThrow();
        Workplace hq = workplaceRepository.findByCode("TLV-HQ").orElseThrow();

        UserAccount admin = UserAccount.builder()
                .username("admin")
                .email("admin@example.com")
                .passwordHash(passwordEncoder.encode("Admin123!"))
                .enabled(true)
                .roles(new HashSet<>(List.of(adminRole)))
                .build();
        userAccountRepository.save(admin);

        UserAccount empUser = UserAccount.builder()
                .username("employee1")
                .email("dana@example.com")
                .passwordHash(passwordEncoder.encode("Employee123!"))
                .enabled(true)
                .roles(new HashSet<>(List.of(employeeRole)))
                .build();
        empUser = userAccountRepository.save(empUser);

        Employee employee = Employee.builder()
                .user(empUser)
                .employeeNumber("E1001")
                .fullName("דנה כהן")
                .department(prod)
                .jobTitle("מפעילת ייצור")
                .email("dana@example.com")
                .phone("050-1234567")
                .employmentStatus(EmploymentStatus.ACTIVE)
                .workplace(hq)
                .build();
        employee = employeeRepository.save(employee);

        Employee employee2 = Employee.builder()
                .employeeNumber("E1002")
                .fullName("יוסי לוי")
                .department(hr)
                .jobTitle("רכז משאבי אנוש")
                .email("yossi@example.com")
                .phone("050-7654321")
                .employmentStatus(EmploymentStatus.ACTIVE)
                .workplace(hq)
                .build();
        UserAccount emp2User = UserAccount.builder()
                .username("employee2")
                .email("yossi@example.com")
                .passwordHash(passwordEncoder.encode("Employee123!"))
                .enabled(true)
                .roles(new HashSet<>(List.of(employeeRole)))
                .build();
        emp2User = userAccountRepository.save(emp2User);
        employee2.setUser(emp2User);
        employeeRepository.save(employee2);

        NfcStation station = NfcStation.builder()
                .stationCode("GATE-01")
                .name("שער כניסה ראשי")
                .workplace(hq)
                .registrationValue("NFC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase())
                .status(NfcStationStatus.ACTIVE)
                .build();
        nfcStationRepository.save(station);

        for (int day = 1; day <= 5; day++) {
            workScheduleRepository.save(WorkSchedule.builder()
                    .employee(employee)
                    .workplace(hq)
                    .name("משמרת בוקר")
                    .dayOfWeek(day)
                    .startTime(LocalTime.of(8, 0))
                    .endTime(LocalTime.of(17, 0))
                    .graceMinutes(10)
                    .overnight(false)
                    .active(true)
                    .build());
        }

        attendanceRulesRepository.save(AttendanceRules.builder()
                .workplace(hq)
                .graceMinutes(10)
                .overtimeAfterMinutes(480)
                .allowOvernight(true)
                .duplicateWindowSeconds(60)
                .build());

        log.info("Seeded development users: admin/Admin123!, employee1/Employee123!, employee2/Employee123!");
        log.info("Seeded NFC station GATE-01 with registration value {}", station.getRegistrationValue());
    }
}
