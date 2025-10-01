package com.carboncredit.repository;

import com.carboncredit.entity.AuditLog;
import com.carboncredit.entity.AuditLog.AuditAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for AuditLog persistence and common queries.
 *
 * Useful query methods:
 * - findByCreditIdOrderByCreatedAtDesc: get audit history for a credit, newest first.
 * - findTopByCreditIdOrderByCreatedAtDesc: get the most recent audit for a credit.
 * - findByVerifierId: get audit entries created by a verifier (CVA) user.
 * - findByAction / findByCreditIdAndActionOrderByCreatedAtDesc: filter by audit action.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Find all audit logs for a given carbon credit, newest first.
     */
    List<AuditLog> findByCreditIdOrderByCreatedAtDesc(UUID creditId);

    /**
     * Find the most recent audit entry for a given carbon credit (if any).
     */
    Optional<AuditLog> findTopByCreditIdOrderByCreatedAtDesc(UUID creditId);

    /**
     * Find audit entries created by a given verifier (user).
     */
    List<AuditLog> findByVerifierId(UUID verifierId);

    /**
     * Count audit entries created by a given verifier (user).
     */
    long countByVerifierId(UUID verifierId);

    /**
     * Find all audit entries with a specific AuditAction (e.g., VERIFIED, REJECTED).
     */
    List<AuditLog> findByAction(AuditAction action);

    /**
     * Find audit entries for a given credit filtered by action, newest first.
     */
    List<AuditLog> findByCreditIdAndActionOrderByCreatedAtDesc(UUID creditId, AuditAction action);
}
