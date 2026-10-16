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
