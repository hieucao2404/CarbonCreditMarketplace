package com.carboncredit.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carboncredit.dto.TransactionDTO;
import com.carboncredit.entity.CarbonCredit;
import com.carboncredit.entity.CarbonCredit.CreditStatus;
import com.carboncredit.entity.CreditListing;
import com.carboncredit.entity.CreditListing.ListingStatus;
import com.carboncredit.entity.Dispute;
import com.carboncredit.entity.Dispute.DisputeStatus;
import com.carboncredit.entity.Transaction;
import com.carboncredit.entity.Transaction.TransactionStatus;
import com.carboncredit.entity.User;
import com.carboncredit.exception.BusinessOperationException;
import com.carboncredit.exception.EntityNotFoundException;
import com.carboncredit.exception.InsufficientBalanceException;
import com.carboncredit.exception.PaymentException;
import com.carboncredit.exception.UnauthorizedOperationException;
import com.carboncredit.repository.CarbonCreditRepository;
import com.carboncredit.repository.CreditListingRepository;
import com.carboncredit.repository.DisputeRepository;
import com.carboncredit.repository.TransactionRepository;
import com.carboncredit.service.PaymentService.PaymentResult;
import com.carboncredit.util.DTOMapper;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    @Autowired
    private ValidationService validationService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private DisputeRepository disputeRepository;

    @Autowired
    private CreditListingRepository creditListingRepository;

    @Autowired
    private CarbonCreditRepository carbonCreditRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private WalletService walletService;

    // ==== TRANSACTION AND PROCESSING ================

    @Transactional
    public Transaction initiatePurchase(UUID listingId, User buyer) {
        log.info("Initiating purchase for listing {} by user {}", listingId, buyer.getUsername());

        validationService.validateId(listingId, "CreditLisitng");
        validationService.validateUser(buyer);

        CreditListing listing = creditListingRepository.findById(listingId)
                .orElseThrow(() -> new EntityNotFoundException("Listing not found with ID: " + listingId));

        validationService.validatePurchaseRequest(listing, buyer);

        CarbonCredit credit = listing.getCredit();
        User seller = credit.getUser();

        // Check Buyer's Wallet for sufficienct cash BEFORE creating transaction
        if (!walletService.hasSufficientBalance(buyer.getId(), listing.getPrice(), "CASH")) {
            log.warn("Insuffiecent funds for buyer {}. Required: {}, Available: {}", buyer.getUsername(),
                    listing.getPrice(), walletService.getCashBalance(buyer.getId()));
            throw new InsufficientBalanceException("Insufficient cash balance. required: " + listing.getPrice());
        }

        Transaction transaction = new Transaction();
        transaction.setCredit(credit);
        transaction.setListing(listing);
        transaction.setBuyer(buyer);
        transaction.setSeller(seller);
        transaction.setAmount(listing.getPrice());
        transaction.setStatus(TransactionStatus.PENDING);

        Transaction savedTransaction = transactionRepository.save(transaction);

        listing.setStatus(ListingStatus.PENDING_TRANSACTION);
        creditListingRepository.save(listing);

        log.info("Transaction {} initiated, proceeding to payment.", savedTransaction.getId());
        // Trigger asynchronous payment processing or handle synchronously
        try {
            // Option 1: Synchronous Payment
            processPayment(savedTransaction);
            // If processPayment throws an exception, the @Transactional will rollback
            // everything
            // If it succeeds, completeTransaction is called within processPayment

            // Option 2: Asynchronous Payment (more complex set up)
            // paymentService.initiateAsynchronousPayment(savedTransaction);
            // log.info("Asynchronous payment initiated for transaction {}",
            // savedTransaction.getId());
        } catch (Exception e) {
            // This catch block might not be strictly needed if processPayment
            // but it's good for logging the initiation failure context.
            log.error("Payment initiation failed for transaction {}: {}", savedTransaction.getId(), e.getMessage());
            // The @Transactional rollback should handle reverting the listing status and
            // transaction.
            // Explicit rollback of listing status might be needed if processPayment error
            // isn't transactional.
            if (e instanceof PaymentException) {
                // Payment specific exception already handled or thrown by processPayment
                throw e;
            } else {
                // Wrap unexpected errors
                throw new RuntimeException("Unexpected error during payment initiation: " + e.getMessage(), e);
            }
        }
        // Return the PENDING transaction reference (caller should check status later if
        // async)
        return savedTransaction; // Or return the final completed/failed transaction if sync
    }

    @Transactional
    public void processPayment(Transaction transaction) {
        log.info("Processing payment for transaction {}", transaction.getId());
        validationService.validateTransactionSecurity(transaction);

        PaymentResult paymentResult = paymentService.processPayment(transaction.getId(),
                transaction.getAmount(), transaction.getBuyer().getId().toString(),
                transaction.getSeller().getId().toString());

        if (paymentResult.isSuccess()) {
            log.info("Payment successful for transaction {}, completing transaction.", transaction.getId());
            completeTransaction(transaction);
        } else {
            log.warn("Payment failed for transaction {}: {}", transaction.getId(), paymentResult.getErrorMessage());
            // Transactional rollback should handle failing the transaction record,
            // but we might need explicit state changes depending on PaymentService
            // behavior.
            failTransaction(transaction, "Payment failed: " + paymentResult.getErrorMessage());
            throw new PaymentException("Payment processing failed: " + paymentResult.getErrorMessage());

        }
    }

    @Transactional
    public Transaction completeTransaction(Transaction transaction) {
        log.info("Completing transaction {}", transaction.getId());

        // Re-fetch entities within this transaction for safety
        Transaction currentTransaction = findTransactionById(transaction.getId()); // Use internal helper
        CreditListing currentListing = creditListingRepository.findById(currentTransaction.getListing().getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Listing not found during completion: " + currentTransaction.getListing().getId()));
        CarbonCredit currentCredit = carbonCreditRepository.findById(currentTransaction.getCredit().getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Carbon credit not found during completion: " + currentTransaction.getCredit().getId()));

        User buyer = currentTransaction.getBuyer();
        User seller = currentTransaction.getSeller();
        BigDecimal price = currentTransaction.getAmount();
        BigDecimal creditAmount = currentCredit.getCreditAmount();

        // validate transaction preconditions
        if (currentTransaction.getStatus() != TransactionStatus.PENDING) {
            log.warn("Attempted to complete transaction {} which is not PENDING (Status: {})", transaction.getId(),
                    currentTransaction.getStatus());
            // Depending on flow, this might be okay (e.g., if re-processed) or an error.
            // For now, let's allow re-completion attempt but log warning. If it must be
            // PENDING, uncomment below:
            // throw new BusinessOperationException("Transaction is not in PENDING state.
            // Cannot complete.");
        }
        if (currentListing.getStatus() != ListingStatus.PENDING_TRANSACTION) {
            log.warn(
                    "Attempted to complete transaction {} for listing {} which is not PENDING_TRANSACTION (Status: {})",
                    transaction.getId(), currentListing.getId(), currentListing.getStatus());
            // Allow for now, but log. If strict state needed, uncomment below:
            // throw new BusinessOperationException(
            // "Listing is not in PENDING_TRANSACTION state. Cannot complete purchase.");

        }
        // --- Perform Atomic Transfers (Idempotency check recommended in WalletService)
        // ---
        log.debug("Transferring cash: {} from {} to {}", price, buyer.getUsername(), seller.getUsername());
        walletService.updateCashBalance(buyer.getId(), price.negate());
        walletService.updateCashBalance(seller.getId(), price);

        log.debug("Transferring credits: {} from {} to {}", creditAmount, seller.getUsername(), buyer.getUsername());
        walletService.updateCreditBalance(seller.getId(), creditAmount.negate());
        walletService.updateCreditBalance(buyer.getId(), creditAmount);

        // --- Update Entity Statuses ---
        currentTransaction.setStatus(TransactionStatus.COMPLETED);
        currentTransaction.setCompletedAt(LocalDateTime.now());
        currentListing.setStatus(ListingStatus.CLOSED);
        currentCredit.setStatus(CreditStatus.SOLD);

        // --- Save all changes ---
        Transaction completedTransaction = transactionRepository.save(currentTransaction);
        creditListingRepository.save(currentListing);
        carbonCreditRepository.save(currentCredit);

        // --- Log Audit & Notify ---
        auditService.logTransactionCompleted(completedTransaction.getId().toString(), buyer.getId().toString(),
                seller.getId().toString());
        notificationService.notifyTransactionCompleted(buyer, seller, completedTransaction.getId().toString());

        log.info("Transaction {} completed successfully", completedTransaction.getId());
        return completedTransaction; // Return the completed entity
    }

    @Transactional
    public Transaction failTransaction(Transaction transaction, String reason) {
        log.warn("Failing transaction {} with reason: {}", transaction.getId(), reason);

        // Re-fetch for sagety
        Transaction currentTransaction = findTransactionById(transaction.getId());

        // Onlye fail if it's Pending
        if (currentTransaction.getStatus() != TransactionStatus.PENDING) {
            log.warn("Attempted to fail transaction {} which is not PENDING (Status: {})", transaction.getId(),
                    currentTransaction.getStatus());
            return currentTransaction; // Already completed or failed, do nothing
        }
        currentTransaction.setStatus(TransactionStatus.CANCELLED);
        Transaction failedTransaction = transactionRepository.save(currentTransaction);

        // Restore listing status IF it was pending due to this transaction
        CreditListing listing = currentTransaction.getListing();
        if (listing != null && listing.getStatus() == ListingStatus.PENDING_TRANSACTION) {
            listing.setStatus(ListingStatus.ACTIVE);
            creditListingRepository.save(listing);
            log.info("Restored listing {} to ACTIVE after transaction failure", listing.getId());
        }

        auditService.logTransactionFailed(failedTransaction.getId().toString(), reason);
        notificationService.notifyTransactionFailed(failedTransaction.getBuyer(),
                failedTransaction.getSeller(), failedTransaction.getId().toString(), reason);

        log.info("Transaction {} marked as CANCELLED", failedTransaction.getId());
        return failedTransaction;
    }

    @Transactional
    public Transaction cancelTransaction(UUID transactionId, User requestingUser) {
        log.info("User {} attempting to cancel transaction {}", requestingUser.getUsername(), transactionId);
        Transaction transaction = findTransactionById(transactionId);

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new BusinessOperationException(
                    "Only PENDING transactions can be cancelled. Status: " + transaction.getStatus());
        }
        if (!canCancelTransaction(transaction, requestingUser)) {
            throw new UnauthorizedOperationException(requestingUser.getId().toString(), "transaction",
                    transactionId.toString(), "cancel");
        }

        // Use failTransaction logic for consistency
        return failTransaction(transaction, "Cancelled by user " + requestingUser.getUsername());
    }

    private boolean canCancelTransaction(Transaction transaction, User user) {
        return transaction.getBuyer().getId().equals(user.getId()) ||
                transaction.getSeller().getId().equals(user.getId()) ||
                hasAdminRole(user);
    }

    private boolean hasAdminRole(User user) {
        return user != null && (user.getRole() == User.UserRole.ADMIN || user.getRole() == User.UserRole.CVA);
    }

    // ==========================================================
    // QUERY METHODS (Returning DTOs)
    // ==========================================================

    @Transactional(readOnly = true)
    public TransactionDTO findTransactionDtoById(UUID transactionId) {
        validationService.validateId(transactionId, "Transaction");
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found with ID: " + transactionId));
        return DTOMapper.toTransactionDTO(transaction); // Use static mapper
    }

    // Keep internal helper that returns entity
    public Transaction findTransactionById(UUID transactionId) {
        validationService.validateId(transactionId, "Transaction");
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found with ID: " + transactionId));
    }

    @Transactional(readOnly = true)
    public Page<TransactionDTO> getUserTransactions(User user, int page, int size) {
        validationService.validateUser(user);
        validationService.validatePageParameters(page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Transaction> entityPage = transactionRepository.findByBuyerOrSeller(user, user, pageable);
        return entityPage.map(DTOMapper::toTransactionDTO); // Use static mapper reference
    }

    @Transactional(readOnly = true)
    public Page<TransactionDTO> getPurchaseHistory(User buyer, int page, int size) {
        validationService.validateUser(buyer);
        validationService.validatePageParameters(page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Transaction> entityPage = transactionRepository.findByBuyer(buyer, pageable);
        return entityPage.map(DTOMapper::toTransactionDTO); // Use static mapper reference
    }

    @Transactional(readOnly = true)
    public Page<TransactionDTO> getSalesHistory(User seller, int page, int size) {
        validationService.validateUser(seller);
        validationService.validatePageParameters(page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Transaction> entityPage = transactionRepository.findBySeller(seller, pageable);
        return entityPage.map(DTOMapper::toTransactionDTO); // Use static mapper reference
    }

    @Transactional(readOnly = true)
    public Page<TransactionDTO> getTransactionsByStatus(TransactionStatus status, int page, int size) {
        validationService.validatePageParameters(page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Transaction> entityPage = transactionRepository.findByStatus(status, pageable);
        return entityPage.map(DTOMapper::toTransactionDTO); // Use static mapper reference
    }

    @Transactional(readOnly = true)
    public Page<TransactionDTO> getDisputedTransactions(int page, int size) {
        validationService.validatePageParameters(page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Transaction> entityPage = transactionRepository.findByStatus(TransactionStatus.DISPUTED, pageable);
        return entityPage.map(DTOMapper::toTransactionDTO); // Use static mapper reference
    }

    @Transactional(readOnly = true)
    public Page<TransactionDTO> getTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate, int page,
            int size) {
        log.info("Fetching transactions DTOs by date range: {} to {}, page: {}, size: {}", startDate, endDate, page,
                size);
        validationService.validatePageParameters(page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Transaction> entityPage = transactionRepository.findByDateRange(startDate, endDate, pageable);
        log.info("Found {} transactions in date range", entityPage.getTotalElements());
        return entityPage.map(DTOMapper::toTransactionDTO); // Use static mapper reference
    }

    // ==========================================================
    // DISPUTE-RELATED STATUS UPDATES (Called BY DisputeService)
    // ==========================================================

    @Transactional
    public Transaction markAsDisputed(UUID transactionId, String disputeId) {
        log.info("Marking transaction {} as disputed due to dispute {}", transactionId, disputeId);
        Transaction transaction = findTransactionById(transactionId); // Use internal helper
        validationService.validateTransactionStatusChange(transaction, TransactionStatus.DISPUTED); // Assumes this
                                                                                                    // exists
        transaction.setStatus(TransactionStatus.DISPUTED);
        Transaction updatedTransaction = transactionRepository.save(transaction);
        log.info("Transaction {} marked as DISPUTED", transactionId);
        return updatedTransaction; // Return entity as this is likely internal
    }

    @Transactional
    public Transaction updateStatusAfterDisputeResolution(UUID transactionId, TransactionStatus finalStatus,
            String resolution) {
        log.info("Updating transaction {} status to {} after dispute resolution: {}", transactionId, finalStatus,
                resolution);
        Transaction transaction = findTransactionById(transactionId); // Use internal helper

        if (transaction.getStatus() != TransactionStatus.DISPUTED) {
            log.warn("Attempted to resolve dispute for transaction {} which is not disputed (status: {})",
                    transactionId, transaction.getStatus());
            // Proceed cautiously or throw error based on requirements
        }

        transaction.setStatus(finalStatus);

        CreditListing listing = transaction.getListing();
        if (listing != null) {
            if (finalStatus == TransactionStatus.CANCELLED) {
                if (listing.getStatus() == ListingStatus.CLOSED
                        || listing.getStatus() == ListingStatus.PENDING_TRANSACTION) {
                    listing.setStatus(ListingStatus.ACTIVE);
                    creditListingRepository.save(listing);
                    log.info("Restored listing {} to ACTIVE after dispute cancellation", listing.getId());
                }
                log.info("Refund processing should be initiated for transaction {}", transactionId);
            } else if (finalStatus == TransactionStatus.COMPLETED) {
                if (listing.getStatus() != ListingStatus.CLOSED) {
                    listing.setStatus(ListingStatus.CLOSED);
                    creditListingRepository.save(listing);
                    log.info("Ensured listing {} is CLOSED after dispute completion", listing.getId());
                }
                if (transaction.getCompletedAt() == null) {
                    transaction.setCompletedAt(LocalDateTime.now());
                }
            }
        }

        Transaction updatedTransaction = transactionRepository.save(transaction);
        log.info("Transaction {} status updated to {} after dispute resolution", transactionId, finalStatus);
        return updatedTransaction; // Return entity as this is likely internal
    }

    // ==========================================================
    // STATISTICS
    // ==========================================================

    @Transactional(readOnly = true)
    public Map<String, Object> getTransactionStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Generating transaction statistics from {} to {}", startDate, endDate);
        Map<String, Object> stats = new HashMap<>();

        long totalTransactions = transactionRepository.countByDateRange(startDate, endDate);
        long completedTransactions = transactionRepository.countByStatusAndDateRange(TransactionStatus.COMPLETED,
                startDate, endDate);
        long disputedTransactions = transactionRepository.countByStatusAndDateRange(TransactionStatus.DISPUTED,
                startDate, endDate);
        long cancelledTransactions = transactionRepository.countByStatusAndDateRange(TransactionStatus.CANCELLED,
                startDate, endDate);

        stats.put("totalTransactions", totalTransactions);
        stats.put("completedTransactions", completedTransactions);
        stats.put("disputedTransactions", disputedTransactions);
        stats.put("cancelledTransactions", cancelledTransactions);

        double successRate = totalTransactions > 0 ? (double) completedTransactions / totalTransactions * 100 : 0.0;
        stats.put("successRate", Math.round(successRate * 100.0) / 100.0);

        double disputeRate = totalTransactions > 0 ? (double) disputedTransactions / totalTransactions * 100 : 0.0;
        stats.put("disputeRate", Math.round(disputeRate * 100.0) / 100.0);

        stats.put("dateRange", Map.of("startDate", startDate, "endDate", endDate));

        log.info("Generated statistics: {} total transactions, {}% success rate", totalTransactions,
                stats.get("successRate"));
        return stats;
    }

}
