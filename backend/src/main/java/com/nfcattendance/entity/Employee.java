package com.nfcattendance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private UserAccount user;

    @Column(name = "employee_number", nullable = false, unique = true, length = 40)
    private String employeeNumber;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "job_title", length = 120)
    private String jobTitle;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(length = 40)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false, length = 30)
    @Builder.Default
    private EmploymentStatus employmentStatus = EmploymentStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workplace_id")
    private Workplace workplace;

    @Column(name = "invitation_token", length = 120)
    private String invitationToken;

    @Column(name = "invitation_expires")
    private Instant invitationExpires;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
