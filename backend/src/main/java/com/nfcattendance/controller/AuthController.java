package com.nfcattendance.controller;

import com.nfcattendance.dto.AuthDtos;
import com.nfcattendance.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    @GetMapping("/setup-status")
    @Operation(summary = "Whether the first admin account still needs to be created")
    public AuthDtos.SetupStatus setupStatus() {
        return authService.setupStatus();
    }

    @PostMapping("/bootstrap")
    @Operation(summary = "Create the first admin account when none exist")
    public AuthDtos.AuthResponse bootstrap(@Valid @RequestBody AuthDtos.BootstrapRequest request) {
        return authService.bootstrap(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT")
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout (client should discard token)")
    public void logout() {
        authService.logout();
    }

    @GetMapping("/me")
    @Operation(summary = "Current authenticated user profile")
    public AuthDtos.MeResponse me() {
        return authService.me();
    }
}
