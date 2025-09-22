package com.carboncredit.repository;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.carboncredit.entity.CarbonCredit;
import com.carboncredit.entity.CreditListing;
import com.carboncredit.entity.CreditListing.ListingStatus;
import com.carboncredit.entity.CreditListing.ListingType;
import com.carboncredit.entity.User;

@Repository
public interface CreditListingRepository extends JpaRepository<CreditListing, UUID> {
        // basic crud inherited from Jpa Repo

        // ============ FIND BY STATUS ===========
        // find all active listings
        List<CreditListing> findByStatus(ListingStatus status);

        // fund active lisitng with pagination
        Page<CreditListing> findByStatus(ListingStatus status, Pageable pageable);

        // find active listings by type(Fixed or auction)
        List<CreditListing> findByStatusAndListingType(ListingStatus status, ListingType listingType);

        Page<CreditListing> findByStatusAndListingType(ListingStatus status, ListingType listingType,
                        Pageable pageable);

        // ============== FIND BY CREDIT ==============

        // find bye listing by carbon credit
        Optional<CreditListing> findByCredit(CarbonCredit credit);

        // find listing by credit id
        @Query("SELECT cl FROM CreditListing cl WHERE cl.credit.id = :creditId")
        Optional<CreditListing> findByCreditId(@Param("creditId") UUID creditId);

        // check if credit is alreayd listed
        boolean existsByCredit(CarbonCredit credit);

        @Query("SELECT COUNT(cl) > 0 FROM CreditListing cl WHERE cl.credit.id = :creditId")
        boolean existsByCreditId(@Param("creditId") UUID creditId);

        // ============ FIND BY USER ===============

        // FIND ALL LISTINS BY CREDIT OWNER
        @Query("SELECT cl FROM CreditListing cl WHERE cl.credit.user = :user")
        List<CreditListing> findByUser(@Param("user") User user);

        @Query("SELECT cl FROM CreditListing cl WHERE cl.credit.user = :user")
        Page<CreditListing> findByUser(@Param("user") User user, Pageable pageable);

        // Find user;s listing by status
        @Query("SELECT cl FROM CreditListing cl WHERE cl.credit.user = :user AND cl.status = :status")
        List<CreditListing> findByUserAndStatus(@Param("user") User user, @Param("status") ListingStatus status);

        @Query("SELECT cl FROM CreditListing cl WHERE cl.credit.user = :user AND cl.status = :status")
        Page<CreditListing> findByUserAndStatus(@Param("user") User user, @Param("status") ListingStatus status,
                        Pageable pageable);

        // ============ PRICE FILTERING ============

        // Find active fixed-price listing within price range
        @Query("SELECT cl FROM CreditListing cl WHERE cl.status = :status AND cl.listingType = 'FIXED' " +
                        "AND cl.price BETWEEN :minPrice AND :maxPrice")
        List<CreditListing> findByStatusAndPriceBetween(
                        @Param("status") ListingStatus status,
                        @Param("minPrice") BigDecimal minPrice,
                        @Param("maxPrice") BigDecimal maxPrice);

        @Query("SELECT cl FROM CreditListing cl WHERE cl.status = :status AND cl.listingType = 'FIXED' " +
                        "AND cl.price BETWEEN :minPrice AND :maxPrice")
        Page<CreditListing> findByStatusAndPriceBetween(
                        @Param("status") ListingStatus status,
                        @Param("minPrice") BigDecimal minPrice,
                        @Param("maxPrice") BigDecimal maxPrice,
                        Pageable pageable);

        // Find active listings below maximum price
        @Query("SELECT cl FROM CreditListing cl WHERE cl.status = :status AND cl.listingType = 'FIXED' "
                        + "AND cl.price <= :maxPrice")
        List<CreditListing> findByStatusandPriceLessThanEqual(
                        @Param("status") ListingStatus status,
                        @Param("maxPrice") BigDecimal maxPrice);

        // ======== AUCTION OPERATIONS =======
        /**
         * Find active auctions
         */
        @Query("SELECT cl FROM CreditListing cl WHERE cl.status = 'ACTIVE' AND cl.listingType = 'AUCTION'")
        List<CreditListing> findActiveAuctions();

        /**
         * Find auctions ending soon (within specified time)
         */
        @Query("SELECT cl FROM CreditListing cl WHERE cl.status = 'ACTIVE' AND cl.listingType = 'AUCTION' " +
                        "AND cl.auctionEndTime <= :endTime")
        List<CreditListing> findAuctionsEndingBefore(@Param("endTime") LocalDateTime endTime);

        /**
         * Find expired auctions that need to be closed
         */
        @Query("SELECT cl FROM CreditListing cl WHERE cl.status = 'ACTIVE' AND cl.listingType = 'AUCTION' " +
                        "AND cl.auctionEndTime < :currentTime")
        List<CreditListing> findExpiredAuctions(@Param("currentTime") LocalDateTime currentTime);

        /**
         * Find auctions by minimum bid range
         */
        @Query("SELECT cl FROM CreditListing cl WHERE cl.status = 'ACTIVE' AND cl.listingType = 'AUCTION' " +
                        "AND cl.minBid BETWEEN :minBid AND :maxBid")
        List<CreditListing> findAuctionsByMinBidBetween(
                        @Param("minBid") BigDecimal minBid,
                        @Param("maxBid") BigDecimal maxBid);

        // ==================== ADVANCED SEARCH ====================

