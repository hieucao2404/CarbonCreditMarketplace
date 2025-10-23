package com.carboncredit.repository;

import com.carboncredit.entity.Dispute;
import com.carboncredit.entity.Dispute.DisputeStatus;
import com.carboncredit.entity.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, UUID> {

       // ==================== BASIC QUERIES ====================

       /**
        * Find disputes by status
        */
       List<Dispute> findByStatus(DisputeStatus status);

       Page<Dispute> findByStatus(DisputeStatus status, Pageable pageable);

       /**
        * Find disputes raised by a specific user
        */
       List<Dispute> findByRaisedBy(User user);

       /**
        * Find disputes for a specific transaction
        */
       List<Dispute> findByTransactionId(UUID transactionId);

       /**
        * Find disputes by transaction ID and status
        */
       List<Dispute> findByTransactionIdAndStatus(UUID transactionId, DisputeStatus status);

       // ==================== COMPLEX QUERIES FOR DISPUTE MANAGEMENT
       // ====================

       /**
        * Find all disputes involving a user (as buyer, seller, or raiser)
        */
       @Query("SELECT d FROM Dispute d WHERE " +
                     "d.raisedBy.id = :userId OR " +
                     "d.transaction.buyer.id = :userId OR " +
                     "d.transaction.seller.id = :userId " +
                     "ORDER BY d.createdAt DESC")
       Page<Dispute> findByUserInvolvedOrderByCreatedAtDesc(@Param("userId") UUID userId, Pageable pageable);

       /**
        * Find open disputes for admin/CVA review
        */
       @Query("SELECT d FROM Dispute d WHERE d.status = 'OPEN' ORDER BY d.createdAt ASC")
       List<Dispute> findOpenDisputesOrderByCreatedAt();

       /**
        * Find disputes by reason pattern (for analytics)
        */
       @Query("SELECT d FROM Dispute d WHERE LOWER(d.reason) LIKE LOWER(CONCAT('%', :reasonPattern, '%')) ORDER BY d.createdAt DESC")
       List<Dispute> findByReasonContainingIgnoreCase(@Param("reasonPattern") String reasonPattern);

       /**
        * Find disputes resolved by a specific user
        */
       @Query("SELECT d FROM Dispute d WHERE d.resolvedBy = :resolverUserId AND d.status = 'RESOLVED' ORDER BY d.resolvedAt DESC")
       Page<Dispute> findByResolvedByOrderByResolvedAtDesc(@Param("resolverUserId") UUID resolverUserId,
                     Pageable pageable);

       /**
        * Find disputes created within date range
        */
       @Query("SELECT d FROM Dispute d WHERE d.createdAt BETWEEN :startDate AND :endDate ORDER BY d.createdAt DESC")
       List<Dispute> findByDateRange(@Param("startDate") LocalDateTime startDate,
                     @Param("endDate") LocalDateTime endDate);

       /**
        * Find overdue open disputes (open for more than specified days)
        */
       @Query("SELECT d FROM Dispute d WHERE d.status = 'OPEN' AND d.createdAt < :cutoffDate ORDER BY d.createdAt ASC")
       List<Dispute> findOverdueOpenDisputes(@Param("cutoffDate") LocalDateTime cutoffDate);

       /**
        * Count disputes by status for dashboard
        */
       @Query("SELECT d.status, COUNT(d) FROM Dispute d GROUP BY d.status")
       List<Object[]> countDisputesByStatus();

       /**
        * Count disputes by reason type for analytics
        */
       @Query("SELECT " +
                     "CASE " +
                     "  WHEN LOWER(d.reason) LIKE '%payment%' THEN 'Payment Issues' " +
                     "  WHEN LOWER(d.reason) LIKE '%verification%' THEN 'Verification Issues' " +
                     "  WHEN LOWER(d.reason) LIKE '%conflict%' OR LOWER(d.reason) LIKE '%listing%' THEN 'Listing Conflicts' "
                     +
                     "  WHEN LOWER(d.reason) LIKE '%security%' THEN 'Security Issues' " +
                     "  ELSE 'Other' " +
                     "END as category, " +
                     "COUNT(d) as count " +
                     "FROM Dispute d " +
                     "GROUP BY " +
                     "CASE " +
                     "  WHEN LOWER(d.reason) LIKE '%payment%' THEN 'Payment Issues' " +
                     "  WHEN LOWER(d.reason) LIKE '%verification%' THEN 'Verification Issues' " +
                     "  WHEN LOWER(d.reason) LIKE '%conflict%' OR LOWER(d.reason) LIKE '%listing%' THEN 'Listing Conflicts' "
                     +
                     "  WHEN LOWER(d.reason) LIKE '%security%' THEN 'Security Issues' " +
                     "  ELSE 'Other' " +
                     "END")
       List<Object[]> getDisputeStatisticsByCategory();

       /**
        * Find disputes for a specific transaction and user
        */
       @Query("SELECT d FROM Dispute d WHERE d.transaction.id = :transactionId AND d.raisedBy.id = :userId")
       List<Dispute> findByTransactionAndUser(@Param("transactionId") UUID transactionId, @Param("userId") UUID userId);

       /**
        * Check if user has any open disputes
        */
       @Query("SELECT COUNT(d) > 0 FROM Dispute d WHERE " +
                     "(d.raisedBy.id = :userId OR d.transaction.buyer.id = :userId OR d.transaction.seller.id = :userId) "
                     +
                     "AND d.status = 'OPEN'")
       boolean hasOpenDisputes(@Param("userId") UUID userId);

       /**
        * Find system-raised disputes (no specific user raised them)
        */
       @Query("SELECT d FROM Dispute d WHERE d.raisedBy IS NULL ORDER BY d.createdAt DESC")
       List<Dispute> findSystemRaisedDisputes();

       /**
        * Find critical disputes that need immediate attention
        */
       @Query("SELECT d FROM Dispute d WHERE LOWER(d.reason) LIKE '%critical%' OR LOWER(d.reason) LIKE '%urgent%' ORDER BY d.createdAt ASC")
       List<Dispute> findCriticalDisputes();

       /**
        * Get dispute resolution statistics for a resolver
        */
       @Query(value = "SELECT " +
                     "COUNT(d.dispute_id) as totalResolved, " +
                     "AVG(EXTRACT(EPOCH FROM (d.resolved_at - d.created_at))/3600) as avgResolutionTimeHours " +
                     "FROM disputes d " +
                     "WHERE d.resolved_by_id = :resolverUserId AND d.status = 'RESOLVED'", nativeQuery = true)
       Object[] getResolverStatistics(@Param("resolverUserId") UUID resolverUserId);

       /**
        * Find disputes that mention specific keywords
        */
       @Query("SELECT d FROM Dispute d WHERE " +
                     "LOWER(d.reason) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "LOWER(COALESCE(d.resolution, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                     "ORDER BY d.createdAt DESC")
       Page<Dispute> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

       /**
        * Find the most recent dispute for a transaction
        */
       @Query("SELECT d FROM Dispute d WHERE d.transaction.id = :transactionId ORDER BY d.createdAt DESC")
       Optional<Dispute> findLatestDisputeForTransaction(@Param("transactionId") UUID transactionId);

       /**
        * Count user's total disputes (as participant)
        */
       @Query("SELECT COUNT(d) FROM Dispute d WHERE " +
                     "d.raisedBy.id = :userId OR " +
                     "d.transaction.buyer.id = :userId OR " +
                     "d.transaction.seller.id = :userId")
       long countUserDisputes(@Param("userId") UUID userId);

       /**
        * Find disputes by status and date range
        */
       @Query("SELECT d FROM Dispute d WHERE d.status = :status AND d.createdAt BETWEEN :startDate AND :endDate ORDER BY d.createdAt DESC")
       List<Dispute> findByStatusAndDateRange(@Param("status") DisputeStatus status,
                     @Param("startDate") LocalDateTime startDate,
                     @Param("endDate") LocalDateTime endDate);

       @Query("SELECT d FROM Dispute d WHERE d.status = 'RESOLVED' AND d.resolvedAt < :cutoffDate")
       List<Dispute> findUnresolvedDisputesOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate); // Renamed for
                                                                                                     // clarity

       // Method needed by validation service potentially
       boolean existsByTransaction_IdAndStatus(UUID transactionId, DisputeStatus status);

       // Note: Parameter name MUST match @Param value
       Page<Dispute> findByResolvedBy_IdOrderByResolvedAtDesc(@Param("resolvedById") UUID resolvedById,
                     Pageable pageable);
}
