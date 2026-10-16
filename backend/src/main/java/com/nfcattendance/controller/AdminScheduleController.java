package com.nfcattendance.controller;

import com.nfcattendance.dto.ScheduleDtos;
import com.nfcattendance.service.ScheduleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/schedules")
@RequiredArgsConstructor
@Tag(name = "Admin Schedules")
public class AdminScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping
    public List<ScheduleDtos.ScheduleResponse> list(@RequestParam(required = false) Long employeeId) {
        return scheduleService.list(employeeId);
    }

    @PostMapping
    public ScheduleDtos.ScheduleResponse create(@Valid @RequestBody ScheduleDtos.ScheduleRequest request) {
        return scheduleService.create(request);
    }

    @PatchMapping("/{id}/deactivate")
    public Map<String, String> deactivate(@PathVariable Long id) {
        scheduleService.deactivate(id);
        return Map.of("message", "לוח הזמנים בוטל");
    }
}
