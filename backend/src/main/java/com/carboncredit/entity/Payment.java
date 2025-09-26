package com.carboncredit.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Payment {

    @Id
    @GeneratedValue
    @Column(name = "payment_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id")
    private User payer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payee_id")
    private User payee;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Column(name = "payment_reference", length = 50)
    private String paymentReference;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Enums for payment method and status
    public enum PaymentMethod {
        CREDIT_CARD,
        DEBIT_CARD,
        BANK_TRANSFER,
        PAYPAL,
        STRIPE,
        WALLET,
        CRYPTOCURRENCY,
        OTHER
    }

    public enum PaymentStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED,
        REFUNDED,
        DISPUTED
    }

    // Helper methods
    public boolean isCompleted() {
        return paymentStatus == PaymentStatus.COMPLETED;
    }

    public boolean isPending() {
        return paymentStatus == PaymentStatus.PENDING;
    }

    public boolean isFailed() {
        return paymentStatus == PaymentStatus.FAILED || paymentStatus == PaymentStatus.CANCELLED;
    }

    public boolean canBeRefunded() {
        return paymentStatus == PaymentStatus.COMPLETED;
    }

    public boolean isRefunded() {
        return paymentStatus == PaymentStatus.REFUNDED;
    }

    // Business logic methods
    public void markAsCompleted() {
        this.paymentStatus = PaymentStatus.COMPLETED;
    }

    public void markAsFailed() {
        this.paymentStatus = PaymentStatus.FAILED;
    }

    public void markAsRefunded() {
        if (!canBeRefunded()) {
            throw new IllegalStateException("Payment cannot be refunded in current status: " + paymentStatus);
        }
        this.paymentStatus = PaymentStatus.REFUNDED;
    }

    public void markAsDisputed() {
        this.paymentStatus = PaymentStatus.DISPUTED;
    }

    @Override
    public String toString() {
        return String.format("Payment{id=%s, amount=%s, method=%s, status=%s, reference='%s'}", 
                           id, amount, paymentMethod, paymentStatus, paymentReference);
    }
}
