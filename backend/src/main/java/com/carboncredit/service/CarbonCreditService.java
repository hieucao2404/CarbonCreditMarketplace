package com.carboncredit.service;

import com.carboncredit.entity.CarbonCredit;
import com.carboncredit.entity.CarbonCredit.CreditStatus;
import com.carboncredit.entity.JourneyData;
import com.carboncredit.entity.User;
import com.carboncredit.repository.CarbonCreditRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CarbonCreditService {

    private final CarbonCreditRepository carbonCreditRepository;
    private final AuditService auditService;

    public BigDecimal calculateCO2Reduction(BigDecimal distanceKm, BigDecimal energyConsumeKwh) {
        // avergage gasoline car emission
        BigDecimal avgCarEmissionPerKm = new BigDecimal("0.21");
        BigDecimal gidEmissionPerKwh = new BigDecimal("0.5");

        // voided emission from not drive a gasoline car
        BigDecimal avoidedCarEmission = distanceKm.multiply(avgCarEmissionPerKm);
        // emission that from EV
        BigDecimal electricityEmissions = energyConsumeKwh.multiply(gidEmissionPerKwh);

        // net CO2 reduce
        BigDecimal netReduction = avoidedCarEmission.subtract(electricityEmissions);

        // Ensure non-negative (if grid is dirtier than car, no credit given)
        return netReduction.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    public CarbonCredit createCarbonCredit(JourneyData journey) {
        CarbonCredit credit = new CarbonCredit();
        credit.setUser(journey.getUser());
        credit.setJourney(journey);
        credit.setCo2ReducedKg(journey.getCo2ReducedKg());
        credit.setStatus(CarbonCredit.CreditStatus.PENDING);
        credit.setCreditAmount(calculateCreditAmount(journey.getCo2ReducedKg(), CarbonCredit.CreditStatus.PENDING));

        return carbonCreditRepository.save(credit);
    }

    public CarbonCredit verifyCarbonCredit(UUID creditId, User verifier, String comments) {
        // role enforcement at service level
        if (verifier == null || verifier.getRole() != User.UserRole.CVA) {
            throw new SecurityException("Only CVA users can verify carbon credits");
        }
        CarbonCredit credit = carbonCreditRepository.findById(creditId)
                .orElseThrow(() -> new RuntimeException("Carbon credit not found"));
        // Idempotent: if already verified, return existing object (no duplicate credit)
        if (credit.getStatus() == CarbonCredit.CreditStatus.VERIFIED) {
            return credit;
        }

        BigDecimal beforeAmount = credit.getCreditAmount();

        credit.setStatus(CarbonCredit.CreditStatus.VERIFIED);
        credit.setVerifiedAt(LocalDateTime.now());
        // Recalculate credit amount with VERIFIED status for better rate
        credit.setCreditAmount(calculateCreditAmount(credit.getCo2ReducedKg(), CarbonCredit.CreditStatus.VERIFIED));

        CarbonCredit saved = carbonCreditRepository.save(credit);

        // persisit audit recored (before/after amounts)
        try {
            auditService.logVerification(saved, verifier, beforeAmount, saved.getCreditAmount(), comments);
        } catch (Exception e) {
            // Log
            log.warn("Failed to write audit log for credit {}: {}", creditId, e.getMessage());

        }

        return saved;
    }

    public CarbonCredit rejectCarbonCredit(UUID creditId, User verifier, String comments) {
        CarbonCredit credit = carbonCreditRepository.findById(creditId)
                .orElseThrow(() -> new RuntimeException("Carbon credit not found"));

        // Idempotent: if already rejected return
        if (credit.getStatus() == CarbonCredit.CreditStatus.REJECTED) {
            return credit;
        }

        credit.setStatus(CarbonCredit.CreditStatus.REJECTED);
        CarbonCredit saved = carbonCreditRepository.save(credit);

        try {
            auditService.logRejection(saved, verifier, comments);
        } catch (Exception e) {
            log.warn("Failed to write audit log for rejection of credit {}: {}", creditId, e.getMessage());
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<CarbonCredit> findByUser(User user) {
        return carbonCreditRepository.findByUser(user);
    }

    @Transactional(readOnly = true)
    public List<CarbonCredit> findPendingCredits() {
        return carbonCreditRepository.findByStatus(CarbonCredit.CreditStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<CarbonCredit> findAvailableCredits() {
        return carbonCreditRepository.findAvailableCredits();
    }

    @Transactional(readOnly = true)
    public Optional<CarbonCredit> findById(UUID id) {
        return carbonCreditRepository.findById(id);
    }

    public CarbonCredit listCarbonCredit(UUID creditId, User listedBy) {
        // Input validation
        if (creditId == null) {
            throw new IllegalArgumentException("Credit ID cannot be null");

        }
        if (listedBy == null) {
            throw new IllegalArgumentException("User listing the credit cannot be null");
        }

        log.info("Attempting to list carbon credit {} by user {}", creditId, listedBy.getId());

        // load credit with better exception handling
        CarbonCredit credit = carbonCreditRepository.findById(creditId)
                .orElseThrow(() -> new EntityNotFoundException("Carbon credit not found with ID: " + creditId));

        // Ownership validation (assuming credits can only be listed by theri owners)
        if (!credit.getUser().getId().equals(listedBy.getId())) {
            log.warn("User {} attetmted to list credit {} owned by {}", listedBy.getId(), creditId,
                    credit.getUser().getId());
            throw new SecurityException("User can only list their own credits");
        }

        // Status validation with more specific erro messages
        if (credit.getStatus() == CreditStatus.LISTED) {
            log.info("Credit {} is already listed, returning existing listing", creditId);
            return credit; // Idempotent behavior - already listed
        }

        if (credit.getStatus() != CreditStatus.VERIFIED) {
            log.warn("Attempted to list credit {} with invalid status: {}", creditId, credit.getStatus());
            throw new IllegalStateException(
                    String.format("Only verified credits can be listed. Current status: %s", credit.getStatus()));
        }

        // business rule validation
        if (credit.getCreditAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Cannot list credit with zero or negative amount");
        }

        // check if credit is expired if needed, not yet

        // Update credit status and metadata
        LocalDateTime now = LocalDateTime.now();
        credit.setStatus(CreditStatus.LISTED);
        credit.setListedAt(now);
        try {
            CarbonCredit savedCredit = carbonCreditRepository.save(credit);
            log.info("Successfully listed carbon credit {} at {}", creditId, now);

            // Optional: Publish domain event for downstream processing
            // publishCreditListedEvent(savedCredit, listedBy);

            return savedCredit;
        } catch (Exception e) {
            log.error("Failed to save listed credit {}: {}", creditId, e.getMessage());
            throw new RuntimeException("Failed to list credit: " + e.getMessage(), e);
        }

    }

    public CarbonCredit markAsSold(UUID creditId) {
        CarbonCredit credit = carbonCreditRepository.findById(creditId)
                .orElseThrow(() -> new RuntimeException("Carbon credit not found"));

        if (credit.getStatus() != CreditStatus.LISTED) {
            throw new RuntimeException("Only listed credits can be sold");
        }

        credit.setStatus(CreditStatus.SOLD);
        // Credit amount remains the same when sold

        return carbonCreditRepository.save(credit);
    }

    private BigDecimal calculateCreditAmount(BigDecimal co2ReducedKg, CreditStatus status) {
        // Edge case - invalid CO2 amount
        if (co2ReducedKg == null || co2ReducedKg.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Rejected credits get no value
        if (status == CreditStatus.REJECTED) {
            return BigDecimal.ZERO;
        }

        // Base calculation: 1000kg CO2 = 1 credit
        BigDecimal baseCredits = co2ReducedKg.divide(new BigDecimal("1000"), 6, RoundingMode.HALF_UP);

        // Tiered multipliers based on CO2 amount (encourage longer trips)
        BigDecimal multiplier;
        if (co2ReducedKg.compareTo(new BigDecimal("50")) >= 0) {
            multiplier = new BigDecimal("1.5"); // 50% bonus for long trips 50+ kg CO2
        } else if (co2ReducedKg.compareTo(new BigDecimal("20")) >= 0) {
            multiplier = new BigDecimal("1.2"); // 20% bonus for medium trips
        } else if (co2ReducedKg.compareTo(new BigDecimal("5")) >= 0) {
            multiplier = new BigDecimal("1.0"); // Standard rate for regular trips
        } else {
            multiplier = new BigDecimal("0.5"); // Half rate for short trips <5 kg CO2
        }

        // Apply status-based verification confidence
        BigDecimal statusMultiplier;
        switch (status) {
            case PENDING:
                statusMultiplier = new BigDecimal("0.7"); // Conservative rate for unverified
                break;
            case VERIFIED:
                statusMultiplier = new BigDecimal("1.0"); // Full rate for verified credits
                break;
            case LISTED:
            case SOLD:
                statusMultiplier = new BigDecimal("1.0"); // Maintain verified rate
                break;
            default:
                statusMultiplier = new BigDecimal("0.8"); // Default moderate rate
                break;
        }

        BigDecimal finalCredits = baseCredits.multiply(multiplier).multiply(statusMultiplier);

        return finalCredits.setScale(6, RoundingMode.HALF_UP);
    }
}
