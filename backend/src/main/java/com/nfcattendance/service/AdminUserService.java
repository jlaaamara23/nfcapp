package com.nfcattendance.service;

import com.nfcattendance.dto.AdminUserDtos;
import com.nfcattendance.entity.Role;
import com.nfcattendance.entity.RoleName;
import com.nfcattendance.entity.UserAccount;
import com.nfcattendance.exception.ApiException;
import com.nfcattendance.repository.RoleRepository;
import com.nfcattendance.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<AdminUserDtos.AdminUserResponse> listAdmins() {
        return userAccountRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ADMIN))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdminUserDtos.AdminUserResponse createAdmin(AdminUserDtos.CreateAdminRequest request) {
        if (userAccountRepository.existsByUsername(request.username())) {
            throw new ApiException(HttpStatus.CONFLICT, "שם משתמש כבר קיים");
        }
        if (userAccountRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "אימייל כבר קיים");
        }
        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "תפקיד ADMIN חסר"));
        UserAccount user = UserAccount.builder()
                .username(request.username().trim())
                .email(request.email().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .enabled(true)
                .roles(new HashSet<>(List.of(adminRole)))
                .build();
        user = userAccountRepository.save(user);
        auditService.log("ADMIN_CREATE", "User", String.valueOf(user.getId()), "נוצר מנהל");
        return toResponse(user);
    }

    @Transactional
    public AdminUserDtos.AdminUserResponse setEnabled(Long id, boolean enabled) {
        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "משתמש לא נמצא"));
        user.setEnabled(enabled);
        user = userAccountRepository.save(user);
        auditService.log(enabled ? "ADMIN_ENABLE" : "ADMIN_DISABLE", "User", String.valueOf(id), null);
        return toResponse(user);
    }

    private AdminUserDtos.AdminUserResponse toResponse(UserAccount u) {
        return AdminUserDtos.AdminUserResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .enabled(u.isEnabled())
                .locked(u.isLocked())
                .roles(u.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet()))
                .build();
    }
}
