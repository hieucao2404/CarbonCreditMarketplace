package com.carboncredit.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.carboncredit.dto.ApiResponse;
import com.carboncredit.dto.CreditListingDTO;
import com.carboncredit.dto.CreditListingRequest;
import com.carboncredit.dto.MarketplaceStatsDTO;
import com.carboncredit.dto.UpdateListingPriceRequest;
import com.carboncredit.entity.CreditListing;
import com.carboncredit.entity.User;
import com.carboncredit.exception.BusinessOperationException;
import com.carboncredit.exception.EntityNotFoundException;
import com.carboncredit.exception.ResourceNotFoundException;
import com.carboncredit.service.CreditListingService;
import com.carboncredit.service.CreditListingService.MarketplaceStats;
import com.carboncredit.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Slf4j
@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class CreditListingController {

    private final CreditListingService listingService;
    private final UserService userService;

    /**
     * Helper authentication method
     */
    private User getCurrentUser(Authentication authentication) {
        return userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", authentication.getName()));
    }

    // ================================
    // EV_OWNER ENDPOINTS(Managing Listings)
    // ==============================

    /**
     * (EV_OWNER) Get ALL listings for the current user (ACTIVE, PENDING, REJECTED,
     * etc.)
     * This is for the user to manage their own listings
     */
    @GetMapping("/my-listings")
    @PreAuthorize("hasRole('EV_OWNER')")
    public ResponseEntity<ApiResponse<Page<CreditListingDTO>>> getMyListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        try {
            User owner = getCurrentUser(authentication);

            // Call the service method that returns ALL statuses
            Page<CreditListingDTO> dtoPage = listingService.getListingsByUser(owner.getId(), page, size);

            return ResponseEntity.ok(ApiResponse.success(dtoPage));

        } catch (IllegalArgumentException e) {
            log.warn("Bad request for my listings: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error getting my listings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred"));
        }
    }

    /**
     * (EV_OWNER) create a new fixed-price listing for a verified credit
     */
    @PostMapping
    @PreAuthorize("hasRole('EV_OWNER')")
    public ResponseEntity<ApiResponse<CreditListingDTO>> createFixedPriceListing(
            @Valid @RequestBody CreditListingRequest request, Authentication authentication) {
        log.info("Request to create listing for credit: {}", request.getCreditId());

        try {
            User owner = getCurrentUser(authentication);
            // Service now return DTO
            CreditListingDTO listingDTO = listingService.createFixedPriceListing(request.getCreditId(), owner,
                    request.getPrice());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Credit listed successfully", listingDTO));
        } catch (EntityNotFoundException | ResourceNotFoundException e) { // Catch ResourceNotFound too
            log.warn("Failed to create listing - resource not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (BusinessOperationException | IllegalStateException | SecurityException | IllegalArgumentException e) {
            log.warn("Failed to create listing - bad request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error creating listing", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred"));
        }
    }

    /**
     * (EV_OWNER) Cancel an active listing
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('EV_OWNER')")
    public ResponseEntity<ApiResponse<CreditListingDTO>> cancelListing(
            @PathVariable UUID id,
            Authentication authentication) {

        log.info("Request to cancel listing: {}", id);
        try {
            User owner = getCurrentUser(authentication);
            // Service returns DTO
            CreditListingDTO listingDTO = listingService.cancelListing(id, owner);

            return ResponseEntity.ok(
                    ApiResponse.success("Listing cancelled successfully", listingDTO));

        } catch (EntityNotFoundException | ResourceNotFoundException e) {
            log.warn("Failed to cancel listing - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (BusinessOperationException | SecurityException | IllegalArgumentException e) {
            log.warn("Failed to cancel listing - bad request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error cancelling listing", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred"));
        }
    }

    /**
     * (EV_OWNER) Update the price of an active fixed-price listing.
     */
    @PutMapping("/{id}/price")
    @PreAuthorize("hasRole('EV_OWNER')")
    public ResponseEntity<ApiResponse<CreditListingDTO>> updateListingPrice(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateListingPriceRequest request,
            Authentication authentication) {

        log.info("Request to update price for listing: {}", id);
        try {
            User owner = getCurrentUser(authentication);
            // Service returns DTO
            CreditListingDTO listingDTO = listingService.updateListingPrice(id, owner, request.getNewPrice());

            return ResponseEntity.ok(
                    ApiResponse.success("Price updated successfully", listingDTO));

        } catch (EntityNotFoundException | ResourceNotFoundException e) {
            log.warn("Failed to update price - not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (BusinessOperationException | SecurityException | IllegalArgumentException e) {
            log.warn("Failed to update price - bad request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error updating price", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred"));
        }
    }

    // ==================================================
    // PUBLIC / BUYER ENDPOINTS (Browsing Marketplace)
    // ==================================================

    /**
     * (PUBLIC) Get all active listings, paginated and sorted.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CreditListingDTO>>> getActiveListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "newest") String sortBy) {

        try {
            // Service returns DTO Page
            Page<CreditListingDTO> dtoPage = listingService.getActiveListings(page, size, sortBy);
            return ResponseEntity.ok(ApiResponse.success(dtoPage));
        } catch (IllegalArgumentException e) {
            log.warn("Bad request for active listings: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error getting active listings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred"));
        }
    }

    /**
     * (PUBLIC) Get a single listing by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CreditListingDTO>> getListingById(@PathVariable UUID id) {
        log.info("Fetching listing by ID: {}", id);
        try {
            // Use the service method that returns Optional<DTO>
            CreditListingDTO listingDTO = listingService.findListingDtoById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("CreditListing", "id", id));

            // Optional: Add check if listing status is ACTIVE if only active should be
            // public
            if (!"ACTIVE".equals(listingDTO.getStatus())) {
                throw new ResourceNotFoundException("Listing", "id", id); // Or Forbidden
            }

            return ResponseEntity.ok(ApiResponse.success(listingDTO));

        } catch (ResourceNotFoundException e) {
            log.warn("Listing not found with ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching listing {}: ", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred: " + e.getMessage()));
        }
    }

    /**
     * (PUBLIC) Search active listings by price range.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<CreditListingDTO>>> searchByPriceRange(
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            // Service returns DTO Page
            Page<CreditListingDTO> dtoPage = listingService.searchByPriceRange(minPrice, maxPrice, page, size);
            return ResponseEntity.ok(ApiResponse.success(dtoPage));
        } catch (IllegalArgumentException e) {
            log.warn("Bad request for search listings: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error searching listings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred"));
        }
    }

    /**
     * (PUBLIC/BUYER) Get all active listings for a specific user.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<Page<CreditListingDTO>>> getUserActiveListings(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            User user = userService.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

            // Service returns DTO Page
            Page<CreditListingDTO> dtoPage = listingService.getUserActiveListings(user, page, size);
            return ResponseEntity.ok(ApiResponse.success(dtoPage));

        } catch (ResourceNotFoundException e) {
            log.warn("Cannot get user listings - user not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Bad request for user listings: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error getting user listings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred"));
        }
    }

    /**
     * (PUBLIC) Get marketplace statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<MarketplaceStatsDTO>> getMarketplaceStats() {
        try {
            // Service returns DTO
            MarketplaceStatsDTO statsDTO = listingService.getMarketplaceStats();
            return ResponseEntity.ok(ApiResponse.success(statsDTO));
        } catch (Exception e) {
            log.error("Unexpected error getting marketplace stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred"));
        }
    }

}
