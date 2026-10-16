package com.nfcattendance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "attendance_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRules {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workplace_id", unique = true)
    private Workplace workplace;

    @Column(name = "grace_minutes", nullable = false)
    @Builder.Default
    private int graceMinutes = 10;

    @Column(name = "overtime_after_minutes", nullable = false)
    @Builder.Default
    private int overtimeAfterMinutes = 480;

    @Column(name = "allow_overnight", nullable = false)
    @Builder.Default
    private boolean allowOvernight = true;

    @Column(name = "duplicate_window_seconds", nullable = false)
    @Builder.Default
    private int duplicateWindowSeconds = 60;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
