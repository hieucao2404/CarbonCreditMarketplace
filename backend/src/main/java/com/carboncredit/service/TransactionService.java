package com.carboncredit.service;

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
import com.carboncredit.entity.CarbonCredit;
import com.carboncredit.entity.CreditListing;
import com.carboncredit.entity.CreditListing.ListingStatus;
import com.carboncredit.entity.Dispute;
import com.carboncredit.entity.Dispute.DisputeStatus;
import com.carboncredit.entity.Transaction;
import com.carboncredit.entity.Transaction.TransactionStatus;
import com.carboncredit.entity.User;
import com.carboncredit.exception.BusinessOperationException;
import com.carboncredit.exception.EntityNotFoundException;
import com.carboncredit.exception.PaymentException;
import com.carboncredit.exception.UnauthorizedOperationException;
import com.carboncredit.repository.CarbonCreditRepository;
import com.carboncredit.repository.CreditListingRepository;
import com.carboncredit.repository.DisputeRepository;
import com.carboncredit.repository.TransactionRepository;
import com.carboncredit.service.PaymentService.PaymentResult;

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

    // ==== TRANSACTION AND PROCESSING ================

    // Initiates transaction for purchasing a carbon credit
    @Transactional
    public Transaction initiatePurchase(UUID listingId, User buyer) {
        log.info("Initiating purchase for listing {} by user {}", listingId, buyer.getUsername());

        // Validate parameters
        validationService.validateId(listingId, "CreditListing");
        validationService.validateUser(buyer);

        // Find and validate listing
        CreditListing listing = creditListingRepository.findById(listingId)
                .orElseThrow(() -> new EntityNotFoundException("Listing not found"));

        // Validate purchase request
        validationService.validatePurchaseRequest(listing, buyer);

        // Get associated carbon credit
        CarbonCredit credit = listing.getCredit();
        User seller = credit.getUser();

        // Create transaction
        Transaction transaction = new Transaction();
        transaction.setCredit(credit);
        transaction.setListing(listing);
        transaction.setBuyer(buyer);
        transaction.setSeller(seller);
        transaction.setAmount(listing.getPrice());
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setCreatedAt(LocalDateTime.now());

        Transaction savedTransaction = transactionRepository.save(transaction);

        // Update listing status to prevent concurrent purchases
        listing.setStatus(ListingStatus.PENDING_TRANSACTION);
        creditListingRepository.save(listing);

        // Process payment
        try {
            processPayment(savedTransaction);
        } catch (Exception e) {
            log.error("Payment failed for transaction {}: {}", savedTransaction.getId(), e.getMessage());
            // Rollback listing status
            listing.setStatus(ListingStatus.ACTIVE);
            creditListingRepository.save(listing);

            savedTransaction.setStatus(TransactionStatus.CANCELLED);
            transactionRepository.save(savedTransaction);

            throw new PaymentException("Payment processing failed: " + e.getMessage(), e);
        }

        // Log audit trail
        auditService.logTransactionInitiated(savedTransaction.getId().toString(),
                buyer.getId().toString(), seller.getId().toString(), savedTransaction.getAmount().toString());

        log.info("Transaction {} initiated successfully", savedTransaction.getId());
        return savedTransaction;
    }

    // Process payment for a transaction
    @Transactional
    public void processPayment(Transaction transaction) {
        log.info("Processing payment for transaction {}", transaction.getId());

        // Validate transaction
        validationService.validateTransactionSecurity(transaction);

        // Process payment through payment service
        PaymentResult paymentResult = paymentService.processPayment(transaction.getId(),
                transaction.getAmount(), transaction.getBuyer().getId().toString(),
                transaction.getSeller().getId().toString());

        if (paymentResult.isSuccess()) {
            completeTransaction(transaction);
        } else {
            failTransaction(transaction, "Payment failed: " + paymentResult.getErrorMessage());
            throw new PaymentException("Payment processing failed: " + paymentResult.getErrorMessage());
        }
    }

    // Complete a successful transaction
    @Transactional
    public Transaction completeTransaction(Transaction transaction) {
        log.info("Completing transaction {}", transaction.getId());

        // Validate current transaction state
        CreditListing currentListing = creditListingRepository.findById(transaction.getListing().getId())
                .orElseThrow(() -> new EntityNotFoundException("Listing not found"));
        CarbonCredit currentCredit = carbonCreditRepository.findById(transaction.getCredit().getId())
                .orElseThrow(() -> new EntityNotFoundException("Carbon credit not found"));

        // Validate transaction preconditions
        validationService.validateTransactionPreconditions(transaction, currentListing, currentCredit);

        // Update transaction status
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(LocalDateTime.now());

        // Update listing status
        currentListing.setStatus(ListingStatus.CLOSED);
        creditListingRepository.save(currentListing);

        // Save completed transaction
        Transaction completedTransaction = transactionRepository.save(transaction);

        // Log audit trail
        auditService.logTransactionCompleted(
                completedTransaction.getId().toString(),
                completedTransaction.getBuyer().getId().toString(),
                completedTransaction.getSeller().getId().toString());

        // Send notification
        notificationService.notifyTransactionCompleted(completedTransaction.getBuyer(),
                completedTransaction.getSeller(), completedTransaction.getId().toString());

        log.info("Transaction {} completed successfully", completedTransaction.getId());
        return completedTransaction;
    }

    // Fail a transaction with reason
    @Transactional
    public Transaction failTransaction(Transaction transaction, String reason) {
        log.info("Failing transaction {} with reason: {}", transaction.getId(), reason);

        // Update transaction status
        transaction.setStatus(TransactionStatus.CANCELLED);
        Transaction failedTransaction = transactionRepository.save(transaction);

        // Restore listing status
        CreditListing listing = transaction.getListing();
        listing.setStatus(ListingStatus.ACTIVE);
        creditListingRepository.save(listing);

        // Log audit trail
        auditService.logTransactionFailed(transaction.getId().toString(), reason);

        // Send notification
        notificationService.notifyTransactionFailed(transaction.getBuyer(),
                transaction.getSeller(), transaction.getId().toString(), reason);

        log.info("Transaction {} failed: {}", failedTransaction.getId(), reason);
        return failedTransaction;
    }

    // Cancel a pending transaction
    @Transactional
    public Transaction cancelTransaction(UUID transactionId, User requestingUser) {
        log.info("Cancelling transaction {} by user {}", transactionId, requestingUser.getUsername());

        Transaction transaction = findTransactionById(transactionId);

        // Validate cancellation rights
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new BusinessOperationException("Only pending transactions can be cancelled");
        }

        if (!canCancelTransaction(transaction, requestingUser)) {
            throw new UnauthorizedOperationException("User not authorized to cancel this transaction");
        }

        return failTransaction(transaction, "Cancelled by " + requestingUser.getUsername());
    }

    // Check if user can cancel the transaction
    private boolean canCancelTransaction(Transaction transaction, User user) {
        return transaction.getBuyer().getId().equals(user.getId()) ||
                transaction.getSeller().getId().equals(user.getId()) ||
                hasAdminRole(user);
    }

    // Check if user has admin role
    private boolean hasAdminRole(User user) {
        return User.UserRole.ADMIN.equals(user.getRole()) || User.UserRole.CVA.equals(user.getRole());
    }

    // =========== QUERY METHODS ===========

    // Find transaction by ID
    public Transaction findTransactionById(UUID transactionId) {
        validationService.validateId(transactionId, "Transaction");

        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found with ID: " + transactionId));
    }

    // Get all transactions for a user (as buyer or seller)
    @Transactional(readOnly = true)
    public Page<Transaction> getUserTransactions(User user, int page, int size) {
        validationService.validateUser(user);
        validationService.validatePageParameters(page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return transactionRepository.findByBuyerOrSeller(user, user, pageable);
    }

    // Get purchase history for a buyer
    @Transactional(readOnly = true)
    public Page<Transaction> getPurchaseHistory(User buyer, int page, int size) {
        validationService.validateUser(buyer);
        validationService.validatePageParameters(page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return transactionRepository.findByBuyer(buyer, pageable);
    }

    // Get sales history for a seller
    @Transactional(readOnly = true)
    public Page<Transaction> getSalesHistory(User seller, int page, int size) {
        validationService.validateUser(seller);
        validationService.validatePageParameters(page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return transactionRepository.findBySeller(seller, pageable);
    }

    // Get transactions by status
    @Transactional(readOnly = true)
    public Page<Transaction> getTransactionsByStatus(TransactionStatus status, int page, int size) {
        validationService.validatePageParameters(page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return transactionRepository.findByStatus(status, pageable);
    }

    // Get disputed transactions
    @Transactional(readOnly = true)
    public Page<Transaction> getDisputedTransactions(int page, int size) {
        validationService.validatePageParameters(page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return transactionRepository.findByStatus(TransactionStatus.DISPUTED, pageable);
    }

    // ================== Dispute related methods ====================

    // Create a dispute for a transaction
    @Transactional
    public Dispute createDispute(UUID transactionId, User user, String reason) {
        log.info("Create dispute for transaction {} by user {}", transactionId, user.getUsername());

        // Find and validate transaction
        Transaction transaction = findTransactionById(transactionId);

        // validate dispute creation rights
        validationService.validateDisputeCreationRights(transaction, user);
        validationService.validateDisputeReason(reason);
        validationService.validateNoExistingOpenDisputes(transactionId);

        // create dispute
        Dispute dispute = new Dispute();
        dispute.setTransaction(transaction);
        dispute.setRaisedBy(user);
        dispute.setReason(reason);
        dispute.setStatus(DisputeStatus.OPEN);
        dispute.setCreatedAt(LocalDateTime.now());

        Dispute savedDispute = disputeRepository.save(dispute);

        // update transaction status
        transaction.setStatus(TransactionStatus.DISPUTED);
        transactionRepository.save(transaction);

        // determine other partu for notifications
        User otherParty = transaction.getBuyer().getId().equals(user.getId()) ? transaction.getSeller()
                : transaction.getBuyer();

        // Send notification
        notificationService.notifyDisputeCreated(user, otherParty, transaction.getId().toString());

        // autdit log
        auditService.logDisputeCreated(savedDispute.getId().toString(), transaction.getId().toString());

        log.info("Dispute {} create successfully for transaction {}", savedDispute.getId(), transactionId);
        return savedDispute;
    }

    // Mark transaction as disputed (called by DisputeSerive)
    @Transactional
    public Transaction markAsDisputed(UUID transactionId, String disputeId) {
        log.info("Marking transaction {} as disputed due to dispute {}", transactionId, disputeId);

        Transaction transaction = findTransactionById(transactionId);

        // Validate transaction status change
        validationService.validateTransactionStatusChange(transaction, TransactionStatus.DISPUTED);

        transaction.setStatus(TransactionStatus.DISPUTED);
        Transaction updatedTransaction = transactionRepository.save(transaction);

        log.info("Transaction {} marked as disputed", transactionId);
        return updatedTransaction;
    }

    // resolve transaction dispute (called the dispute is resolve)
    @Transactional
    public Transaction resolveDisputedTransaction(UUID transactionId, String resolution) {
        log.info("Resolving disputed transaction {} with resolution: {}", transactionId, resolution);

        Transaction transaction = findTransactionById(transactionId);

        if (transaction.getStatus() != TransactionStatus.DISPUTED) {
            throw new BusinessOperationException("Transaction is not in disputed state");
        }

        // determine resoltoon aciton based on resolution text
        if (resolution.toLowerCase().contains("compele") || resolution.toLowerCase().contains("proceed")) {
            // complete the transaction
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setCompletedAt(LocalDateTime.now());

            // update associated lisitng status
            CreditListing listing = transaction.getListing();
            listing.setStatus(ListingStatus.CLOSED);
            creditListingRepository.save(listing);
        } else if (resolution.toLowerCase().contains("cancel") || resolution.toLowerCase().contains("refund")) {
            // cancel the transacton and restore listing
            transaction.setStatus(TransactionStatus.CANCELLED);

            CreditListing listing = transaction.getListing();
            listing.setStatus(ListingStatus.ACTIVE);
            creditListingRepository.save(listing);

            // process refund if payment was made
            log.info("Refund processing initiated for transaction {}", transactionId);
        }

        Transaction resolvedTransaction = transactionRepository.save(transaction);

        // Log resolution
        auditService.logTransactionCompleted(transactionId.toString(), transaction.getBuyer().getId().toString(),
                transaction.getSeller().getId().toString());

        log.info("Disputed transaction {} resovled successfully", transactionId);
        return resolvedTransaction;
    }

    // Get transaction statistiics for dashboard
    @Transactional(readOnly = true)
    public Map<String, Object> getTransactionStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Generating transaction statistics from {} to {}", startDate, endDate);

        Map<String, Object> stats = new HashMap<>();
        // get basic counts
        long totalTransactions = transactionRepository.countByDateRange(startDate, endDate);
        long completedTransactions = transactionRepository.countByStatusAndDateRange(
                TransactionStatus.COMPLETED, startDate, endDate);
        long disputedTransactions = transactionRepository.countByStatusAndDateRange(
                TransactionStatus.DISPUTED, startDate, endDate);
        long cancelledTransactions = transactionRepository.countByStatusAndDateRange(
                TransactionStatus.CANCELLED, startDate, endDate);

        stats.put("totalTransactions", totalTransactions);
        stats.put("completedTransactions", completedTransactions);
        stats.put("disputedTransactions", disputedTransactions);
        stats.put("cancelledTransactions", cancelledTransactions);

        // calculate success rate
        double successRate = totalTransactions > 0 ? (double) completedTransactions / totalTransactions * 100 : 0.0;
        stats.put("successRate", Math.round(successRate * 100.0) / 100.0);

        // calculate dispute rate
        double disputeRate = totalTransactions > 0
                ? (double) disputedTransactions / totalTransactions * 100
                : 0.0;
        stats.put("disputeRate", Math.round(disputeRate * 100.0) / 100.0);

        // Add date range for reference
        stats.put("dateRange", Map.of("startDate", startDate, "endDate", endDate));

        log.info("Generated statistics: {} total transactions, {}% success rate",
                totalTransactions, stats.get("successRate"));

        return stats;

    }

    // get transaction for specific date range
    @Transactional(readOnly = true)
    public Page<Transaction> getTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate, int page,
            int size) {
        log.info("Fetching transactions by date range: {} to {}, page: {}, size: {}",
                startDate, endDate, page, size);

        validationService.validatePageParameters(page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Transaction> transactions = transactionRepository.findByDateRange(startDate, endDate, pageable);

        log.info("Found {} transactions in date range", transactions.getTotalElements());
        return transactions;
    }

}
