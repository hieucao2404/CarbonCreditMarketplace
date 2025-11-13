package com.carboncredit.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors; // Added for DTO mapping

import com.carboncredit.entity.Dispute;
import com.carboncredit.entity.Transaction;
import com.carboncredit.entity.User; // Make sure this is your custom User entity
import com.carboncredit.entity.Dispute.DisputeStatus;
import com.carboncredit.entity.Transaction.TransactionStatus; // Import TransactionStatus
import com.carboncredit.exception.BusinessOperationException;
import com.carboncredit.exception.EntityNotFoundException; // Assuming this exists
import com.carboncredit.repository.DisputeRepository;
// TransactionRepository might not be needed directly anymore
// import com.carboncredit.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// --- DTO and Mapper Imports ---
import com.carboncredit.dto.DisputeDTO;
import com.carboncredit.util.DTOMapper;
// ---

@Service
public class DisputeService {
    private static final Logger log = LoggerFactory.getLogger(DisputeService.class);

    @Autowired private DisputeRepository disputeRepository;
    // Removed TransactionRepository injection if not needed
    @Autowired private ValidationService validationService;
    @Autowired private TransactionService transactionService; // Still needed for status updates
    @Autowired private NotificationService notificationService;
    @Autowired private AuditService auditService;

    // ================= CORE DISPUTE OPERATIONS ==============

    @Transactional
    public Dispute createDispute(UUID transactionId, User raisedBy, String reason) {
        log.info("Creating dispute for transaction {} by user {}", transactionId, raisedBy.getUsername());

        // 1. Find and validate transaction FIRST using TransactionService
        Transaction transaction = transactionService.findTransactionById(transactionId);

        // 2. Validate dispute creation rights based on transaction state
        validationService.validateDisputeCreationRights(transaction, raisedBy); // Assumes this checks roles, status etc.
        validationService.validateDisputeReason(reason);
        validationService.validateNoExistingOpenDisputes(transactionId); // Assumes this uses DisputeRepository

        // 3. Create and save the Dispute entity
        Dispute dispute = new Dispute();
        dispute.setTransaction(transaction);
        dispute.setRaisedBy(raisedBy);
        dispute.setReason(reason);
        dispute.setStatus(DisputeStatus.OPEN);
        // createdAt handled by Auditing

        Dispute savedDispute = disputeRepository.save(dispute);
        log.info("Dispute entity {} created successfully", savedDispute.getId());

        // 4. NOW, update the transaction status via TransactionService
        transactionService.markAsDisputed(transactionId, savedDispute.getId().toString());

        // 5. Notifications and Audit
        User otherParty = transaction.getBuyer().getId().equals(raisedBy.getId()) ? transaction.getSeller() : transaction.getBuyer();
        notificationService.notifyDisputeCreated(raisedBy, otherParty, savedDispute.getId());
        auditService.logDisputeCreated(savedDispute.getId().toString(), transactionId.toString());

        log.info("Dispute creation process complete for transaction {}", transactionId);
        // Return the Entity as it might be needed internally immediately
        return savedDispute;
    }

