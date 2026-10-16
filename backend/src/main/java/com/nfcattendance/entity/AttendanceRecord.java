package com.nfcattendance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "attendance_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nfc_station_id")
    private NfcStation nfcStation;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private AttendanceEventType eventType;

    @Column(name = "server_timestamp", nullable = false)
    private Instant serverTimestamp;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "open_session", nullable = false)
    @Builder.Default
    private boolean openSession = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paired_record_id")
    private AttendanceRecord pairedRecord;

    @Column(name = "worked_minutes")
    private Integer workedMinutes;

    @Column(name = "late_minutes")
    private Integer lateMinutes;

    @Column(name = "early_departure_minutes")
    private Integer earlyDepartureMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private AttendanceStatus status = AttendanceStatus.VALID;

    @Column(name = "correction_reason", length = 500)
    private String correctionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corrected_by_user_id")
    private UserAccount correctedBy;

    @Column(name = "corrected_at")
    private Instant correctedAt;

    @Column(name = "original_timestamp")
    private Instant originalTimestamp;

    @Column(name = "client_request_id", length = 80)
    private String clientRequestId;

    @Column(length = 500)
    private String notes;
}
