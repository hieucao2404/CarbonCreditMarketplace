package com.carboncredit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.carboncredit.dto.JourneyStatistics;
import com.carboncredit.entity.JourneyData;
import com.carboncredit.entity.User;
import com.carboncredit.entity.Vehicle;
import com.carboncredit.repository.JourneyDataRepository;

@SpringBootTest
@ActiveProfiles("test")
public class JourneyDataServiceTest {

    @Mock
    private JourneyDataRepository journeyDataRepository;
    @Mock
    private CarbonCreditService carbonCreditService;
    @Mock
    private ValidationService validationService;

    private JourneyDataService journeyDataService;
    private User testUser;
    private Vehicle testVehicle;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        journeyDataService = new JourneyDataService(journeyDataRepository, carbonCreditService, validationService);

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");

        testVehicle = new Vehicle();
        testVehicle.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Test Journey Statistics Calculation")
    public void testJourneyStatisticsCalculation() {
        System.out.println("\n=== Journey Statistics Test ===");

        // Create test journeys
        List<JourneyData> testJourneys = Arrays.asList(
            createTestJourney(new BigDecimal("100"), new BigDecimal("25"), new BigDecimal("8.5")),
            createTestJourney(new BigDecimal("50"), new BigDecimal("12"), new BigDecimal("4.2")),
            createTestJourney(new BigDecimal("200"), new BigDecimal("50"), new BigDecimal("17.0"))
        );

        when(journeyDataRepository.findByUser(testUser)).thenReturn(testJourneys);

        JourneyStatistics stats = journeyDataService.getJourneyStatistics(testUser);

        System.out.println("Total Journeys: " + stats.getTotalJourneys());
        System.out.println("Total Distance: " + stats.getTotalDistanceKm() + "km");
        System.out.println("Total Energy: " + stats.getTotalEnergyConsumedKwh() + "kWh");
        System.out.println("Average Distance: " + stats.getAverageDistanceKm() + "km");
        System.out.println("Total CO2 Reduced: " + stats.getTotalCo2ReducedKg() + "kg");

        assertEquals(3, stats.getTotalJourneys());
        assertEquals(0, new BigDecimal("350").compareTo(stats.getTotalDistanceKm()));
        assertEquals(0, new BigDecimal("87").compareTo(stats.getTotalEnergyConsumedKwh()));
        assertEquals(0, new BigDecimal("29.70").compareTo(stats.getTotalCo2ReducedKg()));
    }

    private JourneyData createTestJourney(BigDecimal distance, BigDecimal energy, BigDecimal co2Reduced) {
        JourneyData journey = new JourneyData();
        journey.setId(UUID.randomUUID());
        journey.setUser(testUser);
        journey.setVehicle(testVehicle);
        journey.setDistanceKm(distance);
        journey.setEnergyConsumedKwh(energy);
        journey.setCo2ReducedKg(co2Reduced);
        journey.setStartTime(LocalDateTime.now().minusHours(2));
        journey.setEndTime(LocalDateTime.now());
        return journey;
    }
}