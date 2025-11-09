package com.carboncredit.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "inspection_appointments")
public class InspectionAppointment {

    public enum AppointmentStatus {
        REQUESTED,
        SCHEDULED,
        COMPLETED,
        CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "appointment_id")
    private UUID id;

    @OneToOne
    @JoinColumn(name = "journey_id", nullable = false, unique = true)
    private JourneyData journey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ev_owner_id", nullable = false)
    private User evOwner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cva_id", nullable = false)
    private User cva;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    private VerificationStation station;

    @Column(name = "appointment_time")
    private LocalDateTime appointmentTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AppointmentStatus status;

    @Column(name = "cva_notes", columnDefinition = "TEXT")
    private String cvaNotes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}