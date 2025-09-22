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

import com.carboncredit.entity.CarbonCredit;
import com.carboncredit.entity.CarbonCredit.CreditStatus;
import com.carboncredit.entity.CreditListing;
import com.carboncredit.entity.CreditListing.ListingStatus;
import com.carboncredit.entity.CreditListing.ListingType;
import com.carboncredit.entity.User;
import com.carboncredit.exception.BusinessOperationException;
import com.carboncredit.repository.CarbonCreditRepository;
import com.carboncredit.repository.CreditListingRepository;

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

    public CreditListing createFixedPriceListing(UUID creditId, User owner, BigDecimal price) {
        log.info("Creating fixed-price listing for credit {} by user {} at price {}", 
            creditId, owner.getUsername(), price);

        // Find and validate the carbon credit
        CarbonCredit credit = carbonCreditRepository.findById(creditId)
            .orElseThrow(() -> new EntityNotFoundException("Carbon credit not found: " + creditId));

        // Use ValidationService
        validationService.validateListingCreation(credit, owner, price);

        // Check if credit is already listed
        Optional<CreditListing> existingListing = creditListingRepository.findByCredit(credit);
        if (existingListing.isPresent() && existingListing.get().getStatus() == ListingStatus.ACTIVE) {
            throw new BusinessOperationException("Credit is already actively listed in marketplace");
        }

        // Create new fixed-price listing
        CreditListing listing = new CreditListing();
        listing.setCredit(credit);
        listing.setListingType(ListingType.FIXED);
        listing.setPrice(price);
        listing.setStatus(ListingStatus.ACTIVE);

        // Update carbon credit status to LISTED
        credit.setStatus(CreditStatus.LISTED);
        credit.setListedAt(LocalDateTime.now());

        // Save both entities
        CreditListing savedListing = creditListingRepository.save(listing);
        carbonCreditRepository.save(credit);

        log.info("Fixed-price listing created successfully: {} for credit {} at price {}", 
            savedListing.getId(), creditId, price);

        return savedListing;
    }

    // ==================== LISTING MANAGEMENT ====================

    public CreditListing updateListingPrice(UUID listingId, User owner, BigDecimal newPrice) {
        log.info("Updating listing {} price to {} by user {}", listingId, newPrice, owner.getUsername());

        CreditListing listing = findListingById(listingId);

        // Use ValidationService
        validationService.validateOwnership(listing, owner);
        validationService.validatePrice(newPrice);

        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new BusinessOperationException("Cannot update price for inactive listings");
        }

        BigDecimal oldPrice = listing.getPrice();
        listing.setPrice(newPrice);

        CreditListing updated = creditListingRepository.save(listing);

        log.info("Listing {} price updated from {} to {} by user {}", 
            listingId, oldPrice, newPrice, owner.getUsername());

        return updated;
    }

    public CreditListing cancelListing(UUID listingId, User owner) {
        log.info("Cancelling listing {} by user {}", listingId, owner.getUsername());

        CreditListing listing = findListingById(listingId);

        // Use ValidationService
        validationService.validateOwnership(listing, owner);

        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new BusinessOperationException("Can only cancel active listings");
        }

        // Update listing status
        listing.setStatus(ListingStatus.CANCELLED);

        // Revert credit status back to VERIFIED
        CarbonCredit credit = listing.getCredit();
        credit.setStatus(CreditStatus.VERIFIED);
        credit.setListedAt(null);

        // Save updates
        CreditListing cancelled = creditListingRepository.save(listing);
        carbonCreditRepository.save(credit);

        log.info("Listing {} cancelled by user {}", listingId, owner.getUsername());

        return cancelled;
    }

    // ==================== MARKETPLACE BROWSING ====================

    @Transactional(readOnly = true)
    public Page<CreditListing> getActiveListings(int page, int size, String sortBy) {
        validationService.validatePageParameters(page, size);
        
        Sort sort = createSort(sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        return creditListingRepository.findByStatus(ListingStatus.ACTIVE, pageable);
    }

    @Transactional(readOnly = true)
    public Page<CreditListing> searchByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, int page, int size) {
        validationService.validatePageParameters(page, size);
        validationService.validatePriceRange(minPrice, maxPrice);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("price").ascending());
        return creditListingRepository.findByStatusAndPriceBetween(
            ListingStatus.ACTIVE, minPrice, maxPrice, pageable);
    }

    @Transactional(readOnly = true)
    public Page<CreditListing> getUserListings(User user, int page, int size) {
        validationService.validateUser(user);
        validationService.validatePageParameters(page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return creditListingRepository.findByUser(user, pageable);
    }

    @Transactional(readOnly = true)
    public Page<CreditListing> getUserActiveListings(User user, int page, int size) {
        validationService.validateUser(user);
        validationService.validatePageParameters(page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return creditListingRepository.findByUserAndStatus(user, ListingStatus.ACTIVE, pageable);
    }

    // ==================== PURCHASE OPERATIONS ====================

    public CreditListing purchaseListing(UUID listingId, User buyer) {
        log.info("Processing purchase of listing {} by user {}", listingId, buyer.getUsername());

        CreditListing listing = findListingById(listingId);

        // Use ValidationService
        validationService.validatePurchase(listing, buyer);

        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new BusinessOperationException("Listing is no longer available for purchase");
        }

        // Complete the purchase
        listing.setStatus(ListingStatus.CLOSED);

        // Update carbon credit status
        CarbonCredit credit = listing.getCredit();
        credit.setStatus(CreditStatus.SOLD);

        // Save updates
        CreditListing sold = creditListingRepository.save(listing);
        carbonCreditRepository.save(credit);

        log.info("Purchase completed: Listing {} sold to {} for price {}", 
            listing.getId(), buyer.getUsername(), listing.getPrice());

        return sold;
    }

    // ==================== STATISTICS ====================

    @Transactional(readOnly = true)
    public MarketplaceStats getMarketplaceStats() {
        long totalActiveListings = creditListingRepository.countActiveListings();
        BigDecimal averagePrice = creditListingRepository.getAverageFixedPrice();

        return new MarketplaceStats(
            totalActiveListings,
            averagePrice != null ? averagePrice : BigDecimal.ZERO
        );
    }

    // ==================== HELPER METHODS ====================

    private CreditListing findListingById(UUID listingId) {
        validationService.validateId(listingId, "CreditListing");
        
        return creditListingRepository.findById(listingId)
            .orElseThrow(() -> new EntityNotFoundException("Credit listing not found: " + listingId));
    }

    private Sort createSort(String sortBy) {
        return switch (sortBy != null ? sortBy.toLowerCase() : "newest") {
            case "price_asc" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            case "co2_asc" -> Sort.by("credit.co2ReducedKg").ascending();
            case "co2_desc" -> Sort.by("credit.co2ReducedKg").descending();
            case "oldest" -> Sort.by("createdAt").ascending();
            default -> Sort.by("createdAt").descending();
        };
    }

    // ==================== STATISTICS CLASS ====================

    public static class MarketplaceStats {
        private final long totalActiveListings;
        private final BigDecimal averagePrice;

        public MarketplaceStats(long totalActiveListings, BigDecimal averagePrice) {
            this.totalActiveListings = totalActiveListings;
            this.averagePrice = averagePrice;
        }

        public long getTotalActiveListings() { return totalActiveListings; }
        public BigDecimal getAveragePrice() { return averagePrice; }

        @Override
        public String toString() {
            return String.format("MarketplaceStats{listings=%d, avgPrice=%s}", 
                totalActiveListings, averagePrice);
        }
    }
}
