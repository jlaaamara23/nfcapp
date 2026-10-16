package com.nfcattendance.service;

import com.nfcattendance.config.AppProperties;
import com.nfcattendance.dto.AuthDtos;
import com.nfcattendance.entity.Employee;
import com.nfcattendance.entity.UserAccount;
import com.nfcattendance.exception.ApiException;
import com.nfcattendance.repository.EmployeeRepository;
import com.nfcattendance.repository.UserAccountRepository;
import com.nfcattendance.security.JwtService;
import com.nfcattendance.security.UserPrincipal;
import com.nfcattendance.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserAccountRepository userAccountRepository;
    private final EmployeeRepository employeeRepository;
    private final JwtService jwtService;
    private final AppProperties appProperties;
    private final AuditService auditService;

    @Transactional
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        UserAccount user = userAccountRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("bad credentials"));

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new LockedException("locked");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

            user.setFailedLoginCount(0);
            user.setLocked(false);
            user.setLockedUntil(null);
            user.setLastLoginAt(Instant.now());
            userAccountRepository.save(user);

            String token = jwtService.generateToken(principal);
            Employee employee = employeeRepository.findByUserId(user.getId()).orElse(null);

            auditService.log("LOGIN", "User", String.valueOf(user.getId()), "התחברות מוצלחת");

            return AuthDtos.AuthResponse.builder()
                    .accessToken(token)
                    .tokenType("Bearer")
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .roles(principal.getRoleNames().stream().map(Enum::name).collect(Collectors.toSet()))
                    .employeeId(employee != null ? employee.getId() : null)
                    .fullName(employee != null ? employee.getFullName() : user.getUsername())
                    .build();
        } catch (BadCredentialsException ex) {
            int failures = user.getFailedLoginCount() + 1;
            user.setFailedLoginCount(failures);
            if (failures >= appProperties.security().maxFailedLogins()) {
                user.setLocked(true);
                user.setLockedUntil(Instant.now().plus(appProperties.security().lockoutMinutes(), ChronoUnit.MINUTES));
            }
            userAccountRepository.save(user);
            auditService.log("LOGIN_FAILED", "User", String.valueOf(user.getId()), "ניסיון התחברות כושל");
            throw ex;
        }
    }

    public void logout() {
        UserPrincipal principal = SecurityUtils.currentUser();
        auditService.log("LOGOUT", "User", String.valueOf(principal.getId()), "התנתקות");
    }

    @Transactional(readOnly = true)
    public AuthDtos.MeResponse me() {
        UserPrincipal principal = SecurityUtils.currentUser();
        UserAccount user = userAccountRepository.findById(principal.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "משתמש לא נמצא"));
        Employee employee = employeeRepository.findByUserId(user.getId()).orElse(null);

        return AuthDtos.MeResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(principal.getRoleNames().stream().map(Enum::name).collect(Collectors.toSet()))
                .employeeId(employee != null ? employee.getId() : null)
                .fullName(employee != null ? employee.getFullName() : user.getUsername())
                .employeeNumber(employee != null ? employee.getEmployeeNumber() : null)
                .department(employee != null && employee.getDepartment() != null ? employee.getDepartment().getName() : null)
                .jobTitle(employee != null ? employee.getJobTitle() : null)
                .employmentStatus(employee != null ? employee.getEmploymentStatus().name() : null)
                .build();
    }
}
