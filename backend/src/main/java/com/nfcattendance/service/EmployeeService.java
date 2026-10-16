package com.nfcattendance.service;

import com.nfcattendance.dto.EmployeeDtos;
import com.nfcattendance.entity.*;
import com.nfcattendance.exception.ApiException;
import com.nfcattendance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final WorkplaceRepository workplaceRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<EmployeeDtos.EmployeeResponse> list(EmploymentStatus status, Long departmentId, String q) {
        Specification<Employee> spec = Specification.where(null);
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("employmentStatus"), status));
        }
        if (departmentId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("department").get("id"), departmentId));
        }
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fullName")), like),
                    cb.like(cb.lower(root.get("employeeNumber")), like),
                    cb.like(cb.lower(root.get("email")), like)
            ));
        }
        return employeeRepository.findAll(spec).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EmployeeDtos.EmployeeResponse get(Long id) {
        return toResponse(findEmployee(id));
    }

    @Transactional
    public EmployeeDtos.EmployeeResponse create(EmployeeDtos.EmployeeRequest request) {
        if (employeeRepository.existsByEmployeeNumber(request.employeeNumber())) {
            throw new ApiException(HttpStatus.CONFLICT, "מספר עובד כבר קיים");
        }
        if (employeeRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "אימייל עובד כבר קיים");
        }

        Employee employee = Employee.builder()
                .employeeNumber(request.employeeNumber().trim())
                .fullName(request.fullName().trim())
                .jobTitle(request.jobTitle())
                .email(request.email().trim().toLowerCase())
                .phone(request.phone())
                .employmentStatus(EmploymentStatus.ACTIVE)
                .department(resolveDepartment(request.departmentId()))
                .workplace(resolveWorkplace(request.workplaceId()))
                .build();

        boolean createCredentials = request.username() != null && !request.username().isBlank()
                && request.password() != null && !request.password().isBlank();
        boolean sendInvitation = Boolean.TRUE.equals(request.sendInvitation());

        if (createCredentials) {
            employee.setUser(createEmployeeUser(request.username(), request.email(), request.password()));
        } else if (sendInvitation) {
            employee.setInvitationToken(UUID.randomUUID().toString());
            employee.setInvitationExpires(Instant.now().plus(7, ChronoUnit.DAYS));
            String tempPassword = UUID.randomUUID().toString().substring(0, 12);
            String username = request.username() != null && !request.username().isBlank()
                    ? request.username()
                    : request.employeeNumber().toLowerCase();
            employee.setUser(createEmployeeUser(username, request.email(), tempPassword));
        }

        employee = employeeRepository.save(employee);
        auditService.log("EMPLOYEE_CREATE", "Employee", String.valueOf(employee.getId()),
                "נוצר עובד " + employee.getEmployeeNumber());
        return toResponse(employee);
    }

    @Transactional
    public EmployeeDtos.EmployeeResponse update(Long id, EmployeeDtos.EmployeeUpdateRequest request) {
        Employee employee = findEmployee(id);
        employee.setFullName(request.fullName().trim());
        employee.setJobTitle(request.jobTitle());
        employee.setEmail(request.email().trim().toLowerCase());
        employee.setPhone(request.phone());
        employee.setDepartment(resolveDepartment(request.departmentId()));
        employee.setWorkplace(resolveWorkplace(request.workplaceId()));
        employee = employeeRepository.save(employee);
        auditService.log("EMPLOYEE_UPDATE", "Employee", String.valueOf(id), "עודכנו פרטי עובד");
        return toResponse(employee);
    }

    @Transactional
    public EmployeeDtos.EmployeeResponse updateStatus(Long id, EmployeeDtos.StatusUpdateRequest request) {
        Employee employee = findEmployee(id);
        employee.setEmploymentStatus(request.status());
        if (employee.getUser() != null) {
            boolean enabled = request.status() == EmploymentStatus.ACTIVE;
            employee.getUser().setEnabled(enabled);
            if (!enabled) {
                employee.getUser().setLocked(true);
            } else {
                employee.getUser().setLocked(false);
                employee.getUser().setLockedUntil(null);
                employee.getUser().setFailedLoginCount(0);
            }
            userAccountRepository.save(employee.getUser());
        }
        employee = employeeRepository.save(employee);
        auditService.log("EMPLOYEE_STATUS", "Employee", String.valueOf(id), "סטטוס: " + request.status());
        return toResponse(employee);
    }

    @Transactional
    public void resetPassword(Long id, EmployeeDtos.PasswordResetRequest request) {
        Employee employee = findEmployee(id);
        if (employee.getUser() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "לעובד אין חשבון התחברות");
        }
        employee.getUser().setPasswordHash(passwordEncoder.encode(request.newPassword()));
        employee.getUser().setFailedLoginCount(0);
        employee.getUser().setLocked(false);
        employee.getUser().setLockedUntil(null);
        userAccountRepository.save(employee.getUser());
        auditService.log("EMPLOYEE_PASSWORD_RESET", "Employee", String.valueOf(id), "איפוס סיסמה");
    }

    @Transactional
    public void revokeAccess(Long id) {
        Employee employee = findEmployee(id);
        if (employee.getUser() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "לעובד אין חשבון התחברות");
        }
        employee.getUser().setEnabled(false);
        employee.getUser().setLocked(true);
        userAccountRepository.save(employee.getUser());
        auditService.log("EMPLOYEE_REVOKE", "Employee", String.valueOf(id), "בוטל גישה");
    }

    public Employee findEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "עובד לא נמצא"));
    }

    public Employee requireActiveEmployeeForUser(Long userId) {
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "אין פרופיל עובד מקושר"));
        if (employee.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "חשבון העובד אינו פעיל");
        }
        return employee;
    }

    private UserAccount createEmployeeUser(String username, String email, String password) {
        if (userAccountRepository.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "שם משתמש כבר קיים");
        }
        if (userAccountRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "אימייל משתמש כבר קיים");
        }
        Role employeeRole = roleRepository.findByName(RoleName.EMPLOYEE)
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "תפקיד EMPLOYEE חסר"));
        UserAccount user = UserAccount.builder()
                .username(username.trim())
                .email(email.trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(password))
                .enabled(true)
                .roles(new HashSet<>(List.of(employeeRole)))
                .build();
        return userAccountRepository.save(user);
    }

    private Department resolveDepartment(Long id) {
        if (id == null) {
            return null;
        }
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "מחלקה לא נמצאה"));
    }

    private Workplace resolveWorkplace(Long id) {
        if (id == null) {
            return null;
        }
        return workplaceRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "מיקום עבודה לא נמצא"));
    }

    private EmployeeDtos.EmployeeResponse toResponse(Employee e) {
        return EmployeeDtos.EmployeeResponse.builder()
                .id(e.getId())
                .employeeNumber(e.getEmployeeNumber())
                .fullName(e.getFullName())
                .departmentId(e.getDepartment() != null ? e.getDepartment().getId() : null)
                .departmentName(e.getDepartment() != null ? e.getDepartment().getName() : null)
                .jobTitle(e.getJobTitle())
                .email(e.getEmail())
                .phone(e.getPhone())
                .employmentStatus(e.getEmploymentStatus())
                .workplaceId(e.getWorkplace() != null ? e.getWorkplace().getId() : null)
                .workplaceName(e.getWorkplace() != null ? e.getWorkplace().getName() : null)
                .userId(e.getUser() != null ? e.getUser().getId() : null)
                .username(e.getUser() != null ? e.getUser().getUsername() : null)
                .hasLogin(e.getUser() != null)
                .build();
    }
}
