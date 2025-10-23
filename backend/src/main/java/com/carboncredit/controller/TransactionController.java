package com.carboncredit.controller;

import com.carboncredit.dto.ApiResponse;
import com.carboncredit.dto.TransactionDTO; // Make sure this DTO exists
import com.carboncredit.entity.Transaction; // Keep entity import if service returns it sometimes
import com.carboncredit.entity.Transaction.TransactionStatus; // Import status enum
import com.carboncredit.entity.User;
import com.carboncredit.exception.BusinessOperationException;
import com.carboncredit.exception.EntityNotFoundException; // Assuming this exists
import com.carboncredit.exception.InsufficientBalanceException;
import com.carboncredit.exception.PaymentException;
import com.carboncredit.exception.ResourceNotFoundException; // Assuming this exists
import com.carboncredit.exception.UnauthorizedOperationException; // Assuming this exists
import com.carboncredit.service.TransactionService;
import com.carboncredit.service.UserService;
import com.carboncredit.util.DTOMapper; // Keep if you still need it for single entities

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime; // For future date range endpoints
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TransactionController {
    
    private final TransactionService transactionService;
    private final UserService userService;
    // No DTOMapper here, assuming service returns DTOs

    /**
     * Helper to get the authenticated User entity.
     */
    private User getCurrentUser(Authentication authentication) {
        return userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", authentication.getName()));
    }

    // ==========================================================
    // PURCHASE INITIATION
    // ==========================================================

    /**
     * (BUYER) Initiates the purchase of a fixed-price credit listing.
     * This triggers the entire transaction flow including payment and transfers.
     */
    @PostMapping("/purchase/{listingId}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<ApiResponse<TransactionDTO>> initiatePurchase(
            @PathVariable UUID listingId,
            Authentication authentication) {
        
        log.info("Received purchase request for listing {} from user {}", listingId, authentication.getName());
        try {
            User buyer = getCurrentUser(authentication);
            
            // Initiate purchase - service handles payment and completion/failure internally
            Transaction transaction = transactionService.initiatePurchase(listingId, buyer);
            
            // The service returns the final state (COMPLETED or CANCELLED)
            TransactionDTO transactionDTO = DTOMapper.toTransactionDTO(transaction); // Map the final entity state
            
            HttpStatus status = transaction.getStatus() == TransactionStatus.COMPLETED ? HttpStatus.CREATED : HttpStatus.ACCEPTED; // Accepted if pending async payment
            String message = transaction.getStatus() == TransactionStatus.COMPLETED ? "Purchase completed successfully" : "Purchase initiated, payment pending";
            
             if (transaction.getStatus() == TransactionStatus.CANCELLED) {
                 // If initiatePurchase itself failed synchronously (e.g., payment error caught)
                 log.warn("Purchase initiation resulted in cancellation for listing {}", listingId);
                 // Need more context on why it failed from the exception ideally
                 return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                     .body(ApiResponse.error("Purchase failed during initiation. Status: " + transaction.getStatus(), transactionDTO));
             }

            return ResponseEntity.status(status).body(ApiResponse.success(message, transactionDTO));

        } catch (EntityNotFoundException | ResourceNotFoundException e) {
            log.warn("Purchase failed - resource not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (InsufficientBalanceException e) {
             log.warn("Purchase failed - insufficient funds: {}", e.getMessage());
            // HttpStatus.PAYMENT_REQUIRED (402) is automatically handled by @ResponseStatus on the exception
             return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(ApiResponse.error(e.getMessage()));
        } catch (PaymentException e) {
            log.error("Purchase failed - payment error: {}", e.getMessage());
            // Consider returning 402 or 500 depending on the nature of payment failure
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Payment processing failed: " + e.getMessage()));
        } catch (BusinessOperationException | IllegalArgumentException e) {
            log.warn("Purchase failed - invalid request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during purchase initiation for listing {}: ", listingId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred during purchase."));
        }
    }

    // ==========================================================
    // TRANSACTION HISTORY & QUERYING
    // ==========================================================

    /**
     * (Authenticated User) Get transaction history (both bought and sold).
     */
    @GetMapping("/my-history")
    public ResponseEntity<ApiResponse<Page<TransactionDTO>>> getMyTransactions(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("Fetching transaction history for user {}", authentication.getName());
        try {
            User currentUser = getCurrentUser(authentication);
            Page<TransactionDTO> dtoPage = transactionService.getUserTransactions(currentUser, page, size);
            return ResponseEntity.ok(ApiResponse.success(dtoPage));
        } catch (Exception e) {
            log.error("Error fetching transaction history for user {}: ", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve transaction history."));
        }
    }

    /**
     * (Buyer/Seller/Admin) Get a single transaction by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CVA', 'BUYER', 'EV_OWNER')") // Allow involved parties + admins
    public ResponseEntity<ApiResponse<TransactionDTO>> getTransactionById(
            @PathVariable UUID id,
            Authentication authentication) {
        
        log.info("Fetching transaction by ID: {}", id);
        try {
            User currentUser = getCurrentUser(authentication);
            TransactionDTO transactionDTO = transactionService.findTransactionDtoById(id);

            // Security Check: User must be buyer, seller, or admin/CVA
            boolean isAdminOrCva = currentUser.getRole() == User.UserRole.ADMIN || currentUser.getRole() == User.UserRole.CVA;
            boolean isBuyer = transactionDTO.getBuyer().getId().equals(currentUser.getId());
            boolean isSeller = transactionDTO.getSeller().getId().equals(currentUser.getId());

            if (!isAdminOrCva && !isBuyer && !isSeller) {
                log.warn("User {} attempted to access transaction {} without permission", currentUser.getUsername(), id);
                throw new AccessDeniedException("You do not have permission to view this transaction.");
            }

            return ResponseEntity.ok(ApiResponse.success(transactionDTO));

        } catch (EntityNotFoundException e) { // Catch specific exception from service
            log.warn("Transaction not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching transaction {}: ", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred."));
        }
    }

    // ==========================================================
    // TRANSACTION CANCELLATION
    // ==========================================================

    /**
     * (Buyer/Seller/Admin) Cancel a PENDING transaction.
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'CVA', 'BUYER', 'EV_OWNER')") // Allow involved parties + admins
    public ResponseEntity<ApiResponse<TransactionDTO>> cancelTransaction(
            @PathVariable UUID id,
            Authentication authentication) {
        
        log.info("User {} attempting to cancel transaction {}", authentication.getName(), id);
        try {
            User currentUser = getCurrentUser(authentication);
            
            // Service handles authorization check (canCancelTransaction)
            Transaction cancelledTransaction = transactionService.cancelTransaction(id, currentUser);
            TransactionDTO transactionDTO = DTOMapper.toTransactionDTO(cancelledTransaction); // Map final entity state

            return ResponseEntity.ok(ApiResponse.success("Transaction cancelled successfully", transactionDTO));

        } catch (EntityNotFoundException e) {
            log.warn("Cannot cancel - transaction not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (BusinessOperationException | UnauthorizedOperationException e) {
            log.warn("Cancellation failed for transaction {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error cancelling transaction {}: ", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred during cancellation."));
        }
    }

    // ==========================================================
    // ADMIN ENDPOINTS (Optional - Add as needed)
    // ==========================================================

    /**
     * (ADMIN) Get all transactions (paginated).
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<TransactionDTO>>> getAllTransactions(
             @RequestParam(defaultValue = "0") int page,
             @RequestParam(defaultValue = "20") int size) { // Default size higher for admin view
         log.info("Admin fetching all transactions, page {}, size {}", page, size);
         try {
             // Assuming TransactionService has a findAll method similar to others
             // Page<TransactionDTO> dtoPage = transactionService.findAllTransactions(page, size);
             // return ResponseEntity.ok(ApiResponse.success(dtoPage));
             
             // --- TEMPORARY Placeholder if findAllTransactions doesn't exist yet ---
             return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                 .body(ApiResponse.error("findAllTransactions not yet implemented in service"));
             // --- END Placeholder ---
             
         } catch (Exception e) {
             log.error("Error fetching all transactions for admin: ", e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                     .body(ApiResponse.error("Failed to retrieve all transactions."));
         }
     }

    /**
     * (ADMIN) Get transactions by status (paginated).
     */
    @GetMapping("/admin/by-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<TransactionDTO>>> getTransactionsByStatus(
             @RequestParam String status,
             @RequestParam(defaultValue = "0") int page,
             @RequestParam(defaultValue = "10") int size) {
         log.info("Admin fetching transactions with status: {}", status);
         try {
             TransactionStatus transactionStatus = TransactionStatus.valueOf(status.toUpperCase());
             Page<TransactionDTO> dtoPage = transactionService.getTransactionsByStatus(transactionStatus, page, size);
             return ResponseEntity.ok(ApiResponse.success(dtoPage));
         } catch (IllegalArgumentException e) {
             log.warn("Invalid status requested by admin: {}", status);
             return ResponseEntity.badRequest().body(ApiResponse.error("Invalid status value: " + status));
         } catch (Exception e) {
             log.error("Error fetching transactions by status for admin: ", e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                     .body(ApiResponse.error("Failed to retrieve transactions by status."));
         }
     }
}