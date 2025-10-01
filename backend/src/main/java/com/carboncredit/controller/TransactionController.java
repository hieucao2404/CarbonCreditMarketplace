package com.carboncredit.controller;

// Add these imports to TransactionController.java
import com.carboncredit.dto.PurchaseRequest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.carboncredit.entity.Dispute;
import com.carboncredit.entity.Transaction;
import com.carboncredit.entity.User;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import com.carboncredit.dto.DisputeRequest;
import com.carboncredit.dto.TransactionResponse;
import com.carboncredit.service.TransactionService;
import com.carboncredit.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;
    private final UserService userService;

    // initiate a purchase of carbon credits from listing
    @PostMapping("/purchase")
    public ResponseEntity<Transaction> initiateTransaction(@RequestBody PurchaseRequest request,
            Authentication authentication) {
        try {
            User buyer = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            log.info("User {} initiating purchase of listing {}", buyer.getUsername(), request.getListingId());

            Transaction transaction = transactionService.initiatePurchase(request.getListingId(), buyer);

            return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
        } catch (Exception e) {
            log.error("Error initiating purchase: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }

    }

    // Complete a transaction (process payment and finalize
    @PostMapping("/{transactionId}/complete")
    public ResponseEntity<Transaction> completeTransaction(@PathVariable UUID transactionId,
            Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("user not found"));

            Transaction transaction = transactionService.findTransactionById(transactionId);
            if (transaction == null) {
                return ResponseEntity.notFound().build();
            }

            // check if user is authorized (buyer or seller)
            if (!transaction.getBuyer().getId().equals(user.getId())
                    && !transaction.getListing().getCredit().getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Process payment and complete
            transactionService.processPayment(transaction);
            Transaction completedTransaction = transactionService.completeTransaction(transaction);

            log.info("Transaction {} completed successfully", transactionId);

            return ResponseEntity.ok(completedTransaction);
        } catch (Exception e) {
            log.error("Error completing transaction {}: {}", transactionId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // cancel a transaction (before completion
    @PostMapping("/{transactionId}/cancel")
    public ResponseEntity<Transaction> cancelTransaction(@PathVariable UUID transactionId,
            Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Transaction cancelledTransaction = transactionService.cancelTransaction(transactionId, user);

            log.info("Transaction {} cancelled by user {}", transactionId, user.getUsername());

            return ResponseEntity.ok(cancelledTransaction);
        } catch (Exception e) {
            log.error("Error cancelling transaction {}: {}", transactionId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // get specific transaction by Id
    @GetMapping("/{transactionId}")
    public ResponseEntity<Transaction> getTransaction(@PathVariable UUID transactionId, Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Transaction transaction = transactionService.findTransactionById(transactionId);
            if (transaction == null) {
                return ResponseEntity.notFound().build();
            }

            // Check authorization (buyer, seller, or admin)
            boolean isAuthorized = transaction.getBuyer().getId().equals(user.getId())
                    || transaction.getListing().getCredit().getUser().getId().equals(user.getId())
                    || user.getRole() == User.UserRole.ADMIN;

            if (!isAuthorized) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            log.error("Error fetching transaction {}: {}", transactionId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // get user's transaction history (both purchases and sales)
    @GetMapping("/my-transactions")
    public ResponseEntity<Page<Transaction>> getMyTransactions(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        try {
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Page<Transaction> transactions = transactionService.getUserTransactions(user, page, size);

            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            log.error("Error fetching user transaction: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // get user's purchase history
    @GetMapping("/purchases")
    public ResponseEntity<Page<Transaction>> getPurchaseHistory(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size, Authentication authentication) {
        try {
            User buyer = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Page<Transaction> purchases = transactionService.getPurchaseHistory(buyer, page, size);

            return ResponseEntity.ok(purchases);
        } catch (Exception e) {
            log.error("Error fetching purchase history: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // get user's sales history
    @GetMapping("/sales")
    public ResponseEntity<Page<Transaction>> getSalesHistory(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size, Authentication authentication) {
        try {
            User seller = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Page<Transaction> sales = transactionService.getSalesHistory(seller, page, size);

            return ResponseEntity.ok(sales);
        } catch (Exception e) {
            log.error("Error fetching sales history: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // create a dispute for transaction
    @PostMapping("/{transactionId}/dispute")
    public ResponseEntity<Dispute> createDispute(@PathVariable UUID transactionId, @RequestBody DisputeRequest request,
            Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Dispute dispute = transactionService.createDispute(transactionId, user, request.getReason());

            log.info("Dispute created for transaction {} by user {}", transactionId, user.getUsername());

            return ResponseEntity.status(HttpStatus.CREATED).body(dispute);
        } catch (Exception e) {
            log.error("Error creating dispute for transaction {}: {}", transactionId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // Admin: Get all disputed transactions
    @GetMapping("/admin/disputed")
    public ResponseEntity<Page<Transaction>> getDisputedTransactions(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size, Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // check admin role
            if (user.getRole() != User.UserRole.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Page<Transaction> transactions = transactionService.getDisputedTransactions(page, size);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            log.error("Error fetching disputed transactions: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // Admin: get transaction status
    @GetMapping("/admin/statistics")
    public ResponseEntity<Map<String, Object>> getTransactionStatistics(@RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate, Authentication authentication) {
        
        try {
            User user = userService.findByUsername(authentication.getName()).orElseThrow(() -> new RuntimeException("User not found"));

            //Check admin role
            if(user.getRole() != User.UserRole.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate) : LocalDateTime.now().minusDays(30);
            LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate) : LocalDateTime.now();

            Map<String, Object> statistics = transactionService.getTransactionStatistics(start, end);

            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("Error fetching transaction statistics: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}