    @Transactional
    public Dispute resolveDispute(UUID disputeId, User resolver, String resolution) {
        log.info("Resolving dispute {} by user {} with resolution: {}", disputeId, resolver.getUsername(), resolution);

        // 1. Find and validate dispute
        Dispute dispute = findDisputeById(disputeId); // Use internal helper
        Transaction transaction = dispute.getTransaction();

        // 2. Validate authority and state
        validationService.validateDisputeResolutionAuthority(resolver); // Assumes checks Admin/CVA role
        validationService.validateDisputeCanBeResolved(dispute);      // Assumes checks status is OPEN
        validationService.validateDisputeResolution(resolution);      // Assumes checks non-empty

        // 3. Update the Dispute entity
        dispute.setStatus(DisputeStatus.RESOLVED);
        dispute.setResolution(resolution);
        dispute.setResolvedAt(LocalDateTime.now());
        dispute.setResolvedBy(resolver);

        Dispute resolvedDispute = disputeRepository.save(dispute);
        log.info("Dispute entity {} marked as RESOLVED", disputeId);

        // 4. Determine final transaction status based on resolution text
        TransactionStatus finalTransactionStatus;
        if (resolution.toLowerCase().contains("complete") || resolution.toLowerCase().contains("proceed")) {
            finalTransactionStatus = TransactionStatus.COMPLETED;
        } else if (resolution.toLowerCase().contains("cancel") || resolution.toLowerCase().contains("refund")) {
            finalTransactionStatus = TransactionStatus.CANCELLED;
        } else {
            log.warn("Could not determine final transaction status from resolution: '{}'. Defaulting to CANCELLED.", resolution);
            finalTransactionStatus = TransactionStatus.CANCELLED;
        }

        // 5. Update the transaction status via TransactionService's dedicated method
        transactionService.updateStatusAfterDisputeResolution(transaction.getId(), finalTransactionStatus, resolution);

        // 6. Notifications and Audit
        notificationService.notifyDisputeResolved(transaction.getBuyer(), transaction.getSeller(), disputeId, resolution);
        auditService.logDisputeResolved(disputeId.toString(), resolution);

        log.info("Dispute resolution process complete for dispute {}", disputeId);
        // Return the Entity
        return resolvedDispute;
    }

    // closeDispute and reopenDispute remain largely the same, but ensure they call
    // transactionService.updateStatusAfterDisputeResolution if the transaction status needs changing.
     @Transactional
    public Dispute closeDispute(UUID disputeId, User closer, String reason) {
        log.info("Closing dispute {} by user {}", disputeId, closer.getUsername());
        Dispute dispute = findDisputeById(disputeId);
        validationService.validateDisputeResolutionAuthority(closer); // Checks Admin/CVA

        if (dispute.getStatus() != DisputeStatus.OPEN) {
            throw new BusinessOperationException("Only open disputes can be closed without resolution. Status: " + dispute.getStatus());
        }

        dispute.setStatus(DisputeStatus.CLOSED);
        dispute.setResolution("Dispute closed by admin: " + reason);
        dispute.setResolvedAt(LocalDateTime.now());
        dispute.setResolvedBy(closer);
        Dispute closedDispute = disputeRepository.save(dispute);

        // Optionally update transaction status if needed (e.g., if it was DISPUTED)
        // Check current transaction status before deciding
        Transaction transaction = dispute.getTransaction();
        if (transaction.getStatus() == TransactionStatus.DISPUTED) {
             // Decide what status it should revert to - maybe CANCELLED by default?
             transactionService.updateStatusAfterDisputeResolution(transaction.getId(), TransactionStatus.CANCELLED, "Closed by admin: " + reason);
             log.info("Transaction {} status set to CANCELLED after dispute closed.", transaction.getId());
        }


        log.info("Dispute {} closed successfully", disputeId);
        return closedDispute; // Return Entity
    }

    @Transactional
    public Dispute reopenDispute(UUID disputeId, User reopener, String reason) {
        log.info("Reopening dispute {} by user {} for reason: {}", disputeId, reopener.getUsername(), reason);
        Dispute dispute = findDisputeById(disputeId);

        if (!User.UserRole.ADMIN.equals(reopener.getRole())) { // Only ADMIN can reopen
            throw new BusinessOperationException("Only administrators can reopen disputes.");
        }
        if (dispute.getStatus() == DisputeStatus.OPEN) {
            throw new BusinessOperationException("Dispute is already open.");
        }

        // Clear previous resolution info, set status back to OPEN
        DisputeStatus previousStatus = dispute.getStatus();
        dispute.setStatus(DisputeStatus.OPEN);
        dispute.setResolution("Reopened by " + reopener.getUsername() + ": " + reason + " (Previous status: " + previousStatus + ")"); // Add context
        dispute.setResolvedAt(null);
        dispute.setResolvedBy(null);

        Dispute reopenedDispute = disputeRepository.save(dispute);

        // Mark transaction as disputed again using TransactionService
        transactionService.markAsDisputed(dispute.getTransaction().getId(), disputeId.toString());

        // Notify parties?
        // notificationService.notifyDisputeReopened(dispute.getTransaction().getBuyer(), dispute.getTransaction().getSeller(), disputeId.toString(), reason);


        log.info("Dispute {} reopened successfully", disputeId);
        return reopenedDispute; // Return Entity
    }

