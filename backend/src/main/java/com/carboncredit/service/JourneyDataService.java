package com.carboncredit.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.carboncredit.entity.JourneyData;
import com.carboncredit.repository.JourneyDataRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;


@Service
@Transactional
@AllArgsConstructor

public class JourneyDataService {

    private final JourneyDataRepository journeyDataRepository;
    private final CarbonCreditService carbonCreditService;

    
    //Method to save journey and auto-calculate c02
    public JourneyData saveJourneyData(JourneyData journeyData) {
        // calcualte co2 before saving
        BigDecimal co2Reduced = carbonCreditService.calculateCO2Reduction(
            journeyData.getDistanceKm(),
            journeyData.getEnergyConsumedKwh()
        );
        journeyData.setCo2ReducedKg(co2Reduced);
        return journeyDataRepository.save(journeyData);
    }

   
}
