package com.carboncredit.repository;

import com.carboncredit.entity.Transaction;
import com.carboncredit.entity.Transaction.TransactionStatus;
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
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

        Page<Transaction> findByBuyer(User buyer, Pageable pageable);

        Page<Transaction> findBySeller(User seller, Pageable pageable);

        Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);

        Page<Transaction> findByBuyerOrSeller(User buyer, User seller, Pageable pageable);

        List<Transaction> findByBuyerAndStatus(User buyer, Transaction.TransactionStatus status);

        List<Transaction> findBySellerAndStatus(User seller, Transaction.TransactionStatus status);

        @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.seller = :seller AND t.status = 'COMPLETED'")
        BigDecimal getTotalEarningsBySeller(User seller);

        @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.buyer = :buyer AND t.status = 'COMPLETED'")
        BigDecimal getTotalSpendingsByBuyer(User buyer);

        @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :start AND :end ORDER BY t.createdAt DESC")
        List<Transaction> findTransactionsBetween(LocalDateTime start, LocalDateTime end);

        // ======== Methods for service
        /**
         * Find transactions for both buyer and seller (for user profile)
         */
        @Query("SELECT t FROM Transaction t WHERE t.buyer = :user OR t.seller = :user ORDER BY t.createdAt DESC")
        List<Transaction> findByBuyerOrSellerOrderByCreatedAtDesc(@Param("user") User user1, @Param("user") User user2);

        /**
         * Find pending transaction for a specific listing (prevent concurrent
         * purchases)
         */
        @Query("SELECT t FROM Transaction t WHERE t.listing.id = :listingId AND t.status = 'PENDING'")
        Optional<Transaction> findPendingTransactionForListing(@Param("listingId") UUID listingId);

        /**
         * Count recent transactions by user (for security/fraud detection)
         */
        @Query("SELECT COUNT(t) FROM Transaction t WHERE t.buyer.id = :userId AND t.createdAt > :since")
        long countRecentTransactionsByUser(@Param("userId") UUID userId, @Param("since") LocalDateTime since);

        /**
         * Count today's transactions by user (daily limit check)
         */
        @Query("SELECT COUNT(t) FROM Transaction t WHERE t.buyer.id = :userId AND t.createdAt >= :startOfDay AND t.createdAt < :endOfDay")
        long countTodayTransactionsByUser(@Param("userId") UUID userId, @Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

        /**
         * Get daily spending by user (daily limit check)
         */
        @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.buyer.id = :userId AND t.createdAt >= :startOfDay AND t.createdAt < :endOfDay AND t.status = 'COMPLETED'")
        BigDecimal getDailySpentByUser(@Param("userId") UUID userId, @Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

        /**
         * Get average transaction amount by user (fraud detection)
         */
        @Query("SELECT AVG(t.amount) FROM Transaction t WHERE t.buyer.id = :userId AND t.status = 'COMPLETED'")
        Optional<BigDecimal> getAverageTransactionAmountByUser(@Param("userId") UUID userId);

        /**
         * Find transactions by credit ID
         */
        @Query("SELECT t FROM Transaction t WHERE t.credit.id = :creditId ORDER BY t.createdAt DESC")
        List<Transaction> findByCreditId(@Param("creditId") UUID creditId);

        /**
         * Find transactions by listing ID
         */
        @Query("SELECT t FROM Transaction t WHERE t.listing.id = :listingId ORDER BY t.createdAt DESC")
        List<Transaction> findByListingId(@Param("listingId") UUID listingId);

        /**
         * Find transactions with payment reference
         */
        @Query("SELECT t FROM Transaction t JOIN Payment p ON p.transaction.id = t.id WHERE p.paymentReference = :paymentReference")
        Optional<Transaction> findByPaymentReference(@Param("paymentReference") String paymentReference);

        /**
         * Find completed transactions in date range for reporting
         */
        @Query("SELECT t FROM Transaction t WHERE t.status = 'COMPLETED' AND t.completedAt BETWEEN :startDate AND :endDate ORDER BY t.completedAt DESC")
        List<Transaction> findCompletedTransactionsBetween(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        /**
         * Find disputed transactions for admin review
         */
        @Query("SELECT t FROM Transaction t WHERE t.status = 'DISPUTED' ORDER BY t.createdAt ASC")
        List<Transaction> findDisputedTransactions();

        /**
         * Get transaction statistics for dashboard
         */
        @Query("SELECT " +
                        "COUNT(t) as totalCount, " +
                        "COUNT(CASE WHEN t.status = 'COMPLETED' THEN 1 END) as completedCount, " +
                        "COUNT(CASE WHEN t.status = 'DISPUTED' THEN 1 END) as disputedCount, " +
                        "COUNT(CASE WHEN t.status = 'CANCELLED' THEN 1 END) as cancelledCount, " +
                        "COALESCE(SUM(CASE WHEN t.status = 'COMPLETED' THEN t.amount END), 0) as totalVolume " +
                        "FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate")
        Object[] getTransactionStatistics(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        /**
         * Find high-value transactions for monitoring
         */
        @Query("SELECT t FROM Transaction t WHERE t.amount > :threshold AND t.status = 'COMPLETED' ORDER BY t.amount DESC")
        List<Transaction> findHighValueTransactions(@Param("threshold") BigDecimal threshold);

        /**
         * Check if user has any pending transactions
         */
        @Query("SELECT COUNT(t) > 0 FROM Transaction t WHERE (t.buyer.id = :userId OR t.seller.id = :userId) AND t.status = 'PENDING'")
        boolean hasPendingTransactions(@Param("userId") UUID userId);

        /**
         * Find transactions by status and date range
         */
        @Query("SELECT t FROM Transaction t WHERE t.status = :status AND t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
        List<Transaction> findByStatusAndDateRange(@Param("status") Transaction.TransactionStatus status,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        // Count methods for statistics
        @Query("SELECT COUNT(t) FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate")
        long countByDateRange(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        @Query("SELECT COUNT(t) FROM Transaction t WHERE t.status = :status AND t.createdAt BETWEEN :startDate AND :endDate")
        long countByStatusAndDateRange(@Param("status") TransactionStatus status,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        // Additional useful statistics methods
        @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.status = :status AND t.createdAt BETWEEN :startDate AND :endDate")
        BigDecimal sumAmountByStatusAndDateRange(@Param("status") TransactionStatus status,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        @Query("SELECT AVG(t.amount) FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate")
        Double averageAmountByDateRange(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate")
        Page<Transaction> findByDateRange(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        Pageable pageable);

        @Query("SELECT t FROM Transaction t WHERE t.status = :status AND t.createdAt BETWEEN :startDate AND :endDate")
        Page<Transaction> findByStatusAndDateRange(@Param("status") TransactionStatus status,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        Pageable pageable);
}
