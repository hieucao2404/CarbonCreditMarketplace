package com.carboncredit.service;

import java.lang.foreign.Linker.Option;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.carboncredit.entity.CarbonCredit;
import com.carboncredit.entity.CarbonCredit.CreditStatus;
import com.carboncredit.entity.CreditListing;
import com.carboncredit.entity.CreditListing.ListingStatus;
import com.carboncredit.entity.CreditListing.ListingType;
import com.carboncredit.entity.Dispute;
import com.carboncredit.entity.Dispute.DisputeStatus;
import com.carboncredit.entity.JourneyData;
import com.carboncredit.entity.Transaction;
import com.carboncredit.entity.Transaction.TransactionStatus;
import com.carboncredit.entity.User;
import com.carboncredit.exception.BusinessOperationException;
import com.carboncredit.exception.UnauthorizedOperationException;
import com.carboncredit.exception.ValidationException;
import com.carboncredit.repository.DisputeRepository;
import com.carboncredit.repository.TransactionRepository;

@Service
public class ValidationService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private DisputeRepository disputeRepository;

    // ======================== TRANSACTION VALIDATIONS ===============

    // validate transction purchase request
    public void validatePurchaseRequest(CreditListing listing, User buyer) {
        // null checks
        if (listing == null) {
            throw new ValidationException("CreditListing", "listing", "Listing cannot be null");
        }

        if (buyer == null) {
            throw new ValidationException("User", "buyer", "Buyer cannot be null");
        }

        // Lisitn valiadtion
        validateListingForPurchase(listing);

        // credit validation
        validateCreditForPurchase(listing.getCredit());

        // Selfo-purchase validation
        if (listing.getCredit().getUser().getId().equals(buyer.getId())) {
            throw new BusinessOperationException("Cannot purchase your own carbon credit");
        }

        // User authoruzation validation
        validateUserCanBuy(buyer);

        // Check for concurrent transcations
        validateNoConcurrentTransactions(listing.getId());
    }

    // validate listing is available for pruchase
    public void validateListingForPurchase(CreditListing listing) {
        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new BusinessOperationException("Listing is not active. Status: " + listing.getStatus());
        }

        if (listing.getListingType() != ListingType.FIXED) {
            throw new BusinessOperationException("Can only purchase fixed-price lisitings directly");
        }

        validatePrice(listing.getPrice());
    }

    // Validate credit is available for purchase
    public void validateCreditForPurchase(CarbonCredit credit) {
        validateCarbonCredit(credit);

        if (credit.getStatus() != CreditStatus.LISTED) {
            throw new BusinessOperationException("Credit is not availble for purchase, Status: " + credit.getStatus());
        }
    }

    // validate no concurrent transactions for lisitng
    public void validateNoConcurrentTransactions(UUID listingId) {
        Optional<Transaction> existingTransaction = transactionRepository.findPendingTransactionForListing(listingId);

        if (existingTransaction.isPresent()) {
            throw new BusinessOperationException("Listing already has a pending transaction");
        }
    }

    // validate transaction preconditions (during processing)
    public void validateTransactionPreconditions(Transaction transaction, CreditListing currentListing,
            CarbonCredit currentCredit) {
        if (transaction == null) {
            throw new ValidationException("Transaction", "transaction", "Transaction cannot be null");
        }

        // validate listing exist and is active
        if (currentCredit == null) {
            throw new BusinessOperationException("Listing no longer exists");
        }
        if (currentListing.getStatus() != ListingStatus.ACTIVE) {
            throw new BusinessOperationException("Lisitng  no longer active");
        }

        // validate credit stil exisit and available
        if (currentCredit == null) {
            throw new BusinessOperationException("Credit no longer exists");
        }
        if (currentCredit.getStatus() != CreditStatus.LISTED) {
            throw new BusinessOperationException("Credit is no longer available for purchase");
        }

        // validate transaction amoutn matches current listing price
        if (transaction.getAmount().compareTo(currentListing.getPrice()) != 0) {
            throw new BusinessOperationException("Transcation does not match current listing price");
        }

    }

    // validate transaction limits and security
    public void validateTranscationSecurity(Transaction transaction) {
        User buyer = transaction.getBuyer();
        BigDecimal amount = transaction.getAmount();

        // check for suspicious activity patterns
        validateSuspiciousActivity(buyer);

        // Validate transaction limits
        validateTransactionLimits(buyer, amount);

        // Fraud preventation checks
        validateFraudPreventation(transaction);
    }

    // validate suspicious activity patterns
    public void validateSuspiciousActivity(User buyer) {
        // Check for rapid consecutive transactions
        long recentTransactions = transactionRepository.countRecentTransactionsByUser(buyer.getId(),
                LocalDateTime.now().minusMinutes(15));

        if (recentTransactions > 5) {
            throw new SecurityException("Suspicious activity: Too many recent transcations (limit: 5 / 15mins)");
        }

        // check for high-frequencey trading patterns
        long todayTransactions = transactionRepository.countTodayTransactionsByUser(buyer.getId());

        if (todayTransactions > 20) {
            throw new SecurityException("Daily transaction limit exceeded (20/day)");
        }
    }

    // validate transaction limits
    public void validateTransactionLimits(User buyer, BigDecimal amount) {
        // Sing transaction limit
        BigDecimal singleTransactionLimit = new BigDecimal("10000.00");
        if (amount.compareTo(singleTransactionLimit) > 0) {
            throw new SecurityException(
                    "Transaction amount exceeds single transaction limit: $" + singleTransactionLimit);
        }

        // Daily spending limit
        BigDecimal dailySpent = transactionRepository.getDailySpentByUser(buyer.getId());
        BigDecimal dailyLimit = new BigDecimal("5000.00");
        if (dailySpent.add(amount).compareTo(dailyLimit) > 0) {
            throw new SecurityException("Transcation would exceed daily spending limit: $" + dailyLimit);
        }
    }

    // validate fraud prevention
    public void validateFraudPreventation(Transaction transaction) {
        // check for unsual patterns comapared to user' history
        Optional<BigDecimal> averageTransaction = transactionRepository.getAverageTransactionAmountByUser(transaction.getBuyer().getId());

        if(averageTransaction.isPresent() && averageTransaction.get().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ratio = transaction.getAmount().divide(averageTransaction.get(), 2, RoundingMode.HALF_UP);
            if(ratio.compareTo(new BigDecimal("5")) > 0) {
                throw new SecurityException("Transaction amount significantly exceeds user's typical spending patterns");
            }

        }
    }

    // ================= DISPUTE VALIDATIONS =============
    
    //validate dispute creations rights
    public void validateDisputeCreationRights(Transaction transaction, User user) {
        if(transaction == null) {
            throw new ValidationException("Transaction", "transaction", "Transcation cannot be null");
        }
        if(user == null) {
            throw new ValidationException("User", "user", "User cannot be null");
        }

        //only buyer, seller can create dispute
        if(!transaction.getBuyer().getId().equals(user.getId()) &&
        !transaction.getSeller().getId().equals(user.getId())) {
            throw new UnauthorizedOperationException("Only transcation pariticipants");
        }

        // cannot dispute cancelled transcations
        if(transaction.getStatus() == TransactionStatus.CANCELLED) {
            throw new BusinessOperationException("Cannot dispute cancelled transaction");
        }

        //Time limit for disputing completed transcations
        if(transaction.getStatus() == TransactionStatus.COMPLETED) {
            if(transaction.getCompletedAt() != null && transaction.getCompletedAt().plusDays(30).isBefore(LocalDateTime.now())){
                throw new BusinessOperationException("Cannot dispute transcation completed over 30 days ago");
            } 
        }
    }

    /**
     * Validate no existing open disputes
     */
    public void validateNoExistingOpenDisputes(UUID transactionId) {
        List<Dispute> existingDisputes = disputeRepository.findByTransactionIdAndStatus(
                transactionId, DisputeStatus.OPEN);
        
        if (!existingDisputes.isEmpty()) {
            throw new BusinessOperationException("An open dispute already exists for this transaction");
        }
    }

    /**
     * Validate dispute resolution authority
     */
    public void validateDisputeResolutionAuthority(User user) {
        if (user == null) {
            throw new ValidationException("User", "user", "User cannot be null");
        }

        if (user.getRole() != User.UserRole.ADMIN && user.getRole() != User.UserRole.CVA) {
            throw new UnauthorizedOperationException("Insufficient authority to resolve disputes");
        }
    }

    /**
     * Validate dispute can be resolved
     */
    public void validateDisputeCanBeResolved(Dispute dispute) {
        if (dispute == null) {
            throw new ValidationException("Dispute", "dispute", "Dispute cannot be null");
        }

        if (dispute.getStatus() != DisputeStatus.OPEN) {
            throw new BusinessOperationException("Only open disputes can be resolved. Current status: " + dispute.getStatus());
        }
    }

    // valdate transaction status change
    public void validateTransactionStatusChange(Transaction transaction, TransactionStatus newStatus) {
        if (transaction == null) {
            throw new ValidationException("Transaction", "transaction", "Transaction cannot be null");
        }

        TransactionStatus currentStatus = transaction.getStatus();

        // Define valid status transitions
        switch (currentStatus) {
            case PENDING:
                if (newStatus != TransactionStatus.COMPLETED &&
                        newStatus != TransactionStatus.CANCELLED &&
                        newStatus != TransactionStatus.DISPUTED) {
                    throw new BusinessOperationException("Invalid status transition from PENDING to " + newStatus);
                }
                break;
            case COMPLETED:
                if (newStatus != TransactionStatus.DISPUTED) {
                    throw new BusinessOperationException("Completed transactions can only be disputed");
                }
                break;
            case CANCELLED:
                throw new BusinessOperationException("Cannot change status of cancelled transaction");
            case DISPUTED:
                if (newStatus != TransactionStatus.COMPLETED && newStatus != TransactionStatus.CANCELLED) {
                    throw new BusinessOperationException("Disputed transactions can only be completed or cancelled");
                }
                break;
        }
    }


     public void validateDisputeReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new ValidationException("Dispute", "reason", "Dispute reason cannot be null or empty");
        }

        if (reason.length() > 1000) {
            throw new ValidationException("Dispute", "reason", "Dispute reason cannot exceed 1000 characters");
        }
    }

    /**
     * Validate dispute resolution
     */
    public void validateDisputeResolution(String resolution) {
        if (resolution == null || resolution.trim().isEmpty()) {
            throw new ValidationException("Dispute", "resolution", "Dispute resolution cannot be null or empty");
        }

        if (resolution.length() > 2000) {
            throw new ValidationException("Dispute", "resolution", "Dispute resolution cannot exceed 2000 characters");
        }
    }

    // ==================== CREDIT LISTING VALIDATIONS ====================

    /**
     * Validate listing creation business rules
     */
    public void validateListingCreation(CarbonCredit credit, User owner, BigDecimal price) {
        // Check ownership
        if (!credit.getUser().getId().equals(owner.getId())) {
            throw new UnauthorizedOperationException("Only credit owner can create listings");
        }

        // Check credit status - must be VERIFIED
        if (credit.getStatus() != CreditStatus.VERIFIED) {
            throw new BusinessOperationException("Only verified credits can be listed in marketplace");
        }

        // Validate price
        validatePrice(price);

        // Validate user can create listings
        validateUserCanList(owner);
    }

    /**
     * Validate listing ownership
     */
    public void validateOwnership(CreditListing listing, User owner) {
        if (listing == null) {
            throw new ValidationException("CreditListing", "listing", "Listing cannot be null");
        }

        if (owner == null) {
            throw new ValidationException("User", "owner", "Owner cannot be null");
        }

        if (!listing.getCredit().getUser().getId().equals(owner.getId())) {
            throw new UnauthorizedOperationException("Only listing owner can perform this operation");
        }
    }

    /**
     * Validate purchase operation
     */
    public void validatePurchase(CreditListing listing, User buyer) {
        if (listing == null) {
            throw new ValidationException("CreditListing", "listing", "Listing cannot be null");
        }

        if (buyer == null) {
            throw new ValidationException("User", "buyer", "Buyer cannot be null");
        }

        // Cannot buy your own listing
        if (listing.getCredit().getUser().getId().equals(buyer.getId())) {
            throw new BusinessOperationException("Cannot purchase your own listing");
        }

        // Check buyer has valid role
        validateUserCanBuy(buyer);

        // Check listing is purchasable
        if (listing.getListingType() != ListingType.FIXED) {
            throw new BusinessOperationException("Can only directly purchase fixed-price listings");
        }
    }

    // ==================== PRICE VALIDATIONS ====================

    /**
     * Validate price values
     */
    public void validatePrice(BigDecimal price) {
        if (price == null) {
            throw new ValidationException("CreditListing", "price", "Price cannot be null");
        }

        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("CreditListing", "price", "Price must be positive");
        }

        // Set reasonable price limits
        if (price.compareTo(new BigDecimal("10000")) > 0) {
            throw new ValidationException("CreditListing", "price", "Price exceeds maximum allowed limit of $10,000");
        }

        // Check for reasonable precision (max 2 decimal places)
        if (price.scale() > 2) {
            throw new ValidationException("CreditListing", "price", "Price cannot have more than 2 decimal places");
        }
    }

    /**
     * Validate price range for searches
     */
    public void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null) {
            validatePrice(minPrice);
        }

        if (maxPrice != null) {
            validatePrice(maxPrice);
        }

        if (minPrice != null && maxPrice != null) {
            if (minPrice.compareTo(maxPrice) > 0) {
                throw new ValidationException("PriceRange", "minPrice/maxPrice",
                        "Minimum price cannot be greater than maximum price");
            }
        }
    }

    // ==================== USER VALIDATIONS ====================

    /**
     * Validate user entity
     */
    public void validateUser(User user) {
        if (user == null) {
            throw new ValidationException("User", "user", "User cannot be null");
        }

        if (user.getId() == null) {
            throw new ValidationException("User", "id", "User ID cannot be null");
        }

        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new ValidationException("User", "username", "Username cannot be null or empty");
        }
    }

    /**
     * Validate user can create listings
     */
    public void validateUserCanList(User user) {
        validateUser(user);

        if (user.getRole() == null) {
            throw new UnauthorizedOperationException("User role not defined");
        }

        // Only EV_OWNER and ADMIN can create listings
        if (user.getRole() != User.UserRole.EV_OWNER && user.getRole() != User.UserRole.ADMIN) {
            throw new UnauthorizedOperationException("Only EV owners can create credit listings");
        }
    }

    /**
     * Validate user can purchase listings
     */
    public void validateUserCanBuy(User user) {
        validateUser(user);

        if (user.getRole() == null) {
            throw new UnauthorizedOperationException("User role not defined");
        }

        // Only BUYER and ADMIN can purchase listings
        if (user.getRole() != User.UserRole.BUYER && user.getRole() != User.UserRole.ADMIN) {
            throw new UnauthorizedOperationException("User not authorized to purchase credits");
        }
    }

    // ==================== PAGINATION VALIDATIONS ====================

    /**
     * Validate pagination parameters
     */
    public void validatePageParameters(int page, int size) {
        if (page < 0) {
            throw new ValidationException("Pagination", "page", "Page number cannot be negative");
        }

        if (size <= 0) {
            throw new ValidationException("Pagination", "size", "Page size must be positive");
        }

        if (size > 100) {
            throw new ValidationException("Pagination", "size", "Page size cannot exceed 100 items");
        }
    }

    // ==================== CARBON CREDIT VALIDATIONS ====================

    /**
     * Validate carbon credit entity
     */
    public void validateCarbonCredit(CarbonCredit credit) {
        if (credit == null) {
            throw new ValidationException("CarbonCredit", "credit", "Carbon credit cannot be null");
        }

        if (credit.getId() == null) {
            throw new ValidationException("CarbonCredit", "id", "Carbon credit ID cannot be null");
        }

        if (credit.getCo2ReducedKg() == null || credit.getCo2ReducedKg().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("CarbonCredit", "co2ReducedKg", "CO2 reduction must be positive");
        }
    }

    /**
     * Validate journey data for carbon credit creation
     */
    public void validateJourneyForCredit(BigDecimal distanceKm, BigDecimal energyConsumedKwh, BigDecimal co2ReducedKg) {
        if (distanceKm == null || distanceKm.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("JourneyData", "distanceKm", "Distance must be positive");
        }

        if (energyConsumedKwh == null || energyConsumedKwh.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("JourneyData", "energyConsumedKwh", "Energy consumption must be positive");
        }

        if (co2ReducedKg == null || co2ReducedKg.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("JourneyData", "co2ReducedKg", "CO2 reduction must be positive");
        }

        // Validate reasonable ranges
        if (distanceKm.compareTo(new BigDecimal("10000")) > 0) {
            throw new ValidationException("JourneyData", "distanceKm", "Distance exceeds reasonable limit (10,000 km)");
        }
    }

    // ==================== COMMON VALIDATIONS ====================

    /**
     * Validate UUID is not null
     */
    public void validateId(UUID id, String entityType) {
        if (id == null) {
            throw new ValidationException(entityType, "id", "ID cannot be null");
        }
    }

    /**
     * Validate string is not null or empty
     */
    public void validateRequiredString(String value, String entityType, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(entityType, fieldName, fieldName + " cannot be null or empty");
        }
    }

    /**
     * Validate date is not null and not in future (for completed actions)
     */
    public void validatePastDate(LocalDateTime date, String entityType, String fieldName) {
        if (date == null) {
            throw new ValidationException(entityType, fieldName, fieldName + " cannot be null");
        }

        if (date.isAfter(LocalDateTime.now())) {
            throw new ValidationException(entityType, fieldName, fieldName + " cannot be in the future");
        }
    }

    // ==================== JOURNEY DATA VALIDATIONS ====================

    /**
     * Validate journey data entity
     */
    public void validateJourneyData(JourneyData journeyData) {
        if (journeyData == null) {
            throw new ValidationException("JourneyData", "journeyData", "Journey data cannot be null");
        }

        // Validate user
        validateUser(journeyData.getUser());

        // Validate vehicle (optional - can be null)
        if (journeyData.getVehicle() != null) {
            // Basic vehicle validation if needed
            if (journeyData.getVehicle().getId() == null) {
                throw new ValidationException("JourneyData", "vehicle", "Vehicle ID cannot be null");
            }
        }

        // Validate distance
        if (journeyData.getDistanceKm() == null || journeyData.getDistanceKm().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("JourneyData", "distanceKm", "Distance must be positive");
        }

        // Validate energy consumption
        if (journeyData.getEnergyConsumedKwh() == null
                || journeyData.getEnergyConsumedKwh().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("JourneyData", "energyConsumedKwh", "Energy consumption must be positive");
        }

        // Validate CO2 reduction (if set)
        if (journeyData.getCo2ReducedKg() != null && journeyData.getCo2ReducedKg().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("JourneyData", "co2ReducedKg", "CO2 reduction must be positive");
        }

        // Validate journey times
        validateJourneyTimes(journeyData);

        // Validate reasonable limits
        validateJourneyLimits(journeyData);
    }

    /**
     * Validate journey times
     */
    private void validateJourneyTimes(JourneyData journeyData) {
        if (journeyData.getStartTime() != null && journeyData.getEndTime() != null) {
            if (journeyData.getStartTime().isAfter(journeyData.getEndTime())) {
                throw new ValidationException("JourneyData", "startTime/endTime",
                        "Start time cannot be after end time");
            }

            // Journey shouldn't be longer than 24 hours
            if (journeyData.getStartTime().plusDays(1).isBefore(journeyData.getEndTime())) {
                throw new ValidationException("JourneyData", "duration",
                        "Journey duration cannot exceed 24 hours");
            }

            // Journey times shouldn't be in the future
            LocalDateTime now = LocalDateTime.now();
            if (journeyData.getStartTime().isAfter(now)) {
                throw new ValidationException("JourneyData", "startTime",
                        "Journey start time cannot be in the future");
            }

            if (journeyData.getEndTime().isAfter(now)) {
                throw new ValidationException("JourneyData", "endTime",
                        "Journey end time cannot be in the future");
            }
        }

        // Validate creation date is not in the future (this is auto-generated but just
        // in case)
        if (journeyData.getCreatedAt() != null && journeyData.getCreatedAt().isAfter(LocalDateTime.now())) {
            throw new ValidationException("JourneyData", "createdAt",
                    "Creation date cannot be in the future");
        }
    }

    /**
     * Validate journey limits
     */
    private void validateJourneyLimits(JourneyData journeyData) {
        // Validate reasonable distance limits (max 2000km for single journey)
        if (journeyData.getDistanceKm().compareTo(new BigDecimal("2000")) > 0) {
            throw new ValidationException("JourneyData", "distanceKm",
                    "Distance exceeds reasonable limit (2000 km)");
        }

        // Minimum reasonable distance (0.1 km)
        if (journeyData.getDistanceKm().compareTo(new BigDecimal("0.1")) < 0) {
            throw new ValidationException("JourneyData", "distanceKm",
                    "Distance too small (minimum 0.1 km)");
        }

        // Validate reasonable energy consumption (max 500kWh for single journey)
        if (journeyData.getEnergyConsumedKwh().compareTo(new BigDecimal("500")) > 0) {
            throw new ValidationException("JourneyData", "energyConsumedKwh",
                    "Energy consumption exceeds reasonable limit (500 kWh)");
        }

        // Minimum reasonable energy consumption (0.01 kWh)
        if (journeyData.getEnergyConsumedKwh().compareTo(new BigDecimal("0.01")) < 0) {
            throw new ValidationException("JourneyData", "energyConsumedKwh",
                    "Energy consumption too small (minimum 0.01 kWh)");
        }

        // Validate energy efficiency (should be reasonable kWh per km)
        BigDecimal efficiency = journeyData.getEnergyConsumedKwh()
                .divide(journeyData.getDistanceKm(), 4, RoundingMode.HALF_UP);

        // Most EVs consume between 0.1 - 0.4 kWh/km, allow some buffer
        if (efficiency.compareTo(new BigDecimal("1.0")) > 0) {
            throw new ValidationException("JourneyData", "efficiency",
                    "Energy efficiency seems unrealistic (>" + efficiency + " kWh/km, max allowed: 1.0)");
        }

        if (efficiency.compareTo(new BigDecimal("0.05")) < 0) {
            throw new ValidationException("JourneyData", "efficiency",
                    "Energy efficiency seems unrealistic (<" + efficiency + " kWh/km, min allowed: 0.05)");
        }

        // If CO2 reduction is set, validate it's reasonable compared to distance
        if (journeyData.getCo2ReducedKg() != null) {
            // Typical CO2 reduction is around 0.2-0.5 kg per km for EVs vs ICE cars
            BigDecimal co2PerKm = journeyData.getCo2ReducedKg()
                    .divide(journeyData.getDistanceKm(), 4, RoundingMode.HALF_UP);

            if (co2PerKm.compareTo(new BigDecimal("1.0")) > 0) {
                throw new ValidationException("JourneyData", "co2Efficiency",
                        "CO2 reduction per km seems unrealistic (>" + co2PerKm + " kg/km)");
            }

            if (co2PerKm.compareTo(new BigDecimal("0.05")) < 0) {
                throw new ValidationException("JourneyData", "co2Efficiency",
                        "CO2 reduction per km seems too low (<" + co2PerKm + " kg/km)");
            }
        }
    }

    /**
     * Validate journey data for carbon credit creation (additional validation)
     */
    public void validateJourneyDataForCreditCreation(JourneyData journeyData) {
        // First run standard validation
        validateJourneyData(journeyData);

        // Additional validations for credit creation
        if (journeyData.getCo2ReducedKg() == null) {
            throw new ValidationException("JourneyData", "co2ReducedKg",
                    "CO2 reduction must be calculated before creating carbon credit");
        }

        // Journey must have both start and end times for credit creation
        if (journeyData.getStartTime() == null || journeyData.getEndTime() == null) {
            throw new ValidationException("JourneyData", "journeyTimes",
                    "Both start and end times are required for carbon credit creation");
        }

        // Journey must be completed (end time must be set and in the past)
        if (journeyData.getEndTime().isAfter(LocalDateTime.now())) {
            throw new ValidationException("JourneyData", "endTime",
                    "Journey must be completed before creating carbon credit");
        }

        // Minimum distance for carbon credit (e.g., 1 km)
        if (journeyData.getDistanceKm().compareTo(new BigDecimal("1.0")) < 0) {
            throw new ValidationException("JourneyData", "distanceKm",
                    "Minimum 1 km journey required for carbon credit creation");
        }

        // Minimum CO2 reduction for credit (e.g., 0.1 kg)
        if (journeyData.getCo2ReducedKg().compareTo(new BigDecimal("0.1")) < 0) {
            throw new ValidationException("JourneyData", "co2ReducedKg",
                    "Minimum 0.1 kg CO2 reduction required for carbon credit");
        }
    }
}