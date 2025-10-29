package com.carboncredit.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.carboncredit.config.MoMoConfig;
import com.carboncredit.dto.ApiResponse;
import com.carboncredit.dto.DepositRequest;
import com.carboncredit.dto.TransactionDTO;
import com.carboncredit.dto.VNPayDepositRequest;
import com.carboncredit.dto.WalletResponse;
import com.carboncredit.entity.Payment;
import com.carboncredit.entity.Transaction;
import com.carboncredit.entity.User;
import com.carboncredit.entity.Wallet;
import com.carboncredit.exception.ResourceNotFoundException;
import com.carboncredit.repository.PaymentRepository;
import com.carboncredit.dto.WithdrawRequest;
import com.carboncredit.service.BankingService;
import com.carboncredit.service.CurrencyService;
import com.carboncredit.service.MoMoService;
import com.carboncredit.util.DTOMapper;
import com.carboncredit.service.TransactionService;
import com.carboncredit.service.UserService;
import com.carboncredit.service.VNPayService;
import com.carboncredit.service.WalletService;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
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

    @Autowired
    private VNPayService vnPayService;

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private PaymentRepository paymentRepository;

    // ADD THIS TO YOUR WALLETCONTROLLER.JAVA

    // First, add MoMoService and MoMoConfig to existing @Autowired fields:
    @Autowired
    private MoMoService momoService;

    @Autowired
    private MoMoConfig momoConfig;

    /**
     * 🧪 TEST - Manually complete MoMo payment (bypasses signature check)
     */
    @GetMapping("/test-complete-momo")
    public ResponseEntity<Map<String, Object>> testCompleteMoMo(@RequestParam String orderId) {
        log.info("🧪 TEST: Manually completing MoMo payment");
        log.info("   Order ID: {}", orderId);

        try {
            // Find payment by transaction reference
            Optional<Payment> paymentOpt = paymentRepository.findByPaymentReference(orderId);

            if (paymentOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Payment not found for orderId: " + orderId));
            }

            Payment payment = paymentOpt.get();
            User user = payment.getPayer();

            // Check if already completed
            if (payment.getPaymentStatus() == Payment.PaymentStatus.COMPLETED) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "Payment already completed",
                        "paymentId", payment.getId()));
            }

            // Update payment status to COMPLETED
            payment.setPaymentStatus(Payment.PaymentStatus.COMPLETED);
            paymentRepository.save(payment);
            log.info("✅ Payment status updated to COMPLETED");

            // Update wallet balance
            walletService.updateCashBalance(user.getId(), payment.getAmount());
            log.info("✅ Wallet balance updated: +${} USD", payment.getAmount());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "🧪 TEST MODE - MoMo payment completed successfully",
                    "paymentId", payment.getId(),
                    "orderId", orderId,
                    "amountUsd", payment.getAmount(),
                    "status", "COMPLETED",
                    "user", user.getUsername()));

        } catch (Exception e) {
            log.error("❌ Error completing test MoMo payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error: " + e.getMessage()));
        }
    }

    /**
     * Create MoMo payment
     */
    @PostMapping("/deposit/momo")
    public ResponseEntity<Map<String, Object>> depositViaMoMo(
            @RequestBody @Valid VNPayDepositRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        try {
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            log.info("User {} initiating MoMo deposit", user.getUsername());
            log.info("Amount: ${} USD", request.getAmountUsd());

            // Convert USD to VND
            BigDecimal amountVnd = currencyService.convertUsdToVnd(request.getAmountUsd());
            BigDecimal exchangeRate = currencyService.getExchangeRate();
            log.info("Converted: {} VND (rate: {})", amountVnd, exchangeRate);

            // Generate unique transaction reference
            String txnRef = "DEP_" + System.currentTimeMillis();

            // Create payment record
            Payment payment = new Payment();
            payment.setPayer(user);
            payment.setPayee(null);
            payment.setAmount(request.getAmountUsd());
            payment.setPaymentMethod(Payment.PaymentMethod.BANK_TRANSFER);
            payment.setPaymentStatus(Payment.PaymentStatus.PENDING);
            payment.setPaymentReference(txnRef);
            payment = paymentRepository.save(payment);

            log.info("   Payment record created: {}", payment.getId());

            // Create MoMo payment URL
            String paymentUrl = momoService.createPayment(
                    user.getId(),
                    request.getAmountUsd(),
                    txnRef);

            log.info("✅ MoMo payment URL generated successfully");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "paymentUrl", paymentUrl,
                    "paymentId", payment.getId(),
                    "orderId", txnRef,
                    "amountUsd", request.getAmountUsd(),
                    "amountVnd", amountVnd.longValue(),
                    "exchangeRate", exchangeRate,
                    "message", String.format("You will pay %,d VND (≈ $%.2f USD)",
                            amountVnd.longValue(), request.getAmountUsd())));

        } catch (Exception e) {
            log.error("❌ Error creating MoMo payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Payment creation failed: " + e.getMessage()));
        }
    }

    /**
     * MoMo return callback (user redirected here after payment)
     */
    @GetMapping("/momo-return")
    public ResponseEntity<Map<String, Object>> momoReturn(@RequestParam Map<String, String> params) {
        log.info("🔄 MoMo callback received");
        log.info("Result Code: {}", params.get("resultCode"));
        log.info("Order ID: {}", params.get("orderId"));
        log.info("Amount: {} VND", params.get("amount"));

        // Process same as notify
        return momoNotify(params);
    }

    /**
     * MoMo IPN callback (server-to-server notification)
     */
    @PostMapping("/momo-notify")
    public ResponseEntity<Map<String, Object>> momoNotify(@RequestParam Map<String, String> params) {
        log.info("🔔 MoMo IPN notification received");

        try {
            String resultCode = params.get("resultCode");
            String orderId = params.get("orderId"); // This is our txnRef
            String amount = params.get("amount");
            String signature = params.get("signature");

            // Build raw signature for verification
            String rawSignature = String.format(
                    "accessKey=%s&amount=%s&extraData=%s&message=%s&orderId=%s&orderInfo=%s&orderType=%s&partnerCode=%s&payType=%s&requestId=%s&responseTime=%s&resultCode=%s&transId=%s",
                    momoConfig.getAccessKey(),
                    amount,
                    params.getOrDefault("extraData", ""),
                    params.getOrDefault("message", ""),
                    orderId,
                    params.getOrDefault("orderInfo", ""),
                    params.getOrDefault("orderType", ""),
                    momoConfig.getPartnerCode(),
                    params.getOrDefault("payType", ""),
                    params.getOrDefault("requestId", ""),
                    params.getOrDefault("responseTime", ""),
                    resultCode,
                    params.getOrDefault("transId", ""));

            // Verify signature
            if (!momoService.verifySignature(rawSignature, signature)) {
                log.warn("❌ Invalid MoMo signature - possible tampering");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Invalid signature"));
            }

            // Check if payment was successful (resultCode "0" means success)
            if ("0".equals(resultCode)) {
                log.info("✅ MoMo payment successful");

                // Find payment record
                Optional<Payment> paymentOpt = paymentRepository.findByPaymentReference(orderId);
                if (paymentOpt.isPresent()) {
                    Payment payment = paymentOpt.get();
                    User user = payment.getPayer();

                    // Update payment status
                    payment.setPaymentStatus(Payment.PaymentStatus.COMPLETED);
                    payment.setPaymentReference(params.get("transId")); // MoMo transaction ID
                    paymentRepository.save(payment);
                    log.info("   Payment record updated: {}", payment.getId());

                    // Update wallet balance
                    walletService.updateCashBalance(user.getId(), payment.getAmount());
                    log.info("✅ Wallet updated for user: {}", user.getUsername());
                    log.info("   Added: ${} USD", payment.getAmount());

                    return ResponseEntity.ok(Map.of(
                            "message", "Payment successful",
                            "orderId", orderId));
                } else {
                    log.error("❌ Payment record not found for orderId: {}", orderId);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("message", "Payment not found"));
                }
            } else {
                log.warn("⚠️ MoMo payment failed. Result code: {}", resultCode);

                // Update payment status to failed
                paymentRepository.findByPaymentReference(orderId).ifPresent(payment -> {
                    payment.setPaymentStatus(Payment.PaymentStatus.FAILED);
                    paymentRepository.save(payment);
                });

                return ResponseEntity.ok(Map.of(
                        "message", "Payment failed",
                        "resultCode", resultCode));
            }

        } catch (Exception e) {
            log.error("❌ Error processing MoMo callback", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Callback processing failed: " + e.getMessage()));
        }
    }

    /**
     * 
     * Initiate VNPAY deposit (USD input, VND payment)
     */
    @PostMapping("/deposit/vnpay")
    public ResponseEntity<?> depositViaVNPay(@RequestBody @Valid VNPayDepositRequest request,
            Authentication authentication, HttpServletRequest httpRequest) {
        try {
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String ipAddress = getClientIp(httpRequest);

            log.info("User {} initiating VNPay deposit", user.getUsername());
            log.info("Amount: ${} USD", request.getAmountUsd());

            // Convert USD to VND for display
            BigDecimal amountVnd = currencyService.convertUsdToVnd(request.getAmountUsd());
            BigDecimal exchangeRate = currencyService.getExchangeRate();

            log.info("Converted: {} VND (rate: {})", amountVnd, exchangeRate);

            // Generate transaction reference BEFORE creating payment
            String txnRef = "DEP_" + System.currentTimeMillis();

            // Create pending payment record (store in USD)
            Payment payment = new Payment();
            payment.setPayer(user);
            payment.setPayee(null);
            payment.setAmount(request.getAmountUsd()); // Store in USD
            payment.setPaymentMethod(Payment.PaymentMethod.BANK_TRANSFER);
            payment.setPaymentStatus(Payment.PaymentStatus.PENDING);
            payment.setPaymentReference(txnRef);
            payment = paymentRepository.save(payment);

            log.info("   Payment record created: {}", payment.getId());

            // Generate VNPAY payment URL (converts USD to VND internally)
            String paymentUrl = vnPayService.createPaymentUrl(
                    user.getId(),
                    request.getAmountUsd(),
                    ipAddress, txnRef);
            log.info("✅ VNPAY payment URL generated successfully");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "paymentUrl", paymentUrl,
                    "paymentId", payment.getId(),
                    "amountUsd", request.getAmountUsd(),
                    "amountVnd", amountVnd,
                    "exchangeRate", exchangeRate,
                    "message", String.format("You will pay %,.0f VND (≈ $%.2f USD)",
                            amountVnd, request.getAmountUsd())));

        } catch (Exception e) {
            log.error("Error creating VNPAY payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * VNPAY return callback
     * Called by VNPAY after user completes payment
     */
    @GetMapping("/vnpay-return")
    public ResponseEntity<?> vnpayReturn(@RequestParam Map<String, String> params) {
        try {
            log.info("VNPAY callback received");
            log.info("Response CodeL {}", params.get("vnp_ResponseCode"));
            log.info("TxnRef: {}", params.get("vnp_TxnRef"));
            log.info("Amount: {} VND cents", params.get("vnp_Amount"));

            // Verify signature
            boolean isValid = vnPayService.verifyReturnUrl(params);
            if (!isValid) {
                log.warn("Invalid VNPAY signature - possible tampering");
                return redirectToFrontend("invalid", "signature_eror");
            }

            // extact payment details
            String responseCode = params.get("vnp_ResponseCode");
            String amountStr = params.get("vnp_Amount");
            String txnRef = params.get("vnp_TxnRef");
            String transactionNo = params.get("vnp_TransactionNo");
            String bankCode = params.get("vnp_BankCode");

            if ("00".equals(responseCode)) {
                // SUCCESS - Payment completed
                BigDecimal amountVnd = new BigDecimal(amountStr).divide(new BigDecimal("100"));
                BigDecimal amountUsd = currencyService.convertVndToUsd(amountVnd);

                log.info("Payment successfull!");
                log.info("VND: {}", amountVnd);
                log.info("Transaction No: {}", transactionNo);
                log.info("Bank: {}", bankCode);

                // TODO: Update wallet balance and payment status
                // Find payment by txnRef or use other method
                // walletService.updateCashBalance(userId, amountUsd);
                // payment.setPaymentStatus(Payment.PaymentStatus.COMPLETED);
                // payment.setPaymentReference(transactionNo);
                Optional<Payment> paymentOpt = paymentRepository.findByPaymentReference(txnRef);

                if (paymentOpt.isPresent()) {
                    Payment payment = paymentOpt.get();
                    User user = payment.getPayer();

                    // update payment status and reference
                    payment.setPaymentStatus(Payment.PaymentStatus.COMPLETED);
                    payment.setPaymentReference(transactionNo);
                    paymentRepository.save(payment);

                    log.info("Payment record updated: {}", payment.getId());

                    // Update wallet balance
                    walletService.updateCashBalance(user.getId(), amountUsd);

                    log.info(" Wallet updated for user: {}", user.getUsername());
                    log.info("Added: ${} USD", amountUsd);
                    return redirectToFrontend("success", amountUsd.toString());
                } else {
                    log.error("Payment recored not found for txnRef: {}", txnRef);
                    return redirectToFrontend("error", "payment_not_found");
                }

            } else {
                // Failed - Payment declined/cancelled
                String errorMessage = getVNPayErrorMessage(responseCode);

                log.warn("Payment failed");
                log.warn("Response Code: {}", responseCode);
                log.warn("Error: {}", errorMessage);

                // TODO: Update payment status to FAILED
                Optional<Payment> paymentOpt = paymentRepository.findByPaymentReference(txnRef);
                if (paymentOpt.isPresent()) {
                    Payment payment = paymentOpt.get();
                    payment.setPaymentStatus(Payment.PaymentStatus.FAILED);
                    paymentRepository.save(payment);

                    log.info("   Payment marked as FAILED: {}", payment.getId());
                }
                return redirectToFrontend("failed", responseCode);
            }
        } catch (Exception e) {
            log.error("Error processing VNPAY callback", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Get VNPAY error message by response code
     */
    private String getVNPayErrorMessage(String responseCode) {
        Map<String, String> errorMessage = Map.ofEntries(Map.entry("07", "Suspicious transaction"),
                Map.entry("09", "Card not registered for internet banking"),
                Map.entry("10", "Incorrect OTP"),
                Map.entry("11", "Transaction timeout"),
                Map.entry("12", "Card locked"),
                Map.entry("13", "Incorrect transaction password"),
                Map.entry("24", "Transaction cancelled by user"),
                Map.entry("51", "Insufficient balance"),
                Map.entry("65", "Daily transaction limit exceeded"),
                Map.entry("75", "Payment bank under maintenance"),
                Map.entry("79", "Transaction timeout (payment took too long)"));
        return errorMessage.getOrDefault(responseCode, "Unknown error: " + responseCode);
    }

    /**
     * Get client IP address
     * FIXED HERE WHEN DEPLOY
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        // Handle localhost IPv6
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }

    /**
     * Redirect to frontend with status
     */
    private ResponseEntity<?> redirectToFrontend(String status, String detail) {
        String url = "http://localhost:3000/wallet?status=" + status;
        if (detail != null) {
            url += "&detail=" + detail;
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", url)
                .build();
    }

    // Get current user's wallet information
    @GetMapping("/my-wallet")
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet(Authentication authentication) {
        log.info("📥 GET /api/wallets/my-wallet called by: {}", authentication.getName());
        try {
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "username", authentication.getName()));

            Wallet wallet = walletService.getOrCreateWallet(user);
            WalletResponse response = mapToWalletResponse(wallet);

            log.info("✅ Wallet information retrieved for user: {}", user.getUsername());
            log.info("   Credit Balance: {}", response.getCreditBalance());
            log.info("   Cash Balance: {}", response.getCashBalance());

            // ✅ Wrap in ApiResponse for consistency
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (ResourceNotFoundException e) {
            log.error("❌ User not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error retrieving wallet: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve wallet: " + e.getMessage()));
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