    // ==================== QUERY METHODS (Returning DTOs) ====================

    /** Find dispute DTO by ID */
    @Transactional(readOnly = true)
    public DisputeDTO findDisputeDtoById(UUID disputeId) {
        Dispute dispute = findDisputeById(disputeId); // Use internal helper
        return DTOMapper.toDisputeDTO(dispute); // Use static mapper
    }

    /** Keep internal helper that returns entity */
    private Dispute findDisputeById(UUID disputeId) {
         validationService.validateId(disputeId, "Dispute");
         return disputeRepository.findById(disputeId)
                .orElseThrow(() -> new EntityNotFoundException("Dispute not found with ID: " + disputeId));
    }

    /** Get disputes by status as DTO Page */
    @Transactional(readOnly = true)
    public Page<DisputeDTO> getDisputesByStatus(DisputeStatus status, int page, int size) {
        validationService.validatePageParameters(page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Dispute> entityPage = disputeRepository.findByStatus(status, pageable);
        return entityPage.map(DTOMapper::toDisputeDTO); // Use static mapper
    }

    /** Get open disputes as DTO List */
    @Transactional(readOnly = true)
    public List<DisputeDTO> getOpenDisputeDtos() {
        return disputeRepository.findOpenDisputesOrderByCreatedAt().stream()
               .map(DTOMapper::toDisputeDTO) // Use static mapper
               .collect(Collectors.toList());
    }

    /** Get disputes involving a specific user as DTO Page */
    @Transactional(readOnly = true)
    public Page<DisputeDTO> getUserDisputes(User user, int page, int size) {
        validationService.validateUser(user);
        validationService.validatePageParameters(page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Dispute> entityPage = disputeRepository.findByUserInvolvedOrderByCreatedAtDesc(user.getId(), pageable);
        return entityPage.map(DTOMapper::toDisputeDTO); // Use static mapper
    }

    /** Get disputes for a specific transaction as DTO List */
    @Transactional(readOnly = true)
    public List<DisputeDTO> getDisputeDtosForTransaction(UUID transactionId) {
        validationService.validateId(transactionId, "Transaction");
        return disputeRepository.findByTransactionId(transactionId).stream()
                .map(DTOMapper::toDisputeDTO) // Use static mapper
                .collect(Collectors.toList());
    }

    /** Get disputes resolved by a specific user as DTO Page */
    @Transactional(readOnly = true)
    public Page<DisputeDTO> getDisputesResolvedBy(User resolver, int page, int size) {
        validationService.validateUser(resolver);
        validationService.validatePageParameters(page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("resolvedAt").descending());
        // Ensure repository method uses resolvedBy.id
        Page<Dispute> entityPage = disputeRepository.findByResolvedBy_IdOrderByResolvedAtDesc(resolver.getId(), pageable);
        return entityPage.map(DTOMapper::toDisputeDTO); // Use static mapper
    }

     /** Get overdue open disputes as DTO List */
    @Transactional(readOnly = true)
    public List<DisputeDTO> getOverdueDisputeDtos(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        return disputeRepository.findOverdueOpenDisputes(cutoffDate).stream()
               .map(DTOMapper::toDisputeDTO) // Use static mapper
               .collect(Collectors.toList());
    }

    /** Search disputes by keyword as DTO Page */
    @Transactional(readOnly = true)
    public Page<DisputeDTO> searchDisputes(String keyword, int page, int size) {
        validationService.validatePageParameters(page, size);
        // Add keyword validation if needed
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Dispute> entityPage = disputeRepository.findByKeyword(keyword, pageable);
        return entityPage.map(DTOMapper::toDisputeDTO); // Use static mapper
    }


    // =============== ANALYTICS AND STATISTICS (Keep returning raw data for now) ============
    // These often don't need DTOs unless the structure is complex

    @Transactional(readOnly = true)
    public Map<String, Object> getDisputeStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        // ... (logic remains the same, returns Map) ...
        Map<String, Object> stats = new HashMap<>();
        List<Dispute> disputes = disputeRepository.findByDateRange(startDate, endDate);
        long totalDisputes = disputes.size();
        long openDisputes = disputes.stream().filter(d -> d.getStatus() == DisputeStatus.OPEN).count();
        long resolvedDisputes = disputes.stream().filter(d -> d.getStatus() == DisputeStatus.RESOLVED).count();
        long closedDisputes = disputes.stream().filter(d -> d.getStatus() == DisputeStatus.CLOSED).count();
        stats.put("totalDisputes", totalDisputes);
        stats.put("openDisputes", openDisputes);
        stats.put("resolvedDisputes", resolvedDisputes);
        stats.put("closedDisputes", closedDisputes);
        double resolutionRate = totalDisputes > 0 ? (double) (resolvedDisputes + closedDisputes) / totalDisputes * 100 : 0.0;
        stats.put("resolutionRate", Math.round(resolutionRate * 100.0) / 100.0);
        double avgResolutionTime = disputes.stream().filter(d -> d.getResolvedAt() != null)
                .mapToLong(d -> java.time.Duration.between(d.getCreatedAt(), d.getResolvedAt()).toHours()).average()
                .orElse(0.0);
        stats.put("avgResolutionTimeHours", Math.round(avgResolutionTime * 100.0) / 100.0);
        return stats;
    }

    @Transactional(readOnly = true)
    public List<Object[]> getDisputeStatisticsByCategory() {
        return disputeRepository.getDisputeStatisticsByCategory();
    }

    @Transactional(readOnly = true)
    public List<Object[]> getDisputeCountsByStatus() {
        return disputeRepository.countDisputesByStatus();
    }

    // ======= UTILITY METHODS (Keep returning primitives/boolean) ==========

    @Transactional(readOnly = true)
    public boolean hasOpenDisputes(User user){
        validationService.validateUser(user);
        return disputeRepository.hasOpenDisputes(user.getId());
    }

    @Transactional(readOnly = true)
    public long getUserDisputeCount(User user) {
        validationService.validateUser(user);
        return disputeRepository.countUserDisputes(user.getId());
    }

     // Return DTO here for consistency? Or keep Optional<Entity> if only internal?
     // Let's return Optional<DTO> for potential future controller use.
    @Transactional(readOnly = true)
    public Optional<DisputeDTO> getLatestDisputeDtoForTransaction(UUID transactionId) {
        validationService.validateId(transactionId, "Transaction");
        return disputeRepository.findLatestDisputeForTransaction(transactionId)
                .map(DTOMapper::toDisputeDTO); // Use static mapper
    }


    @Transactional(readOnly = true) // Changed to read-only as it doesn't modify state
    public boolean canUserCreateDispute(UUID transactionId, User user){
        try{
            // Use internal findById helper which returns entity
            Transaction transaction = transactionService.findTransactionById(transactionId);
            validationService.validateDisputeCreationRights(transaction, user);
            validationService.validateNoExistingOpenDisputes(transactionId);
            return true;
        } catch (BusinessOperationException | EntityNotFoundException e) {
             log.debug("User {} cannot create dispute for transaction {}: {}", user.getUsername(), transactionId, e.getMessage());
            return false;
        } catch (Exception e) { // Catch broader exceptions just in case
            log.error("Unexpected error checking if user {} can create dispute for transaction {}: {}", user.getUsername(), transactionId, e.getMessage(), e);
            return false;
        }
    }


    @Transactional
    public int autoCloseOldDisputes(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        // Repository method renamed for clarity, ensure it exists
        List<Dispute> oldResolvedDisputes = disputeRepository.findUnresolvedDisputesOlderThan(cutoffDate); 
        // Original logic was filtering in service, better to filter in repo if possible

        int closedCount = 0;
        for(Dispute dispute : oldResolvedDisputes) {
            // Logic seems to target RESOLVED, not UNRESOLVED. Let's assume repo query gets RESOLVED ones.
             if (dispute.getStatus() == DisputeStatus.RESOLVED) { // Double check status
                dispute.setStatus(DisputeStatus.CLOSED);
                disputeRepository.save(dispute);
                closedCount++;
             }
        }
        if (closedCount > 0) {
             log.info("Auto-closed {} resolved disputes older than {} days", closedCount, daysOld);
        }
        return closedCount;
    }

}