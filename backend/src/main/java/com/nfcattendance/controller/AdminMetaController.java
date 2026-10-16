package com.nfcattendance.controller;

import com.nfcattendance.dto.AdminUserDtos;
import com.nfcattendance.entity.AuditLog;
import com.nfcattendance.entity.Department;
import com.nfcattendance.entity.Workplace;
import com.nfcattendance.repository.DepartmentRepository;
import com.nfcattendance.repository.WorkplaceRepository;
import com.nfcattendance.service.AdminUserService;
import com.nfcattendance.service.AuditService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Meta")
public class AdminMetaController {

    private final DepartmentRepository departmentRepository;
    private final WorkplaceRepository workplaceRepository;
    private final AdminUserService adminUserService;
    private final AuditService auditService;

    @GetMapping("/departments")
    public List<Department> departments() {
        return departmentRepository.findByActiveTrueOrderByNameAsc();
    }

    @GetMapping("/workplaces")
    public List<Workplace> workplaces() {
        return workplaceRepository.findByActiveTrueOrderByNameAsc();
    }

    @GetMapping("/users")
    public List<AdminUserDtos.AdminUserResponse> admins() {
        return adminUserService.listAdmins();
    }

    @PostMapping("/users")
    public AdminUserDtos.AdminUserResponse createAdmin(@Valid @RequestBody AdminUserDtos.CreateAdminRequest request) {
        return adminUserService.createAdmin(request);
    }

    @PatchMapping("/users/{id}/enabled")
    public AdminUserDtos.AdminUserResponse setEnabled(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        return adminUserService.setEnabled(id, Boolean.TRUE.equals(body.get("enabled")));
    }

    @GetMapping("/audit-logs")
    public Page<AuditLog> auditLogs(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "50") int size) {
        return auditService.list(PageRequest.of(page, size));
    }
}
