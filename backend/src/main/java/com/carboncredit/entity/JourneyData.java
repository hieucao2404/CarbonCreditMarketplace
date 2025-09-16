package com.carboncredit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "journey_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class JourneyData {
    @Id
    @GeneratedValue
    @Column(name = "journey_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "distance_km", precision = 10, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "energy_consumed_kwh", precision = 10, scale = 2)
    private BigDecimal energyConsumedKwh;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "co2_reduced_kg", precision = 10, scale = 2)
    private BigDecimal co2ReducedKg;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "journey", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private CarbonCredit carbonCredit;
}
