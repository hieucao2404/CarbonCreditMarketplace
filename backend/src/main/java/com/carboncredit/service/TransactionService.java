package com.carboncredit.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.carboncredit.exception.PaymentException;
import com.carboncredit.exception.SecurityException;
import com.carboncredit.exception.UnauthorizedOperationException;
import com.carboncredit.exception.ValidationException;
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
            completedTransaction.getSeller().getId().toString()
        );

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
}
