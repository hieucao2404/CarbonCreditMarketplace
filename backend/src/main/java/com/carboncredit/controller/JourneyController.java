package com.carboncredit.controller;

import com.carboncredit.dto.ApiResponse;
import com.carboncredit.dto.JourneyDataDTO;
import com.carboncredit.dto.JourneyStatistics;
import com.carboncredit.entity.JourneyData;
import com.carboncredit.entity.User;
import com.carboncredit.exception.ResourceNotFoundException;
import com.carboncredit.service.JourneyDataService;
import com.carboncredit.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/journeys")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "*")
public class JourneyController {

    private final JourneyDataService journeyDataService;
    private final UserService userService;

    // ================ EV OWNER ENDPOINTS ===================
    /*
     * Create a new EV journey
     * Joirst starts wit pending status
     * Carbon credit created with pending
     * 
     * only EV_OWNER can create journeys
     */
    @PostMapping()
    @PreAuthorize("hasRole('EV_OWNER')")
    public ResponseEntity<ApiResponse<JourneyDataDTO>> createJourney(@Valid @RequestBody JourneyData journeyData,
            Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            // set user for the journey
            journeyData.setUser(user);

            // Create journey (automatically sets PENDING_VERIFICATION status)
            JourneyData savedJourney = journeyDataService.createJourney(journeyData);

            log.info("Journey created for user {}: {} km, {} kwH, {} kg C02 (PENDING verification)", user.getUsername(),
                    savedJourney.getDistanceKm(),
                    savedJourney.getEnergyConsumedKwh(), savedJourney.getCo2ReducedKg());

            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                    "Journey created successfully. Awaiting CVA verification", new JourneyDataDTO(savedJourney)));
        } catch (Exception e) {
            log.error("Error creating journey: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to create journey: " + e.getMessage()));
        }
    }

    /**
     * Get all journeys for the authenticated user
     * show all journeys with verification status
     */
    @GetMapping("/my-journeys")
    public ResponseEntity<ApiResponse<List<JourneyDataDTO>>> getMyJourneys(Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            List<JourneyData> journeys = journeyDataService.findByUser(user);
            List<JourneyDataDTO> journeyDTOs = journeys.stream()
                    .map(JourneyDataDTO::new)
                    .collect(Collectors.toList());

            log.info("Retrieved {} journeys for user {}", journeyDTOs.size(), user.getUsername());
            return ResponseEntity.ok(ApiResponse.success(journeyDTOs));

        } catch (Exception e) {
            log.error("Error fetching user journeys: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch journeys: " + e.getMessage()));

        }
    }

    /**
     * Get specific journey by Id (onlu if user owns it)
     */
    @GetMapping("/{journeyId}")
    public ResponseEntity<ApiResponse<JourneyDataDTO>> getJourney(@PathVariable UUID journeyId,
            Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("user not found"));

            JourneyData journey = journeyDataService.findById(journeyId);

            // Check owner ship(or allow CVA/ADMIN to view)
            boolean isOwner = journey.getUser().getId().equals(user.getId());
            boolean isCVA = user.getRole() == User.UserRole.CVA;
            boolean isAdmin = user.getRole() == User.UserRole.ADMIN;

            if (!isOwner && !isCVA && !isAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("You do not have permission to view this journey"));
            }
            return ResponseEntity.ok(ApiResponse.success(new JourneyDataDTO(journey)));
        } catch (Exception e) {
            log.error("Error fetching journey {}: {}", journeyId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Journey not found"));
        }
    }

    /**
     * Update existing journey (only if PENDING or REJECTED)
     * PUT /api/journeys/{journeyId}
     * 
     * Cannot update VERIFIED journeys (credits already issued)
     */
    @PutMapping("/{journeyId}")
    public ResponseEntity<ApiResponse<JourneyDataDTO>> updateJourney(
            @PathVariable UUID journeyId,
            @Valid @RequestBody JourneyData updatedJourney,
            Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            JourneyData existingJourney = journeyDataService.findById(journeyId);

            // Check ownership
            if (!existingJourney.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("You don't have permission to update this journey"));
            }

            // Check if journey can be updated
            if (existingJourney.getVerificationStatus() == JourneyData.VerificationStatus.VERIFIED) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Cannot update verified journey. Credits have been issued."));
            }

            JourneyData savedJourney = journeyDataService.updateJourney(journeyId, updatedJourney, user);

            return ResponseEntity.ok(ApiResponse.success(
                    "Journey updated successfully",
                    new JourneyDataDTO(savedJourney)));

        } catch (Exception e) {
            log.error("Error updating journey {}: {}", journeyId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to update journey: " + e.getMessage()));
        }
    }

    /**
     * Delete journey (only if PENDING or REJECTED)
     * DELETE /api/journeys/{journeyId}
     */
    @DeleteMapping("/{journeyId}")
    public ResponseEntity<ApiResponse<Void>> deleteJourney(
            @PathVariable UUID journeyId,
            Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            journeyDataService.deleteJourney(journeyId, user);

            return ResponseEntity.ok(ApiResponse.success("Journey deleted successfully", null));

        } catch (Exception e) {
            log.error("Error deleting journey {}: {}", journeyId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to delete journey: " + e.getMessage()));
        }
    }

    /**
     * Get journey statistics for authenticated user
     * GET /api/journeys/statistics
     * 
     * Includes verification status breakdown
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<JourneyStatistics>> getJourneyStatistics(
            Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            JourneyStatistics stats = journeyDataService.getJourneyStatistics(user);

            return ResponseEntity.ok(ApiResponse.success(stats));

        } catch (Exception e) {
            log.error("Error fetching journey statistics: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch statistics: " + e.getMessage()));
        }
    }

    // ==================== ADMIN ENDPOINTS ====================

    /**
     * Get all journeys (Admin only)
     * GET /api/journeys/admin/all
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<JourneyDataDTO>>> getAllJourneys() {
        try {
            List<JourneyData> journeys = journeyDataService.findAll();
            List<JourneyDataDTO> journeyDTOs = journeys.stream()
                    .map(JourneyDataDTO::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(journeyDTOs));

        } catch (Exception e) {
            log.error("Error fetching all journeys: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch journeys: " + e.getMessage()));
        }
    }

    /**
     * Get journeys by verification status (Admin/CVA)
     * GET /api/journeys/admin/by-status/{status}
     */
    @GetMapping("/admin/by-status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CVA')")
    public ResponseEntity<ApiResponse<List<JourneyDataDTO>>> getJourneysByStatus(
            @PathVariable String status) {
        try {
            JourneyData.VerificationStatus verificationStatus = JourneyData.VerificationStatus
                    .valueOf(status.toUpperCase());

            List<JourneyData> journeys = journeyDataService
                    .findByVerificationStatus(verificationStatus);

            List<JourneyDataDTO> dtos = journeys.stream()
                    .map(JourneyDataDTO::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(dtos));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid status: " + status));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch journeys"));
        }
    }
}