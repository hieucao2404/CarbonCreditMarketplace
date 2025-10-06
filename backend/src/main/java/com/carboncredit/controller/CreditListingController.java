package com.carboncredit.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.carboncredit.dto.CreditListingDTO;
import com.carboncredit.entity.CreditListing;
import com.carboncredit.entity.User;
import com.carboncredit.service.CreditListingService;
import com.carboncredit.service.CreditListingService.MarketplaceStats;
import com.carboncredit.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@Slf4j
@RestController
@RequestMapping("/credit-listings")
@RequiredArgsConstructor
public class CreditListingController {

    private final CreditListingService creditListingService;
    private final UserService userService;

    // create fixed-price listing
    @PostMapping("/create")
    public ResponseEntity<CreditListingDTO> createListing(@RequestParam UUID creditId, @RequestParam BigDecimal price,
            Authentication authentication) {
        try {
            User owner = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            CreditListing listing = creditListingService.createFixedPriceListing(creditId, owner, price);

            log.info("Listing created successfully by user: {}", owner.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(new CreditListingDTO(listing)); // FIX: Convert to DTO

        } catch (Exception e) {
            log.error("Error creating listing: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // Get all active listings (marketplace)
    @GetMapping
    public ResponseEntity<Page<CreditListingDTO>> getActiveListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "newest") String sortBy) {
        Page<CreditListing> listings = creditListingService.getActiveListings(page, size, sortBy);
        Page<CreditListingDTO> dtos = listings.map(CreditListingDTO::new);
        return ResponseEntity.ok(dtos);
    }

    // search by price range
    @GetMapping("/search")
    public ResponseEntity<Page<CreditListingDTO>> searchByPriceRange(@RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<CreditListing> listings = creditListingService.searchByPriceRange(minPrice, maxPrice, page, size);
            Page<CreditListingDTO> dtos = listings.map(CreditListingDTO::new); // Convert to DTO
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            log.error("Error searching listings by price range: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // get my lisitngs
    @GetMapping("/my-listings")
    public ResponseEntity<Page<CreditListingDTO>> getMyListings(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, Authentication authentication) {

        try {
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Page<CreditListing> listings = creditListingService.getUserListings(user, page, size);
            Page<CreditListingDTO> dtos = listings.map(CreditListingDTO::new); // Convert to DTO

            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            log.error("Error retrieving user listings: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // get my active listings only
    @GetMapping("/my-active-listings")
    public ResponseEntity<Page<CreditListingDTO>> getMyActiveListings(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Page<CreditListing> listings = creditListingService.getUserActiveListings(user, page, size);
            Page<CreditListingDTO> dtos = listings.map(CreditListingDTO::new); // Convert to DTO
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            log.error("Error retrieving user active listings: {}", e.getMessage()); // Fix typo
            return ResponseEntity.badRequest().build();
        }
    }

    // Purchase listing
    @PostMapping("/{listingId}/purchase") // Fix: was {listngId}
    public ResponseEntity<CreditListingDTO> purchaseListing(@PathVariable UUID listingId, Authentication authentication) { // Fix: was purchaseLisitng
        try {
            User buyer = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            CreditListing listing = creditListingService.purchaseListing(listingId, buyer);

            log.info("Listing {} purchased by user: {}", listingId, buyer.getUsername());
            return ResponseEntity.ok(new CreditListingDTO(listing)); // Convert to DTO
        } catch (Exception e) {
            log.error("Error purchasing listing {}: {}", listingId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // update lisitng price
    @PutMapping("/{listingId}/price")
    public ResponseEntity<CreditListingDTO> updatePrice(@PathVariable UUID listingId, @RequestParam BigDecimal newPrice,
            Authentication authentication) {
        try {
            User owner = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            CreditListing listing = creditListingService.updateListingPrice(listingId, owner, newPrice);
            log.info("Listing {} price updated by user: {}", listingId, owner.getUsername()); // Fix: was udpated
            return ResponseEntity.ok(new CreditListingDTO(listing)); // Convert to DTO
        } catch (Exception e) {
            log.error("Error updating listing price {}: {}", listingId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // cancel listing
    @DeleteMapping("/{listingId}")
    public ResponseEntity<CreditListingDTO> cancelListing(
            @PathVariable UUID listingId, Authentication authentication) {
        try {
            User owner = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found")); // Fix: was "user not found"
            CreditListing listing = creditListingService.cancelListing(listingId, owner);

            log.info("Listing {} cancelled by user: {}", listingId, owner.getUsername());
            return ResponseEntity.ok(new CreditListingDTO(listing)); // Convert to DTO
        } catch (Exception e) {
            log.error("Error cancelling listing {}: {}", listingId, e.getMessage()); // Fix: was lisitng
            return ResponseEntity.badRequest().build();
        }

    }

    // Get marketplace statistics
    @GetMapping("/stats")
    public ResponseEntity<MarketplaceStats> getMarketplaceStats() {
        try {
            MarketplaceStats stats = creditListingService.getMarketplaceStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error retrieving marketplace stats: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
