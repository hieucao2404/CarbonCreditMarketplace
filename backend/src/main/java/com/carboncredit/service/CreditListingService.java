package com.carboncredit.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carboncredit.dto.CreditListingDTO;
import com.carboncredit.dto.MarketplaceStatsDTO;
import com.carboncredit.entity.CarbonCredit;
import com.carboncredit.entity.CarbonCredit.CreditStatus;
import com.carboncredit.entity.CreditListing;
import com.carboncredit.entity.CreditListing.ListingStatus;
import com.carboncredit.entity.CreditListing.ListingType;
import com.carboncredit.entity.User;
import com.carboncredit.exception.BusinessOperationException;
import com.carboncredit.repository.CarbonCreditRepository;
import com.carboncredit.repository.CreditListingRepository;
import com.carboncredit.util.DTOMapper;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CreditListingService {

    private final CreditListingRepository creditListingRepository;
    private final CarbonCreditRepository carbonCreditRepository;
    private final ValidationService validationService; // Keep this

    // ==================== LISTING CREATION ====================

    /**
     * Creates a fixed-price listing and returns its DTO
     */
    public CreditListingDTO createFixedPriceListing(UUID creditId, User owner, BigDecimal price) {
        log.info("Creating fixed-price listing for credit {} by user {} at price {}",
                creditId, owner.getUsername(), price);

        CarbonCredit credit = carbonCreditRepository.findById(creditId)
                .orElseThrow(() -> new EntityNotFoundException("Carbon Credit not found: " + creditId));

        validationService.validateListingCreation(credit, owner, price);

        Optional<CreditListing> existingListing = creditListingRepository.findByCredit(credit);
        if (existingListing.isPresent()) {
            CreditListing existing = existingListing.get();
            ListingStatus status = existing.getStatus();

            // Prevent duplicate if status is ACTIVE or PENDING_APPROVAL
            if (status == ListingStatus.ACTIVE || status == ListingStatus.PENDING_APPROVAL) {
                throw new BusinessOperationException(
                        String.format("Credit already has a %s listing. Cannot create duplicate.",
                                status.name().toLowerCase().replace("_", " ")));
            }
        }

        // Create new listing with PENDING_APPROVAL status
        CreditListing listing = CreditListing.builder()
                .credit(credit)
                .listingType(ListingType.FIXED)
                .price(price)
                .status(ListingStatus.PENDING_APPROVAL) // Not ACTIVE yet
                .build();

        // Don't update credit status until CVA approves
        // credit.setStatus(CreditStatus.LISTED); // ❌ Don't do this yet

        CreditListing savedListing = creditListingRepository.save(listing);
        log.info("Fixed-price listing created with PENDING_APPROVAL status: {}", savedListing.getId());

        return DTOMapper.toCreditListingDTO(savedListing);
    }

    public CreditListingDTO approveListing(UUID listingId, User cva) {
        CreditListing listing = findListingEntityById(listingId);

        if (listing.getStatus() != ListingStatus.PENDING_APPROVAL) {
            throw new BusinessOperationException("Only pending listings can be approved");
        }

        listing.setStatus(ListingStatus.ACTIVE);
        listing.setApprovedBy(cva);
        listing.setApprovedAt(LocalDateTime.now());

        // NOW update credit status
        CarbonCredit credit = listing.getCredit();
        credit.setStatus(CreditStatus.LISTED);
        credit.setListedAt(LocalDateTime.now());
        carbonCreditRepository.save(credit);

        return DTOMapper.toCreditListingDTO(creditListingRepository.save(listing));
    }

    public CreditListingDTO rejectListing(UUID listingId, User cva, String reason) {
        CreditListing listing = findListingEntityById(listingId);

        if (listing.getStatus() != ListingStatus.PENDING_APPROVAL) {
            throw new BusinessOperationException("Only pending listings can be rejected");
        }

        listing.setStatus(ListingStatus.REJECTED);
        listing.setRejectionReason(reason);

        return DTOMapper.toCreditListingDTO(creditListingRepository.save(listing));
    }

    /**
     * Get all pending listings for CVA review
     */
    @Transactional(readOnly = true)
    public Page<CreditListingDTO> getPendingListings(int page, int size) {
        validationService.validatePageParameters(page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        Page<CreditListing> entityPage = creditListingRepository.findByStatus(
                ListingStatus.PENDING_APPROVAL, pageable);
        return entityPage.map(DTOMapper::toCreditListingDTO);
    }

    /**
     * Ipdate listing price and return the updated DTO
     */
    public CreditListingDTO updateListingPrice(UUID listingId, User owner, BigDecimal newPrice) {
        log.info("Updating listing {} price to {} by user {}", listingId, newPrice);

        CreditListing listing = findListingEntityById(listingId);

        validationService.validateOwnership(listing, owner);
        validationService.validatePrice(newPrice);

        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new BusinessOperationException("Cannot update price fo inactive listings");
        }
        if (listing.getListingType() != ListingType.FIXED) {
            throw new BusinessOperationException("Price can only update for Fixed listings");
        }

        BigDecimal oldPrice = listing.getPrice();
        listing.setPrice(newPrice);

        CreditListing updated = creditListingRepository.save(listing);

        log.info("Listing {} price updated from {} to {}", listingId, oldPrice, newPrice);

        // map to DTO
        return DTOMapper.toCreditListingDTO(updated);
    }

    /**
     * Cancels a listing and returns its DTO
     */
    public CreditListingDTO cancelListing(UUID listingId, User owner) {
        log.info("Cancelling listing {} by user {}", listingId, owner.getUsername());

        CreditListing listing = findListingEntityById(listingId); // Use internal helper

        validationService.validateOwnership(listing, owner);

        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new BusinessOperationException("Can only cancel active listings");
        }

        listing.setStatus(ListingStatus.CANCELLED);

        CarbonCredit credit = listing.getCredit();
        credit.setStatus(CreditStatus.VERIFIED); // Revert status
        credit.setListedAt(null);

        CreditListing cancelled = creditListingRepository.save(listing);
        carbonCreditRepository.save(credit);

        log.info("Listing {} cancelled", listingId);

        // Map to DTO
        return DTOMapper.toCreditListingDTO(cancelled);
    }

    // ==================== MARKETPLACE BROWSING ========================
    /**
     * Gets active listing as a DTO Page
     */
    @Transactional(readOnly = true)
    public Page<CreditListingDTO> getActiveListings(int page, int size, String sortBy) {
        validationService.validatePageParameters(page, size);
        Sort sort = createSort(sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<CreditListing> entityPage = creditListingRepository.findByStatus(ListingStatus.ACTIVE, pageable);

        // Map Page<Entity> -> Page<DTO>
        return entityPage.map(DTOMapper::toCreditListingDTO);
    }

    @Transactional(readOnly = true)
    public Page<CreditListingDTO> getListingsByUser(UUID userId, int page, int size) {
        validationService.validatePageParameters(page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Change this line - remove status filter
        Page<CreditListing> entityPage = creditListingRepository
                .findAllByUserId(userId, pageable); // Returns ALL statuses

        return entityPage.map(DTOMapper::toCreditListingDTO);
    }

    /**
     * Searched active listings by price range and returns a DTO page
     */

    @Transactional(readOnly = true)
    public Page<CreditListingDTO> searchByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, int page, int size) {
        validationService.validatePageParameters(page, size);
        validationService.validatePriceRange(minPrice, maxPrice);

        Pageable pageable = PageRequest.of(page, size, Sort.by("price").ascending());
        Page<CreditListing> entityPage = creditListingRepository.findByStatusAndPriceBetween(
                ListingStatus.ACTIVE, minPrice, maxPrice, pageable);

        // Map Page<Entity> -> Page<DTO>
        return entityPage.map(DTOMapper::toCreditListingDTO);
    }

    /**
     * Gets a user's listings (all statuses) as a DTO Page.
     */
    @Transactional(readOnly = true)
    public Page<CreditListingDTO> getUserListings(User user, int page, int size) {
        validationService.validateUser(user);
        validationService.validatePageParameters(page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<CreditListing> entityPage = creditListingRepository.findByUser(user, pageable);

        // Map Page<Entity> -> Page<DTO>
        return entityPage.map(DTOMapper::toCreditListingDTO);
    }

    /**
     * Gets a user's ACTIVE listings as a DTO Page.
     */
    @Transactional(readOnly = true)
    public Page<CreditListingDTO> getUserActiveListings(User user, int page, int size) {
        validationService.validateUser(user);
        validationService.validatePageParameters(page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<CreditListing> entityPage = creditListingRepository.findByUserAndStatus(user, ListingStatus.ACTIVE,
                pageable);

        // Map Page<Entity> -> Page<DTO>
        return entityPage.map(DTOMapper::toCreditListingDTO);
    }

    // ================== PURCHASE OPERATIONS ===========================
    /**
     * Processes the purchase (update staus ) and returns the DTO
     * this method does not handle wallet transfer
     * belong to transaction service
     */
    // public CreditListingDTO processPurchaseStatusUpdate(UUID listingId, User
    // buyer){
    // log.info("Processing purchase status update for listing {} by buyer {}",
    // listingId, buyer.getUsername());

    // CreditListing listing = findListingEntityById(listingId); // Use internal
    // helper

    // validationService.validatePurchase(listing, buyer); // Checks buyer !=
    // seller, etc.

    // if (listing.getStatus() != ListingStatus.ACTIVE) {
    // throw new BusinessOperationException("Listing is no longer available for
    // purchase. Status: " + listing.getStatus());
    // }

    // // --- Update Statuses ---
    // listing.setStatus(ListingStatus.CLOSED); // Use CLOSED instead of SOLD for
    // clarity
    // CarbonCredit credit = listing.getCredit();
    // credit.setStatus(CreditStatus.SOLD); // Credit is now SOLD

    // // --- Save Updates ---
    // CreditListing updatedListing = creditListingRepository.save(listing);
    // carbonCreditRepository.save(credit);

    // log.info("Purchase status updated: Listing {} marked CLOSED, Credit {} marked
    // SOLD",
    // listing.getId(), credit.getId());

    // // Map to DTO
    // return DTOMapper.toCreditListingDTO(updatedListing);
    // }

    // ==================== STATISTICS ====================

    /**
     * Gets marketplace statistics and returns a DTO.
     */
    @Transactional(readOnly = true)
    public MarketplaceStatsDTO getMarketplaceStats() {
        long totalActiveListings = creditListingRepository.countActiveListings();
        BigDecimal averagePrice = creditListingRepository.getAverageFixedPrice();

        // Use the DTO constructor
        return new MarketplaceStatsDTO(new MarketplaceStats(
                totalActiveListings,
                averagePrice));
    }

    // ==================== HELPER METHODS ====================

    /**
     * Internal helper to find the ENTITY, ensuring it exists.
     */
    private CreditListing findListingEntityById(UUID listingId) {
        validationService.validateId(listingId, "CreditListing");
        return creditListingRepository.findById(listingId)
                .orElseThrow(() -> new EntityNotFoundException("Credit listing not found: " + listingId));
    }

    /**
     * Finds a single listing by ID and returns its DTO.
     * (Needed for the Controller's GET /{id} endpoint)
     */
    @Transactional(readOnly = true)
    public Optional<CreditListingDTO> findListingDtoById(UUID listingId) {
        return creditListingRepository.findById(listingId)
                .map(DTOMapper::toCreditListingDTO);
    }

    private Sort createSort(String sortBy) {
        // Using simplified sorting based on common needs
        return switch (sortBy != null ? sortBy.toLowerCase() : "newest") {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            // Add other sorts if needed, e.g., by credit amount or CO2
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "createdAt"); // Default: newest
        };
    }

    // ==================== STATISTICS CLASS ====================
    // Keep this inner class for getMarketplaceStats()
    public static class MarketplaceStats {
        private final long totalActiveListings;
        private final BigDecimal averagePrice;

        public MarketplaceStats(long totalActiveListings, BigDecimal averagePrice) {
            this.totalActiveListings = totalActiveListings;
            this.averagePrice = averagePrice;
        }

        public long getTotalActiveListings() {
            return totalActiveListings;
        }

        public BigDecimal getAveragePrice() {
            return averagePrice;
        }
    }
}
