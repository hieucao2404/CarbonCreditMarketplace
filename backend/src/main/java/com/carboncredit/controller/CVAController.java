package com.carboncredit.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.carboncredit.dto.ApiResponse;
import com.carboncredit.dto.CarbonCreditDTO;
import com.carboncredit.dto.CreditListingDTO;
import com.carboncredit.dto.JourneyDataDTO;
import com.carboncredit.dto.MonthlyReportDTO;
import com.carboncredit.entity.JourneyData;
import com.carboncredit.entity.User;
import com.carboncredit.exception.ResourceNotFoundException;
import com.carboncredit.service.CVAService;
import com.carboncredit.service.CreditListingService;
import com.carboncredit.service.JourneyDataService;
import com.carboncredit.service.PdfGenerationService;
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

    private final PdfGenerationService pdfGenerationService;
    private final CVAService cvaService;
    private final UserService userService;
    private final JourneyDataService journeyDataService;
    private final CreditListingService creditListingService;

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
            return ResponseEntity.ok(ApiResponse.success(new JourneyDataDTO(journey)));
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
    public ResponseEntity<ApiResponse<JourneyDataDTO>> rejectJourney(@PathVariable UUID id,
            @RequestParam @NotBlank(message = "Rejection reason is required") String reason,
            Authentication authentication) {
        try {
            User cva = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("CVA user not found"));

            JourneyData rejectedJourney = cvaService.rejectJourneyByCVA(id, cva, reason);

            log.warn("CVA {} rejected journey {}. Reason: {}", cva.getUsername());

            return ResponseEntity.ok(
                    ApiResponse.success("Journey rejected. Reason: " + reason, new JourneyDataDTO(rejectedJourney)));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(ApiResponse.error("Journey not found: " + id));
        } catch (Exception e) {
            log.error("error rejecting journey {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to reject journey: " + e.getMessage()));
        }

    }

    // Add these methods to CVAController:

    @GetMapping("/pending-listings")
    @PreAuthorize("hasRole('CVA')")
    public ResponseEntity<ApiResponse<Page<CreditListingDTO>>> getPendingListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<CreditListingDTO> listings = creditListingService.getPendingListings(page, size);
            return ResponseEntity.ok(ApiResponse.success(listings));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/listing/{id}/approve")
    @PreAuthorize("hasRole('CVA')")
    public ResponseEntity<ApiResponse<CreditListingDTO>> approveListing(
            @PathVariable UUID id,
            Authentication authentication) {
        try {
            User cva = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("CVA not found"));
            CreditListingDTO approved = creditListingService.approveListing(id, cva);
            return ResponseEntity.ok(ApiResponse.success("Listing approved", approved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/listing/{id}/reject")
    @PreAuthorize("hasRole('CVA')")
    public ResponseEntity<ApiResponse<CreditListingDTO>> rejectListing(
            @PathVariable UUID id,
            @RequestParam String reason,
            Authentication authentication) {
        try {
            User cva = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("CVA not found"));
            CreditListingDTO rejected = creditListingService.rejectListing(id, cva, reason);
            return ResponseEntity.ok(ApiResponse.success("Listing rejected", rejected));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get all journeys for a specific user (by username).
     * For CVA review/auditing purposes.
     * * @param username The username of the user whose journeys are to be
     * retrieved.
     */
    @GetMapping("/user/{username}/journeys")
    @PreAuthorize("hasRole('CVA')")
    public ResponseEntity<ApiResponse<List<JourneyDataDTO>>> getJourneysByUsername(
            @PathVariable String username,
            Authentication authentication) {

        try {
            // 1. Find the user specified in the path
            // We use the controller's injected userService
            User targetUser = userService.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

            // 2. Call the service to get that user's journeys
            // (You will need to add this 'getJourneysForUser' method to CVAService)
            List<JourneyData> journeys = journeyDataService.getJourneysByUsername(targetUser.getUsername());

            List<JourneyDataDTO> dtos = journeys.stream()
                    .map(JourneyDataDTO::new)
                    .collect(Collectors.toList());

            log.info("CVA {} retrieved {} journeys for user {}",
                    authentication.getName(), dtos.size(), username);

            return ResponseEntity.ok(ApiResponse.success(dtos));

        } catch (ResourceNotFoundException e) {
            log.warn("CVA {} failed to find journeys for user {}: {}",
                    authentication.getName(), username, e.getMessage());
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching journeys for user {}: {}", username, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch journeys for user " + username));
        }
    }

    /**
     * Get verification statistic
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('CVA')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCVAStatistics(Authentication authentication) {
        try {
            User cva = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("CVA user not found exception"));

            Map<String, Object> stats = cvaService.getCVAStatistics(cva);

            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
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
            User cva = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("CVA user not found"));

            List<JourneyData> verifications = cvaService.getMyVerifications(cva);
            List<JourneyDataDTO> dtos = verifications.stream().map(JourneyDataDTO::new).collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(dtos));
        } catch (Exception e) {
            log.error("Error fetching CVA verifications: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch verifications: " + e.getMessage()));
        }
    }

    /**
     * Get all approved and rejected carbon credits
     * Shows credits with VERIFIED or REJECTED status
     */
    @GetMapping("/approved-credits")
    @PreAuthorize("hasRole('CVA')")
    public ResponseEntity<ApiResponse<List<CarbonCreditDTO>>> getApprovedCredits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            Authentication authentication) {
        try {
            // Get all verified/rejected journeys and their credits
            List<JourneyData> processedJourneys = cvaService.getProcessedJourneys();

            // Map to CarbonCreditDTO
            List<CarbonCreditDTO> credits = processedJourneys.stream()
                    .filter(j -> j.getCarbonCredit() != null)
                    .map(j -> {
                        CarbonCreditDTO dto = new CarbonCreditDTO(j.getCarbonCredit());
                        // Add journey info for context
                        dto.getJourneyId();
                        // dto.setJourney(new JourneyDataDTO(j));
                        return dto;
                    })
                    .collect(Collectors.toList());

            log.info("Retrieved {} approved/rejected credits", credits.size());
            return ResponseEntity.ok(ApiResponse.success(credits));
        } catch (Exception e) {
            log.error("Error fetching approved credits: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch approved credits: " + e.getMessage()));
        }
    }

    @PostMapping("/reports/download")
    public ResponseEntity<ByteArrayResource> downloadMonthlyReport(@RequestBody MonthlyReportDTO report) {
        try {
            log.info("📥 Received report download request for period: {}", report.getPeriod());

            log.debug("Report data: title={}, period={}, approved={}, rejected={}, rate={}",
                    report.getTitle(), report.getPeriod(), report.getApproved(), 
                    report.getRejected(), report.getRate());

            byte[] pdfBytes = pdfGenerationService.generateMonthlyReportPdf(report);

            log.info("✅ PDF generated successfully, size: {} bytes", pdfBytes.length);

            ByteArrayResource resource = new ByteArrayResource(pdfBytes);

            String filename = "CVA_Report_" + (report.getPeriod() != null 
                ? report.getPeriod().replaceAll("[^a-zA-Z0-9-]", "_") 
                : "Report") + ".pdf";

            log.info("📄 Downloading as: {}", filename);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfBytes.length)
                    .body(resource);

        } catch (Exception e) {
            log.error("❌ Error in downloadMonthlyReport: {}", e.getMessage(), e);
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

}
