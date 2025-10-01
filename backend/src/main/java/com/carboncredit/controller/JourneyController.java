package com.carboncredit.controller;

import com.carboncredit.dto.JourneyStatistics;
import com.carboncredit.entity.JourneyData;
import com.carboncredit.entity.User;
import com.carboncredit.service.JourneyDataService;
import com.carboncredit.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/journeys")
@RequiredArgsConstructor
public class JourneyController {

    private final JourneyDataService journeyDataService;
    private final UserService userService;

    /**
     * Create a new EV journey and automatically generate carbon credits
     */
    @PostMapping
    public ResponseEntity<JourneyData> createJourney(@RequestBody JourneyData journeyData, 
                                                   Authentication authentication) {
        try {
            // Get authenticated user
            User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Set user for the journey
            journeyData.setUser(user);
            
            // Create journey (automatically generates carbon credits)
            JourneyData savedJourney = journeyDataService.createJourney(journeyData);
            
            log.info("Journey created for user {}: {} km, {} kWh, {} kg CO2 reduced", 
                user.getUsername(), savedJourney.getDistanceKm(), 
                savedJourney.getEnergyConsumedKwh(), savedJourney.getCo2ReducedKg());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(savedJourney);
            
        } catch (Exception e) {
            log.error("Error creating journey: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get all journeys for the authenticated user
     */
    @GetMapping("/my-journeys")
    public ResponseEntity<List<JourneyData>> getMyJourneys(Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            List<JourneyData> journeys = journeyDataService.findByUser(user);
            return ResponseEntity.ok(journeys);
            
        } catch (Exception e) {
            log.error("Error fetching user journeys: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get specific journey by ID (only if user owns it)
     */
    @GetMapping("/{journeyId}")
    public ResponseEntity<JourneyData> getJourney(@PathVariable UUID journeyId,
                                                Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            JourneyData journey = journeyDataService.findById(journeyId);
            if (journey == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Check ownership
            if (!journey.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            return ResponseEntity.ok(journey);
            
        } catch (Exception e) {
            log.error("Error fetching journey {}: {}", journeyId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get journey statistics for authenticated user
     */
    @GetMapping("/statistics")
    public ResponseEntity<JourneyStatistics> getJourneyStatistics(Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            JourneyStatistics stats = journeyDataService.getJourneyStatistics(user);
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Error fetching journey statistics: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Admin endpoint: Get all journeys
     */
    @GetMapping("/admin/all")
    public ResponseEntity<List<JourneyData>> getAllJourneys(Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Check admin role
            if (user.getRole() != User.UserRole.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            List<JourneyData> journeys = journeyDataService.findAll();
            return ResponseEntity.ok(journeys);
            
        } catch (Exception e) {
            log.error("Error fetching all journeys: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update existing journey (before any credits are verified)
     */
    @PutMapping("/{journeyId}")
    public ResponseEntity<JourneyData> updateJourney(@PathVariable UUID journeyId,
                                                   @RequestBody JourneyData updatedJourney,
                                                   Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Check ownership
            JourneyData existingJourney = journeyDataService.findById(journeyId);
            if (existingJourney == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (!existingJourney.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // Update journey using the service method signature
            JourneyData savedJourney = journeyDataService.updateJourney(journeyId, updatedJourney, user);
            
            return ResponseEntity.ok(savedJourney);
            
        } catch (Exception e) {
            log.error("Error updating journey {}: {}", journeyId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}