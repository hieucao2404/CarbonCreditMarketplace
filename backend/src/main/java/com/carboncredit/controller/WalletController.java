package com.carboncredit.controller;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.carboncredit.dto.ApiResponse;
import com.carboncredit.dto.DepositRequest;
import com.carboncredit.dto.TransactionDTO;
import com.carboncredit.dto.WalletResponse;
import com.carboncredit.entity.Transaction;
import com.carboncredit.entity.User;
import com.carboncredit.entity.Wallet;
import com.carboncredit.exception.ResourceNotFoundException;
import com.carboncredit.dto.WithdrawRequest;
import com.carboncredit.service.BankingService;
import com.carboncredit.util.DTOMapper;
import com.carboncredit.service.TransactionService;
import com.carboncredit.service.UserService;
import com.carboncredit.service.WalletService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Slf4j
@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
@Validated
public class WalletController {
    private final WalletService walletService;
    private final UserService userService;
    private final BankingService bankingService;
    private final TransactionService transactionService;

    // Get current user's wallet information
    @GetMapping("/my-wallet")
    public ResponseEntity<WalletResponse> getMyWallet(Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Wallet wallet = walletService.getOrCreateWallet(user);
            WalletResponse response = mapToWalletResponse(wallet);

            log.info("Wallet information retrieved for user: {}", user.getUsername());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving wallet information: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // Check if user has sufficient balance for a transaction
    @GetMapping("/balance-check")
    public ResponseEntity<Boolean> checkSufficientBalance(
            @RequestParam BigDecimal amount,
            @RequestParam(defaultValue = "CASH") String balanceType,
            Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Wallet wallet = walletService.getOrCreateWallet(user);

            boolean hasSufficientBalance;
            if ("CREDIT".equalsIgnoreCase(balanceType)) {
                hasSufficientBalance = wallet.getCreditBalance().compareTo(amount) >= 0; // Fixed: use
                                                                                         // getCreditBalance()
            } else {
                hasSufficientBalance = wallet.getCashBalance().compareTo(amount) >= 0;
            }

            log.info("Balance check for user {}: {} {} sufficient for {}", user.getUsername(), balanceType,
                    hasSufficientBalance ? "is" : "is not", amount);

            return ResponseEntity.ok(hasSufficientBalance);
        } catch (Exception e) {
            log.error("Error checking balance: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // Deposit funds to wallet
    @PostMapping("/deposit")
    public ResponseEntity<WalletResponse> depositFunds(
            @Valid @RequestBody DepositRequest request,
            Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Process banking transaction first
            boolean paymentSuccessful = bankingService.processDeposit(user.getId(), request.getAmount(),
                    request.getPaymentMethodId());

            if (!paymentSuccessful) {
                log.warn("Banking deposit failed for user: {}", user.getUsername());
                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).build();
            }

            // Update wallet balance
            Wallet updatedWallet = walletService.updateCashBalance(user.getId(), request.getAmount());
            WalletResponse response = mapToWalletResponse(updatedWallet);

            log.info("Deposit successful: {} added to wallet for user {}", request.getAmount(), user.getUsername());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid deposit request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error processing deposit: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Withdraw funds from wallet
    @PostMapping("/withdraw")
    public ResponseEntity<WalletResponse> withdrawFunds(@Valid @RequestBody WithdrawRequest request,
            Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Check if user has sufficient balance
            Wallet wallet = walletService.getOrCreateWallet(user);
            if (wallet.getCashBalance().compareTo(request.getAmount()) < 0) {
                log.warn("Insufficient funds for withdrawal: user {}, requested {}, available {}", user.getUsername(),
                        request.getAmount(), wallet.getCashBalance());
                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).build();
            }

            // Process banking withdrawal
            boolean withdrawalSuccessful = bankingService.processWithdrawal(user.getId(), request.getAmount(),
                    request.getBankAccountInfo()); // Fixed: correct field name

            if (!withdrawalSuccessful) {
                log.warn("Banking withdrawal failed for user: {}", user.getUsername());
                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).build();
            }

            // Update wallet balance (negative amount for withdrawal)
            Wallet updatedWallet = walletService.updateCashBalance(user.getId(), request.getAmount().negate());
            WalletResponse response = mapToWalletResponse(updatedWallet);

            log.info("Withdrawal successful: {} deducted from wallet for user {}",
                    request.getAmount(), user.getUsername());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid withdrawal request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error processing withdrawal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get wallet transaction history - Fixed to use existing TransactionService
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<Page<TransactionDTO>>> getWalletTransactions( // Added ApiResponse wrapper
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        log.info("Fetching wallet transactions for user {}", authentication.getName());
        try {
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "username", authentication.getName())); // Use
                                                                                                                     // specific
                                                                                                                     // exception

            // *** FIX HERE ***
            // Service now returns the DTO page directly
            Page<TransactionDTO> transactionDTOs = transactionService.getUserTransactions(user, page, size);
            // *** REMOVED: Page<Transaction> transactions = ... ***
            // *** REMOVED: Page<TransactionDTO> transactionDTOs =
            // DTOMapper.toTransactionDTOPage(transactions); ***

            log.info("Retrieved {} wallet transactions for user: {}",
                    transactionDTOs.getTotalElements(), user.getUsername()); // Use DTO page for count

            // Wrap in ApiResponse
            return ResponseEntity.ok(ApiResponse.success(transactionDTOs));

        } catch (ResourceNotFoundException e) { // Catch specific exception
            log.warn("Cannot get transactions - user not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error retrieving wallet transactions for user {}: {}", authentication.getName(), e.getMessage(),
                    e); // Log full exception
            // Wrap in ApiResponse
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve transaction history."));
        }

    }

