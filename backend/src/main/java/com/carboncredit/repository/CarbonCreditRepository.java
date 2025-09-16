package com.carboncredit.repository;

import com.carboncredit.entity.CarbonCredit;
import com.carboncredit.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CarbonCreditRepository extends JpaRepository<CarbonCredit, UUID> {
    
    List<CarbonCredit> findByUser(User user);
    
    List<CarbonCredit> findByStatus(CarbonCredit.CreditStatus status);
    
    List<CarbonCredit> findByUserAndStatus(User user, CarbonCredit.CreditStatus status);
    
    @Query("SELECT SUM(c.creditAmount) FROM CarbonCredit c WHERE c.user = :user AND c.status = 'VERIFIED'")
    BigDecimal getTotalVerifiedCreditsByUser(User user);
    
    @Query("SELECT c FROM CarbonCredit c WHERE c.status = 'PENDING' AND c.createdAt < :cutoffDate")
    List<CarbonCredit> findPendingCreditsOlderThan(LocalDateTime cutoffDate);
    
    @Query("SELECT c FROM CarbonCredit c WHERE c.status IN ('VERIFIED', 'LISTED') ORDER BY c.createdAt DESC")
    List<CarbonCredit> findAvailableCredits();
}
