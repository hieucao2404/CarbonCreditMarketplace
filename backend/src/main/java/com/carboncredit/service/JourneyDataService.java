package com.carboncredit.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.carboncredit.dto.JourneyStatistics;
import com.carboncredit.dto.JourneyStatisticsWithCredits;
import com.carboncredit.entity.CarbonCredit;
import com.carboncredit.entity.JourneyData;
import com.carboncredit.entity.User;
import com.carboncredit.exception.BusinessOperationException;
import com.carboncredit.exception.UnauthorizedOperationException;
import com.carboncredit.repository.CarbonCreditRepository;
import com.carboncredit.repository.JourneyDataRepository;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class JourneyDataService {
    private final JourneyDataRepository journeyDataRepository;
    private final CarbonCreditService carbonCreditService;
    private final ValidationService validationService;
    private final CarbonCreditRepository carbonCreditRepository;
    private final AuditService auditService;

    private void validateJourneyData(JourneyData journeyData) {
        validationService.validateJourneyData(journeyData);
    }

    // Creatate and save new journey with validation
    public JourneyData createJourney(JourneyData journeyData) {
        log.info("Creating new journey for user {}", journeyData.getUser().getId());

        // Inpue validation
        validateJourneyData(journeyData);

        // Calculate CO2 reduction
        BigDecimal co2Reduced = carbonCreditService.calculateCO2Reduction(journeyData.getDistanceKm(),
                journeyData.getEnergyConsumedKwh());

        journeyData.setCo2ReducedKg(co2Reduced);

        // INITILA VERIFICATION STATUS
        journeyData.setVerificationStatus(JourneyData.VerificationStatus.PENDING_VERIFICATION);

        // Set creation time if not already set
        if (journeyData.getCreatedAt() == null) {
            journeyData.setCreatedAt(LocalDateTime.now());
        }

        JourneyData savedJourney = journeyDataRepository.save(journeyData);

        CarbonCredit credit = new CarbonCredit();
        credit.setJourney(savedJourney);
        credit.setUser(journeyData.getUser());
        credit.setCo2ReducedKg(co2Reduced);
        credit.setCreditAmount(co2Reduced);
        credit.setStatus(CarbonCredit.CreditStatus.PENDING); // ⭐ PENDING

        carbonCreditRepository.save(credit);

        // Log audit
        auditService.logSubmission(credit, journeyData.getUser());

        log.info("Journey created with PENDING verification status");

        return savedJourney;

    }

    // Find journey by ID with exception handling
    @Transactional(readOnly = true)
    public JourneyData findById(UUID journeyId) {
        return journeyDataRepository.findById(journeyId)
                .orElseThrow(
                        () -> new EntityNotFoundException("JourneyData not found with id: " + journeyId.toString()));
    }

    // Find all journeys by user
    @Transactional(readOnly = true)
    public List<JourneyData> findByUser(User user) {
        if (user == null) {
            throw new ValidationException("User cannot be null");
        }
        return journeyDataRepository.findByUser(user);
    }

    // find journeys by user wuith data range
    @Transactional(readOnly = true)
    public List<JourneyData> findByUserAndDateRange(User user, LocalDateTime startDate, LocalDateTime endDate) {
        if (user == null) {
            throw new ValidationException("User cannot be null");
        }
        if (startDate == null || endDate == null) {
            throw new ValidationException("Start and end dates cannot be null");
        }

        if (startDate.isAfter(endDate)) {
            throw new ValidationException("Start date cannot be after end date");

        }

        return journeyDataRepository.findByUserAndStartTimeBetween(user, startDate, endDate);
    }

    // get total co2 reduction by user
    @Transactional(readOnly = true)
    public BigDecimal getTotalCO2ReductionByUser(User user) {
        if (user == null) {
            throw new ValidationException("User cannot be null");
        }

        BigDecimal total = journeyDataRepository.getTotalCo2ReductionByUser(user);
        return total != null ? total : BigDecimal.ZERO;
    }

    // Find journeys without carbon credits
    @Transactional(readOnly = true)
    public List<JourneyData> findJourneyWithoutCredits() {
        return journeyDataRepository.findJourneysWithoutCredits();
    }

    // get all jourenys (admin function )
    @Transactional(readOnly = true)
    public List<JourneyData> findAll() {
        return journeyDataRepository.findAll();
    }

    // Update existing journey
    public JourneyData updateJourney(UUID journeyId, JourneyData updatedData, User requestingUser) {
        log.info("Updating journey {} by user {}", journeyId, requestingUser.getId());

        JourneyData existing = findById(journeyId);

        // check onwner ship
        if (!existing.getUser().getId().equals(requestingUser.getId())) {
            throw new UnauthorizedOperationException(
                    requestingUser.getId().toString(), "journeyData", journeyId.toString(), "update");
        }

        // chekc if journey can be updated (no carbon credit yet)
        if (existing.getCarbonCredit() != null) {
            throw new BusinessOperationException("journeyData", "update",
                    "cannot update journey that already has carbon credits");
        }

        // validate updated data
        validateJourneyData(updatedData);

        // Update fields
        existing.setDistanceKm(updatedData.getDistanceKm());
        existing.setEnergyConsumedKwh(updatedData.getEnergyConsumedKwh());
        existing.setStartTime(updatedData.getStartTime());
        existing.setEndTime(updatedData.getEndTime());

        // Recalculate CO2 reductionn
        BigDecimal co2Reduced = carbonCreditService.calculateCO2Reduction(existing.getDistanceKm(),
                existing.getEnergyConsumedKwh());

        existing.setCo2ReducedKg(co2Reduced);

        JourneyData saved = journeyDataRepository.save(existing);
        log.info("Journey {} updated successfully", journeyId);

        return saved;

    }

    public void deleteJourney(UUID journeyId, User requestingUser) {
        log.info("Deleting journey {} by user {}", journeyId, requestingUser.getId());

        JourneyData journey = findById(journeyId);

        // Check ownership
        if (!journey.getUser().getId().equals(requestingUser.getId())) {
            throw new UnauthorizedOperationException(
                    requestingUser.getId().toString(), "JourneyData",
                    journeyId.toString(),
                    "delete");
        }

        // Check if journey can be deleted
        if (journey.getCarbonCredit() != null) {
            throw new BusinessOperationException("journeyData", "delete",
                    "cannot delete journey that has carbon credits");
        }

        journeyDataRepository.delete(journey);
        log.info("Journey {} deleted successfully", journeyId);
    }

    // Get journey statistics for a user
    @Transactional(readOnly = true)
    public JourneyStatistics getJourneyStatistics(User user) {
        List<JourneyData> journeys = findByUser(user);

        if (journeys.isEmpty()) {
            return new JourneyStatistics(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        BigDecimal totalDistance = journeys.stream().map(JourneyData::getDistanceKm).reduce(BigDecimal.ZERO,
                BigDecimal::add);

        BigDecimal totalEnergy = journeys.stream().map(JourneyData::getEnergyConsumedKwh).reduce(BigDecimal.ZERO,
                BigDecimal::add);

        BigDecimal totalCo2Reduced = journeys.stream().map(JourneyData::getCo2ReducedKg).reduce(BigDecimal.ZERO,
                BigDecimal::add);

        BigDecimal averageDistance = totalDistance.divide(BigDecimal.valueOf(journeys.size()), 2, RoundingMode.HALF_UP);

        return new JourneyStatistics(
                journeys.size(), totalDistance, totalEnergy, averageDistance, totalCo2Reduced);
    }

    @Transactional(readOnly = true)
    public List<JourneyData> findEligibleForCarbonCredits(User user) {
        return journeyDataRepository.findByUser(user).stream()
                .filter(journey -> journey.getCarbonCredit() == null)
                .filter(journey -> journey.getCo2ReducedKg() != null)
                .filter(journey -> journey.getCo2ReducedKg().compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    // Bulk create carbon credit for multiple journeys
    @Transactional
    public List<CarbonCredit> createCarbonCreditsForUser(User user) {
        List<JourneyData> eligibleJourneys = findEligibleForCarbonCredits(user);
        List<CarbonCredit> createdCredits = new ArrayList<>();

        for (JourneyData journey : eligibleJourneys) {
            try {
                CarbonCredit credit = carbonCreditService.createCarbonCredit(journey);
                createdCredits.add(credit);
                log.info("Created carbon credit for journey {}", journey.getId());
            } catch (Exception e) {
                log.error("Failed to create carbon credit for journey {}: {}", journey.getId(), e.getMessage());
            }
        }
        return createdCredits;
    }

    // Get journey statistics including carbon credit information
    @Transactional(readOnly = true)
    public JourneyStatisticsWithCredits getDetailedJourneyStatistics(User user) {
        List<JourneyData> journeys = findByUser(user);

        if (journeys.isEmpty()) {
            return new JourneyStatisticsWithCredits(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    0, 0, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        // Basic statistics
        BigDecimal totalDistance = journeys.stream().map(JourneyData::getDistanceKm).reduce(BigDecimal.ZERO,
                BigDecimal::add);

        BigDecimal totalEnergy = journeys.stream().map(JourneyData::getEnergyConsumedKwh).reduce(BigDecimal.ZERO,
                BigDecimal::add);

        BigDecimal totalCo2Reduced = journeys.stream()
                .map(JourneyData::getCo2ReducedKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageDistance = totalDistance.divide(BigDecimal.valueOf(journeys.size()), 2, RoundingMode.HALF_UP);

        // Carbon credit statistics
        List<JourneyData> journeysWithCredits = journeys.stream().filter(j -> j.getCarbonCredit() != null).toList();

        int journeysWithCreditCount = journeysWithCredits.size();
        int journeysWithoutCreditCount = journeys.size() - journeysWithCreditCount;

        BigDecimal totalCreditAmount = journeysWithCredits.stream().map(j -> j.getCarbonCredit().getCreditAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal potentialCreditAmount = journeys.stream()
                .filter(j -> j.getCarbonCredit() == null)
                .map(j -> carbonCreditService.calculateCO2Reduction(j.getDistanceKm(), j.getEnergyConsumedKwh()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new JourneyStatisticsWithCredits(journeys.size(), totalDistance, totalEnergy, averageDistance,
                totalCo2Reduced,
                journeysWithCreditCount, journeysWithoutCreditCount, totalCreditAmount, potentialCreditAmount);
    }

    public void deleteJourney(UUID journeyId) {
        JourneyData journey = findById(journeyId);

        // check if journey can be deleted (onlu pending / rejected journeys)
        if (journey.getVerificationStatus() == JourneyData.VerificationStatus.VERIFIED) {
            throw new BusinessOperationException("journeyData", "delete",
                    "Cannot delete verified journey. Credit have been issued");
        }

        // delete associated carbon credit if exists
        if (journey.getCarbonCredit() != null) {
            carbonCreditRepository.delete(journey.getCarbonCredit());
        }

        journeyDataRepository.delete(journey);
        log.info("Journey {} deleted successfully", journeyId);
    }

    /**
     * Find journeys by verification status
     * Used by CVA to get pending journeys, and by admin to filter journeys
     */
    @Transactional(readOnly = true)
    public List<JourneyData> findByVerificationStatus(JourneyData.VerificationStatus status) {
        if (status == null) {
            throw new ValidationException("Verification status cannot be null");
        }
        return journeyDataRepository.findByVerificationStatus(status);
    }

}
