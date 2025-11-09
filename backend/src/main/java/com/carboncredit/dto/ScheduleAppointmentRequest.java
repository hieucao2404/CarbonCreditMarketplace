package com.carboncredit.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ScheduleAppointmentRequest {
    @NotNull
    private UUID appointmentId; // The appointment being scheduled
    @NotNull
    private UUID stationId;
    @NotNull
    private LocalDateTime appointmentTime;
}