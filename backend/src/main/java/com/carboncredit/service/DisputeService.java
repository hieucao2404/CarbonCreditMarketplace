package com.carboncredit.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.carboncredit.entity.Dispute;
import com.carboncredit.entity.Transaction;
import com.carboncredit.entity.User; // Make sure this is your custom User entity
import com.carboncredit.entity.Dispute.DisputeStatus;
import com.carboncredit.exception.BusinessOperationException;
import com.carboncredit.exception.EntityNotFoundException;
import com.carboncredit.repository.DisputeRepository;
import com.carboncredit.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DisputeService {
    private static final Logger log = LoggerFactory.getLogger(DisputeService.class);

    @Autowired
    private DisputeRepository disputeRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ValidationService validationService;

    @Autowired TransactionService transactionService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditService auditService;

    // ================= CORE DISPUTE OPERATIONS ==============

    // Create a new dispute for a transaciton
    @Transactional
    public Dispute createDispute(UUID transactionId, User raisedBy, String reason) {
        log.info("Creating dispute for transaction {}  by user {}", transactionId, raisedBy.getUsername());

        // Delegate to TransactionService for create (maintains transaction login)
        return transactionService.createDispute(transactionId, raisedBy, reason);

    }

    // Resovle a dispute
    @Transactional
    public Dispute resolveDispute(UUID disputeId, User resolver, String resolution) {
        log.info("Resolving dispute {} by user {}", disputeId, resolution);

        // find and validate dispute
        Dispute dispute = findDisputeById(disputeId);

        // validate resolution authority
        validationService.validateDisputeResolutionAuthority(resolver);
        validationService.validateDisputeCanBeResolved(dispute);
        validationService.validateDisputeResolution(resolution);

        // update dispute
        dispute.setStatus(DisputeStatus.RESOLVED);
        dispute.setResolution(resolution);
        dispute.setResolvedAt(LocalDateTime.now());
        dispute.setResolvedBy(resolver);

        Dispute resolvedDispute = disputeRepository.save(dispute);

        // Resolv the associated transaction
        transactionService.resolveDisputedTransaction(dispute.getTransaction().getId(), resolution);

        // send notification to all parties
        Transaction transaction = dispute.getTransaction();
        notificationService.notifyDisputeResolved(transaction.getBuyer(), transaction.getSeller(), disputeId.toString(),
                resolution);

        // log audit trail
        auditService.logDisputeResolved(disputeId.toString(), resolution);

        log.info("Dispute {} resoleved successfully", disputeId);
        return resolvedDispute;
    }

    // close the dispute (admin only - for disputest that do not need resilution)
    @Transactional
    public Dispute closeDispute(UUID disputeId, User closer, String reason) {
        log.info("Closing dispute {} by user {}", disputeId, closer.getUsername());

        Dispute dispute = findDisputeById(disputeId);

        // validate authority (only admin/CVA can close)
        validationService.validateDisputeResolutionAuthority(closer);

        if (dispute.getStatus() != DisputeStatus.OPEN) {
            throw new BusinessOperationException("Only open disputes can be closed");
        }

        dispute.setStatus(DisputeStatus.CLOSED);
        dispute.setResolution("Dispute closed: " + reason);
        dispute.setResolvedAt(LocalDateTime.now());
        dispute.setResolvedBy(closer);

        Dispute closedDispute = disputeRepository.save(dispute);

        log.info("Dispute {} closed successfull", disputeId);

        return closedDispute;
    }

    // Reopen a closed/resolve dispute (admin onluy
    @Transactional
    public Dispute reopenDispute(UUID disputeId, User reopener, String reason) {
        log.info("Reopeing dispute {} by user {}", disputeId, reopener.getUsername());

        Dispute dispute = findDisputeById(disputeId);

        // validate authority (only admin can reopen)
        if (!User.UserRole.ADMIN.equals(reopener.getRole())) {
            throw new BusinessOperationException("Only ad min cna reopen disoutes");
        }
        if (dispute.getStatus() == DisputeStatus.OPEN) {
            throw new BusinessOperationException("Dispute is already open");
        }

        dispute.setStatus(DisputeStatus.OPEN);
        dispute.setResolution(null);
        dispute.getResolvedAt();

        Dispute reopenedDispute = disputeRepository.save(dispute);

        // mart transaction as dispute again
        transactionService.markAsDisputed(dispute.getTransaction().getId(), disputeId.toString());

        log.info("Dispute {} reopened successfully", disputeId);
        return reopenedDispute;
    }

    // ==================== QUERY METHODS ====================

    /**
     * Find dispute by ID
     */
    public Dispute findDisputeById(UUID disputeId) {
        validationService.validateId(disputeId, "Dispute");

        return disputeRepository.findById(disputeId)
                .orElseThrow(() -> new EntityNotFoundException("Dispute not found with ID: " + disputeId));
    }

    /**
     * Get disputes by status
     */
    @Transactional(readOnly = true)
    public Page<Dispute> getDisputesByStatus(DisputeStatus status, int page, int size) {
        validationService.validatePageParameters(page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return disputeRepository.findByStatus(status, pageable);
    }

    /**
     * Get open disputes for admin/CVA review
     */
    @Transactional(readOnly = true)
    public List<Dispute> getOpenDisputes() {
        return disputeRepository.findOpenDisputesOrderByCreatedAt();
    }

    /**
     * Get disputes involving a specific user
     */
    @Transactional(readOnly = true)
    public Page<Dispute> getUserDisputes(User user, int page, int size) {
        validationService.validateUser(user);
        validationService.validatePageParameters(page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return disputeRepository.findByUserInvolvedOrderByCreatedAtDesc(user.getId(), pageable);
    }

    /**
     * Get disputes raised by a specific user
     */
    // @Transactional(readOnly = true)
    // public Page<Dispute> getDisputesRaisedByUser(User user, int page, int size) {
    // validationService.validateUser(user);
    // validationService.validatePageParameters(page, size);

    // Pageable pageable = PageRequest.of(page, size,
    // Sort.by("createdAt").descending());
    // return disputeRepository.findByRaisedBy(user, pageable);
    // }

    /**
     * Get disputes for a specific transaction
     */
    @Transactional(readOnly = true)
    public List<Dispute> getDisputesForTransaction(UUID transactionId) {
        validationService.validateId(transactionId, "Transaction");
        return disputeRepository.findByTransactionId(transactionId);
    }

    /**
     * Get disputes resolved by a specific user
     */
    @Transactional(readOnly = true)
    public Page<Dispute> getDisputesResolvedBy(User resolver, int page, int size) {
        validationService.validateUser(resolver);
        validationService.validatePageParameters(page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("resolvedAt").descending());
        return disputeRepository.findByResolvedByOrderByResolvedAtDesc(resolver.getId(), pageable);
    }

    /**
     * Get overdue open disputes (open for more than specified days)
     */
    @Transactional(readOnly = true)
    public List<Dispute> getOverdueDisputes(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        return disputeRepository.findOverdueOpenDisputes(cutoffDate);
    }

    /**
     * Search disputes by keyword in reason or resolution
     */
    @Transactional(readOnly = true)
    public Page<Dispute> searchDisputes(String keyword, int page, int size) {
        validationService.validatePageParameters(page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return disputeRepository.findByKeyword(keyword, pageable);
    }

    // =============== ANALYTICS AND STATISTICS

    // get dispute statistics for dashboard
    @Transactional(readOnly = true)
    public Map<String, Object> getDisputeStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> stats = new HashMap<>();

        // get dipsute in date range
        List<Dispute> disputes = disputeRepository.findByDateRange(startDate, endDate);

        // count by status
        long totalDisputes = disputes.size();
        long openDisputes = disputes.stream().filter(d -> d.getStatus() == DisputeStatus.OPEN).count();
        long resolvedDisputes = disputes.stream().filter(d -> d.getStatus() == DisputeStatus.RESOLVED).count();
        long closedDisputes = disputes.stream().filter(d -> d.getStatus() == DisputeStatus.CLOSED).count();

        stats.put("totalDisputes", totalDisputes);
        stats.put("openDisputes", openDisputes);
        stats.put("resolvedDisputes", resolvedDisputes);
        stats.put("closedDisputes", closedDisputes);

        // resolution rate
        double resolutionRate = totalDisputes > 0 ? (double) (resolvedDisputes + closedDisputes) / totalDisputes * 100
                : 0.0;
        stats.put("resolutionRate", Math.round(resolutionRate * 100.0) / 100.0);

        // avertage resolution time (in hours) for resolved disputes
        double avgResolutionTime = disputes.stream().filter(d -> d.getResolvedAt() != null)
                .mapToLong(d -> java.time.Duration.between(d.getCreatedAt(), d.getResolvedAt()).toHours()).average()
                .orElse(0.0);

        stats.put("avgResolutionTimeHours", Math.round(avgResolutionTime * 100.0) / 100.0);

        return stats;

    }

    //get dispute statistics by catefgory
    @Transactional(readOnly = true)
    public List<Object[]> getDisputeStatisticsByCategory() {
        return disputeRepository.getDisputeStatisticsByCategory();
    }

    //get dispute counts by status
    @Transactional(readOnly = true)
    public List<Object[]> getDisputeCountsByStatus() {
        return disputeRepository.countDisputesByStatus();
    }

    // ======= UTILITY METHODS ==========
    //Check if user has any open disputes

    @Transactional(readOnly = true)
    public boolean hasOpenDisputes(User user){
        validationService.validateUser(user);
        return disputeRepository.hasOpenDisputes(user.getId());
    }

    //get user's dispute count
    @Transactional(readOnly = true)
    public long getUserDisputeCount(User user) {
        validationService.validateUser(user);
        return disputeRepository.countUserDisputes(user.getId());
    }

    //get lates dispute for a transaction
    @Transactional(readOnly = true)
    public Optional<Dispute> getLatestDisputeForTransaction(UUID transactionId) {
        validationService.validateId(transactionId, "Transaction");
        return disputeRepository.findLatestDisputeForTransaction(transactionId);
    }

    // check if  user create dispute for transaction
    @Transactional
    public boolean canUserCreateDispute(UUID transactionId, User user){
        try{
            Transaction transaction = transactionService.findTransactionById(transactionId);
            validationService.validateDisputeCreationRights(transaction, user);
            validationService.validateNoExistingOpenDisputes(transactionId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    //auto closed old resolved disputes
    @Transactional
    public int autoCloseOldDisputes(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        List<Dispute> oldDisputes = disputeRepository.findUnresolvedDisputesOlderThan(cutoffDate);

        int closedCount =  0;
        for(Dispute dispute : oldDisputes) {
            if(dispute.getStatus() == DisputeStatus.RESOLVED) {
                dispute.setStatus(DisputeStatus.CLOSED);
                disputeRepository.save(dispute);
                closedCount++;
            }
        }
        log.info("Auto-closed {} old resovled disputes", closedCount);
        return closedCount;
    }

}
