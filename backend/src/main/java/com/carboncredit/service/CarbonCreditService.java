package com.carboncredit.service;

import com.carboncredit.entity.CarbonCredit;
import com.carboncredit.entity.CarbonCredit.CreditStatus;
import com.carboncredit.entity.JourneyData;
import com.carboncredit.entity.User;
import com.carboncredit.repository.CarbonCreditRepository;
import com.carboncredit.repository.JourneyDataRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CarbonCreditService {

    private final CarbonCreditRepository carbonCreditRepository;
    private final JourneyDataRepository journeyDataRepository;

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

    public CarbonCredit verifyCarbonCredit(UUID creditId, User verifier) {
        CarbonCredit credit = carbonCreditRepository.findById(creditId)
                .orElseThrow(() -> new RuntimeException("Carbon credit not found"));

        credit.setStatus(CarbonCredit.CreditStatus.VERIFIED);
        credit.setVerifiedAt(LocalDateTime.now());
        // Recalculate credit amount with VERIFIED status for better rate
        credit.setCreditAmount(calculateCreditAmount(credit.getCo2ReducedKg(), CarbonCredit.CreditStatus.VERIFIED));

        return carbonCreditRepository.save(credit);
    }

    public CarbonCredit rejectCarbonCredit(UUID creditId, User verifier) {
        CarbonCredit credit = carbonCreditRepository.findById(creditId)
                .orElseThrow(() -> new RuntimeException("Carbon credit not found"));

        credit.setStatus(CarbonCredit.CreditStatus.REJECTED);

        return carbonCreditRepository.save(credit);
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

    public CarbonCredit listCarbonCredit(UUID creditId) {
        CarbonCredit credit = carbonCreditRepository.findById(creditId)
                .orElseThrow(() -> new RuntimeException("Carbon credit not found"));
        
        if (credit.getStatus() != CreditStatus.VERIFIED) {
            throw new RuntimeException("Only verified credits can be listed");
        }

        credit.setStatus(CreditStatus.LISTED);
        credit.setListedAt(LocalDateTime.now());
        // Credit amount remains the same when listing (already calculated as VERIFIED)

        return carbonCreditRepository.save(credit);
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
