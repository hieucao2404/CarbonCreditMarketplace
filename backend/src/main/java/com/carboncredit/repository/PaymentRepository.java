// src/main/java/com/carboncredit/repository/PaymentRepository.java
package com.carboncredit.repository;

import com.carboncredit.entity.Payment;
import com.carboncredit.entity.Payment.PaymentStatus;
import com.carboncredit.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /**
     * Find all payments by payer (user who made the payment)
     */
    Page<Payment> findByPayerOrderByCreatedAtDesc(User payer, Pageable pageable);

    /**
     * Find all payments by payee (user who received the payment)
     */
    Page<Payment> findByPayeeOrderByCreatedAtDesc(User payee, Pageable pageable);

    /**
     * Find all payments for a user (either payer or payee)
     */
    @Query("SELECT p FROM Payment p WHERE p.payer = :user OR p.payee = :user ORDER BY p.createdAt DESC")
    Page<Payment> findAllPaymentsByUser(@Param("user") User user, Pageable pageable);

    /**
     * Find payments by status
     */
    List<Payment> findByPaymentStatus(PaymentStatus status);

    /**
     * Find payment by reference number
     */
    Optional<Payment> findByPaymentReference(String paymentReference);

    /**
     * Find pending payments for a user
     */
    @Query("SELECT p FROM Payment p WHERE p.payer = :user AND p.paymentStatus = 'PENDING' ORDER BY p.createdAt DESC")
    List<Payment> findPendingPaymentsByUser(@Param("user") User user);

    // /**
    // * Find payments by transaction ID
    // */
    // @Query("SELECT p FROM Payment p WHERE p.transaction.transactionId =
    // :transactionId")
    // List<Payment> findByTransactionId(@Param("transactionId") UUID
    // transactionId);
    /**
     * Find payments by transaction (for marketplace purchases)
     * Note: Will return empty list for wallet deposits (they have no transaction)
     */
    // @Query("SELECT p FROM Payment p WHERE p.transaction IS NOT NULL AND p.transaction.transactionId = :transactionId")
    // List<Payment> findByTransactionId(@Param("transactionId") UUID transactionId);

    /**
     * Find payments within date range
     */
    @Query("SELECT p FROM Payment p WHERE p.createdAt BETWEEN :startDate AND :endDate ORDER BY p.createdAt DESC")
    List<Payment> findPaymentsBetweenDates(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Count payments by status for a user
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.payer = :user AND p.paymentStatus = :status")
    long countPaymentsByUserAndStatus(@Param("user") User user, @Param("status") PaymentStatus status);

    /**
     * Get total amount paid by user
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.payer = :user AND p.paymentStatus = 'COMPLETED'")
    BigDecimal getTotalAmountPaidByUser(@Param("user") User user);

    /**
     * Get total amount received by user
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.payee = :user AND p.paymentStatus = 'COMPLETED'")
    BigDecimal getTotalAmountReceivedByUser(@Param("user") User user);

    /**
     * Check if payment reference exists (prevent duplicates)
     */
    boolean existsByPaymentReference(String paymentReference);
}
