package com.nfcattendance.service;

import com.nfcattendance.entity.AuditLog;
import com.nfcattendance.entity.UserAccount;
import com.nfcattendance.repository.AuditLogRepository;
import com.nfcattendance.repository.UserAccountRepository;
import com.nfcattendance.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserAccountRepository userAccountRepository;

    public void log(String action, String entityType, String entityId, String details) {
        UserAccount actor = null;
        try {
            actor = userAccountRepository.findById(SecurityUtils.currentUser().getId()).orElse(null);
        } catch (Exception ignored) {
            // unauthenticated context (e.g. failed login)
        }
        AuditLog log = AuditLog.builder()
                .actor(actor)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .ipAddress(resolveIp())
                .build();
        auditLogRepository.save(log);
    }

    public Page<AuditLog> list(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    private String resolveIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