    // Admin: Get any user's wallet (admin only)
    @GetMapping("/admin/user/{userId}")
    public ResponseEntity<WalletResponse> getUserWallet(@PathVariable UUID userId, Authentication authentication) { // Fixed:
                                                                                                                    // @PathVariable
                                                                                                                    // not
                                                                                                                    // @RequestParam
        try {
            User currentUser = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Check admin privileges - Fixed enum access
            if (currentUser.getRole() != User.UserRole.ADMIN &&
                    currentUser.getRole() != User.UserRole.CVA) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            User targetUser = userService.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Target user not found"));

            Wallet wallet = walletService.getOrCreateWallet(targetUser);
            WalletResponse response = mapToWalletResponse(wallet);

            log.info("Admin {} accessed wallet for user: {}", currentUser.getUsername(), targetUser.getUsername());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving user wallet: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // Admin: Update user's wallet balance (admin only)
    @PutMapping("/admin/user/{userId}/balance")
    public ResponseEntity<WalletResponse> updateUserBalance(@PathVariable UUID userId,
            @RequestParam BigDecimal creditAmount, @RequestParam BigDecimal cashAmount,
            @RequestParam(required = false) String reason, Authentication authentication) {
        try {
            User currentUser = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Check admin privileges
            if (currentUser.getRole() != User.UserRole.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            User targetUser = userService.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Target user not found"));

            // Update both balances
            walletService.updateCreditBalance(userId, creditAmount);
            Wallet updatedWallet = walletService.updateCashBalance(userId, cashAmount);

            WalletResponse response = mapToWalletResponse(updatedWallet);

            log.info("Admin {} updated wallet for user {}: credit={}, cash={}, reason='{}'", currentUser.getUsername(),
                    targetUser.getUsername(), creditAmount, cashAmount, reason);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating user wallet balance: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // Helper method to map Wallet entity to WalletResponse DTO
    private WalletResponse mapToWalletResponse(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getUser().getId(),
                wallet.getUser().getUsername(),
                wallet.getCreditBalance(),
                wallet.getCashBalance(),
                wallet.getUpdatedAt());
    }
}