        /**
         * Search listings with multiple filters
         */
        @Query("SELECT cl FROM CreditListing cl WHERE " +
                        "(:status IS NULL OR cl.status = :status) AND " +
                        "(:listingType IS NULL OR cl.listingType = :listingType) AND " +
                        "(:minPrice IS NULL OR (cl.listingType = 'FIXED' AND cl.price >= :minPrice) OR (cl.listingType = 'AUCTION' AND cl.minBid >= :minPrice)) AND "
                        +
                        "(:maxPrice IS NULL OR (cl.listingType = 'FIXED' AND cl.price <= :maxPrice) OR (cl.listingType = 'AUCTION' AND cl.minBid <= :maxPrice)) AND "
                        +
                        "(:minCO2 IS NULL OR cl.credit.co2ReducedKg >= :minCO2) AND " +
                        "(:maxCO2 IS NULL OR cl.credit.co2ReducedKg <= :maxCO2)")
        Page<CreditListing> findWithFilters(
                        @Param("status") ListingStatus status,
                        @Param("listingType") ListingType listingType,
                        @Param("minPrice") BigDecimal minPrice,
                        @Param("maxPrice") BigDecimal maxPrice,
                        @Param("minCO2") BigDecimal minCO2,
                        @Param("maxCO2") BigDecimal maxCO2,
                        Pageable pageable);

        // ================ STATISTICS AND ANALYTICS

        // count active listings
        @Query("SELECT COUNT(cl) FROM CreditListing cl WHERE cl.status = 'ACTIVE'")
        long countActiveListings();

        // Count lisitng by type
        @Query("SELECT COUNT(cl) FROM CreditListing cl WHERE cl.status = 'ACTIVE' AND cl.listingType = :type")
        long countActiveListingsByType(@Param("type") ListingType type);

        /**
         * Get average price of active fixed listings
         */
        @Query("SELECT AVG(cl.price) FROM CreditListing cl WHERE cl.status = 'ACTIVE' AND cl.listingType = 'FIXED'")
        BigDecimal getAverageFixedPrice();

        /**
         * Get price statistics
         */
        @Query("SELECT MIN(cl.price), MAX(cl.price), AVG(cl.price) FROM CreditListing cl " +
                        "WHERE cl.status = 'ACTIVE' AND cl.listingType = 'FIXED'")
        Object[] getPriceStatistics();

        /**
         * Count user's active listings
         */
        @Query("SELECT COUNT(cl) FROM CreditListing cl WHERE cl.credit.user = :user AND cl.status = 'ACTIVE'")
        long countUserActiveListings(@Param("user") User user);

        // ==================== DATE RANGE QUERIES ====================

        /**
         * Find listings created within date range
         */
        @Query("SELECT cl FROM CreditListing cl WHERE cl.createdAt BETWEEN :startDate AND :endDate")
        List<CreditListing> findByCreatedAtBetween(
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        /**
         * Find listings created within date range with status
         */
        @Query("SELECT cl FROM CreditListing cl WHERE cl.createdAt BETWEEN :startDate AND :endDate AND cl.status = :status")
        List<CreditListing> findByCreatedAtBetweenAndStatus(
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        @Param("status") ListingStatus status);

        // ==================== MARKETPLACE RECOMMENDATIONS ====================

        /**
         * Find similar listings (by CO2 amount and price range) - for recommendations
         */
        @Query("SELECT cl FROM CreditListing cl WHERE cl.status = 'ACTIVE' AND cl.id != :excludeId AND " +
                        "ABS(cl.credit.co2ReducedKg - :co2Amount) <= :co2Tolerance AND " +
                        "(:listingType = 'AUCTION' OR ABS(cl.price - :referencePrice) <= :priceTolerance)")
        List<CreditListing> findSimilarListings(
                        @Param("excludeId") UUID excludeId,
                        @Param("co2Amount") BigDecimal co2Amount,
                        @Param("co2Tolerance") BigDecimal co2Tolerance,
                        @Param("referencePrice") BigDecimal referencePrice,
                        @Param("priceTolerance") BigDecimal priceTolerance,
                        @Param("listingType") ListingType listingType,
                        Pageable pageable);

        /**
         * Find listings from different sellers (exclude current user) - for marketplace
         * diversity
         */
        @Query("SELECT cl FROM CreditListing cl WHERE cl.status = 'ACTIVE' AND cl.credit.user != :excludeUser")
        Page<CreditListing> findByStatusExcludingUser(
                        @Param("excludeUser") User excludeUser,
                        Pageable pageable);

        // ==================== CUSTOM SORTING ====================

        /**
         * Find active listings ordered by price (ascending)
         */
        @Query("SELECT cl FROM CreditListing cl WHERE cl.status = 'ACTIVE' AND cl.listingType = 'FIXED' ORDER BY cl.price ASC")
        List<CreditListing> findActiveFixedListingsOrderByPriceAsc();

        /**
         * Find active listings ordered by CO2 amount (descending)
         */
        @Query("SELECT cl FROM CreditListing cl WHERE cl.status = 'ACTIVE' ORDER BY cl.credit.co2ReducedKg DESC")
        List<CreditListing> findActiveListingsOrderByCO2Desc();

        /**
         * Find newest active listings
         */
        @Query("SELECT cl FROM CreditListing cl WHERE cl.status = 'ACTIVE' ORDER BY cl.createdAt DESC")
        List<CreditListing> findActiveListingsOrderByNewest();

}
