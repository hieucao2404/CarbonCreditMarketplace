package com.carboncredit.controller;

import com.carboncredit.dto.*;
import com.carboncredit.entity.InspectionAppointment;
import com.carboncredit.entity.JourneyData;
import com.carboncredit.entity.User;
import com.carboncredit.exception.ResourceNotFoundException;
import com.carboncredit.service.UserService;
import com.carboncredit.service.VerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/verification")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VerificationController {

    private final VerificationService verificationService;
    private final UserService userService;

    private User getCurrentUser(Authentication authentication) {
        return userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * (CVA) CVA requests a physical inspection for a journey.
     * This creates a 'REQUESTED' appointment.
     */
    @PostMapping("/journey/{journeyId}/request-inspection")
    @PreAuthorize("hasRole('CVA')")
    public ResponseEntity<ApiResponse<InspectionAppointment>> requestInspection(
            @PathVariable UUID journeyId,
            Authentication authentication) {
        
        User cva = getCurrentUser(authentication);
        InspectionAppointment appointment = verificationService.requestInspection(journeyId, cva);
        return ResponseEntity.ok(ApiResponse.success("Inspection requested. Waiting for EV Owner to schedule.", appointment));
    }

    /**
     * (EV_OWNER) Get all active verification stations to choose from.
     */
    @GetMapping("/stations")
    @PreAuthorize("hasRole('EV_OWNER')")
    public ResponseEntity<ApiResponse<List<VerificationStationDTO>>> getActiveStations() {
        
        List<VerificationStationDTO> stations = verificationService.getActiveStations();
        return ResponseEntity.ok(ApiResponse.success(stations));
    }

    /**
     * (EV_OWNER) EV Owner books an appointment.
     */
    @PostMapping("/schedule")
    @PreAuthorize("hasRole('EV_OWNER')")
    public ResponseEntity<ApiResponse<InspectionAppointment>> scheduleAppointment(
            @Valid @RequestBody ScheduleAppointmentRequest request,
            Authentication authentication) {
        
        User evOwner = getCurrentUser(authentication);
        InspectionAppointment appointment = verificationService.scheduleAppointment(request, evOwner);
        return ResponseEntity.ok(ApiResponse.success("Appointment scheduled successfully.", appointment));
    }

    /**
     * (CVA) CVA marks the inspection as complete (Approve/Reject).
     */
    @PostMapping("/appointment/{appointmentId}/complete")
    @PreAuthorize("hasRole('CVA')")
    public ResponseEntity<ApiResponse<JourneyData>> completeInspection(
            @PathVariable UUID appointmentId,
            @Valid @RequestBody CompleteInspectionRequest request,
            Authentication authentication) {
        
        User cva = getCurrentUser(authentication);
        JourneyData journey = verificationService.completeInspection(appointmentId, request, cva);
        String message = request.getIsApproved() ? "Inspection completed and journey approved." : "Inspection completed and journey rejected.";
        return ResponseEntity.ok(ApiResponse.success(message, journey));
    }
}