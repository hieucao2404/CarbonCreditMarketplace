package com.carboncredit.dto;

import com.carboncredit.entity.InspectionAppointment;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
public class InspectionAppointmentDTO {
    private UUID id;
    private UUID journeyId;
    private UserDTO evOwner;
    private UserDTO cva;
    private VerificationStationDTO station;
    private LocalDateTime appointmentTime;
    private String status;
    private String cvaNotes;
    private LocalDateTime createdAt;

    public InspectionAppointmentDTO(InspectionAppointment appointment) {
        this.id = appointment.getId();
        this.journeyId = appointment.getJourney() != null ? appointment.getJourney().getId() : null;
        this.evOwner = appointment.getEvOwner() != null ? new UserDTO(appointment.getEvOwner()) : null;
        this.cva = appointment.getCva() != null ? new UserDTO(appointment.getCva()) : null;
        
        if (appointment.getStation() != null) {
            this.station = new VerificationStationDTO(appointment.getStation());
        }
        
        this.appointmentTime = appointment.getAppointmentTime();
        this.status = appointment.getStatus().toString();
        this.cvaNotes = appointment.getCvaNotes();
        this.createdAt = appointment.getCreatedAt();
    }
}