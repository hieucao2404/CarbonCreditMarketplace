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
@Table(name = "certificates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Certificate {
    @Id
    @GeneratedValue
    @Column(name = "certificate_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_id")
    private CarbonCredit credit;

    @CreatedDate
    @Column(name = "issue_date")
    private LocalDateTime issueDate;

    @Column(name = "co2_reduced_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal co2ReducedKg;

    @Column(name = "certificate_code", unique = true, nullable = false, length = 50)
    private String certificateCode;
}
