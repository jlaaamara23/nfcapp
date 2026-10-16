package com.nfcattendance.controller;

import com.nfcattendance.dto.EmployeeDtos;
import com.nfcattendance.entity.EmploymentStatus;
import com.nfcattendance.service.EmployeeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/employees")
@RequiredArgsConstructor
@Tag(name = "Admin Employees")
public class AdminEmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public List<EmployeeDtos.EmployeeResponse> list(
            @RequestParam(required = false) EmploymentStatus status,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String q
    ) {
        return employeeService.list(status, departmentId, q);
    }

    @PostMapping
    public EmployeeDtos.EmployeeResponse create(@Valid @RequestBody EmployeeDtos.EmployeeRequest request) {
        return employeeService.create(request);
    }

    @GetMapping("/{id}")
    public EmployeeDtos.EmployeeResponse get(@PathVariable Long id) {
        return employeeService.get(id);
    }

    @PutMapping("/{id}")
    public EmployeeDtos.EmployeeResponse update(@PathVariable Long id,
                                                @Valid @RequestBody EmployeeDtos.EmployeeUpdateRequest request) {
        return employeeService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public EmployeeDtos.EmployeeResponse updateStatus(@PathVariable Long id,
                                                      @Valid @RequestBody EmployeeDtos.StatusUpdateRequest request) {
        return employeeService.updateStatus(id, request);
    }

    @PostMapping("/{id}/reset-password")
    public Map<String, String> resetPassword(@PathVariable Long id,
                                             @Valid @RequestBody EmployeeDtos.PasswordResetRequest request) {
        employeeService.resetPassword(id, request);
        return Map.of("message", "הסיסמה אופסה בהצלחה");
    }

    @PostMapping("/{id}/revoke-access")
    public Map<String, String> revoke(@PathVariable Long id) {
        employeeService.revokeAccess(id);
        return Map.of("message", "הגישה בוטלה");
    }
}
