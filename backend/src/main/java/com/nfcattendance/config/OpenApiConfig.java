package com.nfcattendance.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        final String scheme = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("NFC Attendance API")
                        .description("""
                                Employee attendance API with NFC station validation.
                                NFC tag payloads are identifiers only — not secrets. The backend requires
                                authenticated JWT requests, active station checks, duplicate prevention,
                                rate limiting window, and audit logs. For stronger anti-cloning, use
                                cryptographic tags (NTAG 424 DNA), rotating challenge-response, geo-fencing,
                                and device attestation.
                                """)
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(scheme))
                .components(new Components().addSecuritySchemes(scheme,
                        new SecurityScheme()
                                .name(scheme)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
