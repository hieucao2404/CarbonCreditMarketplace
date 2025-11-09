package com.carboncredit.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carboncredit.dto.CompleteInspectionRequest;
import com.carboncredit.dto.ScheduleAppointmentRequest;
import com.carboncredit.dto.VerificationStationDTO;
import com.carboncredit.entity.InspectionAppointment;
import com.carboncredit.entity.JourneyData;
import com.carboncredit.entity.JourneyData.VerificationStatus;
import com.carboncredit.entity.User;
import com.carboncredit.entity.VerificationStation;
import com.carboncredit.entity.InspectionAppointment.AppointmentStatus;
import com.carboncredit.exception.BusinessOperationException;
import com.carboncredit.exception.ResourceNotFoundException;
import com.carboncredit.repository.InspectionAppointmentRepository;
import com.carboncredit.repository.JourneyDataRepository;
import com.carboncredit.repository.VerificationStationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final JourneyDataRepository journeyDataRepository;
    private final InspectionAppointmentRepository appointmentRepository;
    private final VerificationStationRepository stationRepository;
    private final NotificationService notificationService;
    private final CVAService cvaService; // for final approval/rejection

    /**
     * Called by CVA. Moves a journey form PENDING to PENDING_INSPECTION
     * and creates a schedulable appointment
     */
    @Transactional
    public InspectionAppointment requestInspection(UUID journeyId, User cva) {
        log.info("CVA {} is requesting inspection for journey {}", cva.getUsername());

        JourneyData journey = journeyDataRepository.findById(journeyId)
                .orElseThrow(() -> new ResourceNotFoundException("Journey not found" + journeyId));

        if (journey.getVerificationStatus() != VerificationStatus.PENDING_VERIFICATION) {
            throw new BusinessOperationException("Journey is not pending verification");
        }

        // 1. Update Journey
        journey.setVerificationStatus(VerificationStatus.PENDING_INSPECTION);
        journey.setVerifiedBy(cva);
        journeyDataRepository.save(journey);

        // Create new Appointment
        InspectionAppointment appointment = new InspectionAppointment();
        appointment.setJourney(journey);
        appointment.setEvOwner(journey.getUser());
        appointment.setCva(cva);
        appointment.setStatus(AppointmentStatus.REQUESTED);
        InspectionAppointment savedAppointment = appointmentRepository.save(appointment);

        // 3. Notify EV Owner
        notificationService.notifyUser(
                journey.getUser(),
                "Inspection Required",
                "A CVA has requested a physical inspection for your journey. Please schedule an appointment.");

        log.info("Journey {} status updated to PENDING_INSPECTION. Appointment {} created.", journeyId,
                savedAppointment.getId());
        return savedAppointment;
    }

    /**
     * Called by EV Owner. Gets all action stations for scheduling
     */
    @Transactional(readOnly = true)
    public List<VerificationStationDTO> getActiveStations() {
        return stationRepository.findByIsActive(true).stream().map(VerificationStationDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Cakked by EVOwner. Books a time and station for an appointment
     */
    @Transactional
    public InspectionAppointment scheduleAppointment(ScheduleAppointmentRequest request, User evOwner) {
        log.info("EVOwner {} is scheduling appointment {}", evOwner.getUsername());

        InspectionAppointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found."));

        VerificationStation station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new ResourceNotFoundException("Station not found"));

        // Security check: Make sure the owner of the appointment is making schedule
        if (!appointment.getEvOwner().getId().equals(evOwner.getId())) {
            throw new BusinessOperationException("You do not have permission to schedule");
        }

        // 1. Update appointment
        appointment.setStation(station);
        appointment.setAppointmentTime(request.getAppointmentTime());
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        InspectionAppointment savedAppointment = appointmentRepository.save(appointment);

        // 2. Notify CVA
        notificationService.notifyUser(appointment.getCva(), "Inspection Scheduled",
                "An EV Owner has scheduled an inspection for journey " + appointment.getJourney().getId() + " on "
                        + request.getAppointmentTime() + " at " + station.getName());

        log.info("Appointment {} scheduled for {}", appointment.getId(), request.getAppointmentTime());
        return savedAppointment;
    }

    /**
     * Called by CVA. Final step for approve or reject the journey after inspection
     */
    @Transactional
    public JourneyData completeInspection(UUID appointmentId, CompleteInspectionRequest request, User cva) {
        log.info("CVA {} is completing inspection for appointment {}", cva.getUsername(), appointmentId);

        InspectionAppointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found."));

        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new BusinessOperationException("This appointment is not in a schedule");
        }

        // 1. Updae appoint ment
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setCvaNotes(request.getNotes());
        appointmentRepository.save(appointment);

        // 2 Approve or Rejct the Journey
        JourneyData journey = appointment.getJourney();
        if (request.getIsApproved()) {
            // Re-use your existing CVAService logic to ensure credits are created
            log.info("Inspection for journey {} is APPROVED.", journey.getId());
            return cvaService.approveJourneyByCVA(journey.getId(), cva, request.getNotes());
        } else {
            // Re-use your existing CVAService logic
            log.warn("Inspection for journey {} is REJECTED. Reason: {}", journey.getId(), request.getNotes());
            return cvaService.rejectJourneyByCVA(journey.getId(), cva, request.getNotes());
        }

    }
}