package com.nfcattendance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        Cors cors,
        Attendance attendance,
        Security security
) {
    public record Jwt(String secret, long expirationMs) {}
    public record Cors(String allowedOrigins) {}
    public record Attendance(int duplicateWindowSeconds, int defaultGraceMinutes, String defaultTimezone) {}
    public record Security(int maxFailedLogins, int lockoutMinutes) {}
}
