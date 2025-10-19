package com.carboncredit.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.carboncredit.dto.ApiResponse;
import com.carboncredit.dto.JourneyDataDTO;
import com.carboncredit.entity.JourneyData;
import com.carboncredit.entity.User;
import com.carboncredit.exception.ResourceNotFoundException;
import com.carboncredit.service.CVAService;
import com.carboncredit.service.UserService;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * CVAController - Carbon Verification Authority REST API
 * 
 * Endpoints for CVA users to:
 * - Review pending journeys
 * - Approve journeys and issue credits
 * - Reject journeys with reasons
 * - View verification statistics
 * 
 * All endpoints require CVA role authentication
 */

@Slf4j
@RestController
@RequestMapping("/api/cva")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CVAController {

    private final CVAService cvaService;
    private final UserService userService;

    /**
     * Get all pending journey for CVA reviews
     */

    @GetMapping("/pending-journeys")
    @PreAuthorize("hasRole('CVA')")
    public ResponseEntity<ApiResponse<List<JourneyDataDTO>>> getPendingJourneys() {
        try {
            List<JourneyData> pendingJourneys = cvaService.getPendingJourneyForVerification();
            List<JourneyDataDTO> dtos = pendingJourneys.stream().map(JourneyDataDTO::new).collect(Collectors.toList());

            log.info("Retrieved {}  pending journeys for CVA review", dtos.size());

            return ResponseEntity.ok(ApiResponse.success(dtos));
        } catch (Exception e) {
            log.error("Error fetching pending journeys: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch pending journeys: " + e.getMessage()));
        }
    }

    /**
     * Get speciofc journey for review
     * 
     */
    @GetMapping("/journey/{id}")
    @PreAuthorize("hasRole('CVA')")
    public ResponseEntity<ApiResponse<JourneyDataDTO>> getJourneyForReview(@PathVariable UUID id) {
        try {
            JourneyData journey = cvaService.getJourneyDataForView(id);
            return ResponseEntity.ok(ApiResponse.success(new JourneyDataDTO()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(ApiResponse.error("Journey not found: " + id));
        } catch (Exception e) {
            log.error("Errpr fetching journey {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to fetching journey: " + e.getMessage()));
        }
    }

    /**
     * Approve journey and carboncreidt
     */
    @PostMapping("/journey/{id}/approve")
    @PreAuthorize("hasRole('CVA')")
    public ResponseEntity<ApiResponse<JourneyDataDTO>> approveJourney(
            @PathVariable UUID id,
            @RequestParam(required = false, defaultValue = "Approved by CVA") String notes,
            Authentication authentication) {
        try {
            User cva = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("CVA user not found"));

            JourneyData approvedJourney = cvaService.approveJourneyByCVA(id, cva, notes);

            log.info("CVA {} approved journey {}", cva.getUsername(), id);

            return ResponseEntity.ok(ApiResponse.success(
                    "Journey verified successfully. Credits added to owner's wallet.",
                    new JourneyDataDTO(approvedJourney)));

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("Journey not found: " + id));
        } catch (Exception e) {
            log.error("Error approving journey {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to approve journey: " + e.getMessage()));
        }
    }

    /**
     * Reject journey with a reasom
     */
    @PostMapping("/journey/{id}/reject")
    @PreAuthorize("hasRole('CVA')")
    public ResponseEntity<ApiResponse<JourneyDataDTO>> rejectJourney(@PathVariable UUID id, @RequestParam @NotBlank(message = "Rejection reason is required") String reason, Authentication authentication) {
        try {
            User cva = userService.findByUsername(authentication.getName()).orElseThrow(() -> new ResourceNotFoundException("CVA user not found"));

            JourneyData rejectedJourney = cvaService.rejectJourneyByCVA(id, cva, reason);
            
            log.warn("CVA P{ rejected journey {}. Reason: {}", cva.getUsername());
            
            return ResponseEntity.ok(ApiResponse.success("Journey rejected. Reason: " + reason, new JourneyDataDTO(rejectedJourney)));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(ApiResponse.error("Journey not found: " + id));
        } catch (Exception e) {
            log.error("error rejecting journey {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to reject journey: " + e.getMessage()));
        }
        
    }
    

    /**
     * Get verification statistic
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('CVA')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCVAStatistics(Authentication authentication) {
        try {
            User cva = userService.findByUsername(authentication.getName()).orElseThrow(() -> new ResourceNotFoundException("CVA user not found exception"));

            Map<String, Object> stats = cvaService.getCVAStatistics(cva);

            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch(Exception e) {
            log.error("Error fetching CVA statistics: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to fetch statistics: " + e.getMessage()));
        }
    }

    /**
     * Get all journeys verified by current CVA
     * 
     */
    @GetMapping("/my-verifications")
    @PreAuthorize("hasRole('CVA')")
    public ResponseEntity<ApiResponse<List<JourneyDataDTO>>> getMyVerifications(Authentication authentication) {
        try {
            User cva = userService.findByUsername(authentication.getName()).orElseThrow(() -> new ResourceNotFoundException("CVA user not found"));

            List<JourneyData> verifications = cvaService.getMyVerifications(cva);
            List<JourneyDataDTO> dtos = verifications.stream().map(JourneyDataDTO::new).collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(dtos));
        } catch (Exception e) {
            log.error("Error fetching CVA verifications: {}", e.getMessage());
            return  ResponseEntity.badRequest().body(ApiResponse.error("Failed to fetch verifications: " + e.getMessage()));
        }
    }
    
    

}
