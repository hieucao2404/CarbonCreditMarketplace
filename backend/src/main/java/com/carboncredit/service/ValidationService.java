package com.carboncredit.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.carboncredit.entity.CarbonCredit;
import com.carboncredit.entity.CarbonCredit.CreditStatus;
import com.carboncredit.entity.CreditListing;
import com.carboncredit.entity.CreditListing.ListingType;
import com.carboncredit.entity.JourneyData;
import com.carboncredit.entity.User;
import com.carboncredit.exception.BusinessOperationException;
import com.carboncredit.exception.UnauthorizedOperationException;
import com.carboncredit.exception.ValidationException;

@Service
public class ValidationService {

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