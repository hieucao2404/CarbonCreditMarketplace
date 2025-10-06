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
import com.carboncredit.dto.DisputeDTO;
import com.carboncredit.dto.DisputeRequest;
import com.carboncredit.dto.TransactionDTO;
import com.carboncredit.dto.TransactionResponse;
import com.carboncredit.service.TransactionService;
import com.carboncredit.util.DTOMapper;
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
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;
    private final UserService userService;

    // initiate a purchase of carbon credits from listing
    @PostMapping("/purchase")
    public ResponseEntity<TransactionDTO> initiateTransaction(@RequestBody PurchaseRequest request,
            Authentication authentication) {
        try {
            User buyer = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            log.info("User {} initiating purchase of listing {}", buyer.getUsername(), request.getListingId());

            Transaction transaction = transactionService.initiatePurchase(request.getListingId(), buyer);
            TransactionDTO transactionDTO = DTOMapper.toTransactionDTO(transaction);

            return ResponseEntity.status(HttpStatus.CREATED).body(transactionDTO);
        } catch (Exception e) {
            log.error("Error initiating purchase: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }

    }

    // Complete a transaction (process payment and finalize
    @PostMapping("/{transactionId}/complete")
    public ResponseEntity<TransactionDTO> completeTransaction(@PathVariable UUID transactionId,
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
            TransactionDTO transactionDTO = DTOMapper.toTransactionDTO(completedTransaction);

            log.info("Transaction {} completed successfully", transactionId);

            return ResponseEntity.ok(transactionDTO);
        } catch (Exception e) {
            log.error("Error completing transaction {}: {}", transactionId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // cancel a transaction (before completion
    @PostMapping("/{transactionId}/cancel")
    public ResponseEntity<TransactionDTO> cancelTransaction(@PathVariable UUID transactionId,
            Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Transaction cancelledTransaction = transactionService.cancelTransaction(transactionId, user);
            TransactionDTO transactionDTO = DTOMapper.toTransactionDTO(cancelledTransaction);

            log.info("Transaction {} cancelled by user {}", transactionId, user.getUsername());

            return ResponseEntity.ok(transactionDTO);
        } catch (Exception e) {
            log.error("Error cancelling transaction {}: {}", transactionId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // get specific transaction by Id
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionDTO> getTransaction(@PathVariable UUID transactionId, Authentication authentication) {
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

            TransactionDTO transactionDTO = DTOMapper.toTransactionDTO(transaction);
            return ResponseEntity.ok(transactionDTO);
        } catch (Exception e) {
            log.error("Error fetching transaction {}: {}", transactionId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // get user's transaction history (both purchases and sales)
    @GetMapping("/my-transactions")
    public ResponseEntity<Page<TransactionDTO>> getMyTransactions(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        try {
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Page<Transaction> transactions = transactionService.getUserTransactions(user, page, size);
            Page<TransactionDTO> transactionDTOs = DTOMapper.toTransactionDTOPage(transactions);

            return ResponseEntity.ok(transactionDTOs);
        } catch (Exception e) {
            log.error("Error fetching user transaction: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // get user's purchase history
    @GetMapping("/purchases")
    public ResponseEntity<Page<TransactionDTO>> getPurchaseHistory(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size, Authentication authentication) {
        try {
            User buyer = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Page<Transaction> purchases = transactionService.getPurchaseHistory(buyer, page, size);
            Page<TransactionDTO> purchaseDTOs = DTOMapper.toTransactionDTOPage(purchases);

            return ResponseEntity.ok(purchaseDTOs);
        } catch (Exception e) {
            log.error("Error fetching purchase history: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // get user's sales history
    @GetMapping("/sales")
    public ResponseEntity<Page<TransactionDTO>> getSalesHistory(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size, Authentication authentication) {
        try {
            User seller = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Page<Transaction> sales = transactionService.getSalesHistory(seller, page, size);
            Page<TransactionDTO> salesDTOs = DTOMapper.toTransactionDTOPage(sales);

            return ResponseEntity.ok(salesDTOs);
        } catch (Exception e) {
            log.error("Error fetching sales history: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // create a dispute for transaction
    @PostMapping("/{transactionId}/dispute")
    public ResponseEntity<DisputeDTO> createDispute(@PathVariable UUID transactionId, @RequestBody DisputeRequest request,
            Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Dispute dispute = transactionService.createDispute(transactionId, user, request.getReason());
            DisputeDTO disputeDTO = DTOMapper.toDisputeDTO(dispute);

            log.info("Dispute created for transaction {} by user {}", transactionId, user.getUsername());

            return ResponseEntity.status(HttpStatus.CREATED).body(disputeDTO);
        } catch (Exception e) {
            log.error("Error creating dispute for transaction {}: {}", transactionId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // Admin: Get all disputed transactions
    @GetMapping("/admin/disputed")
    public ResponseEntity<Page<TransactionDTO>> getDisputedTransactions(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size, Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // check admin role
            if (user.getRole() != User.UserRole.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Page<Transaction> transactions = transactionService.getDisputedTransactions(page, size);
            Page<TransactionDTO> transactionDTOs = DTOMapper.toTransactionDTOPage(transactions);
            return ResponseEntity.ok(transactionDTOs);
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


