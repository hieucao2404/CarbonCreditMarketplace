package com.carboncredit.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.carboncredit.entity.Vehicle;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDTO {
    private UUID id;
    private UUID userId;
    private String username;
    private String vin;
    private String model;
    private LocalDate registrationDate;
    private LocalDateTime createdAt;
    private int journeyCount;

    // Constructor from Vehicle entity
    public VehicleDTO(Vehicle vehicle) {
        this.id = vehicle.getId();
        this.userId = vehicle.getUser() != null ? vehicle.getUser().getId() : null;
        this.username = vehicle.getUser() != null ? vehicle.getUser().getUsername() : null;
        this.vin = vehicle.getVin();
        this.model = vehicle.getModel();
        this.registrationDate = vehicle.getRegistrationDate();
        this.createdAt = vehicle.getCreatedAt();
        this.journeyCount = vehicle.getJourneys() != null ? vehicle.getJourneys().size() : 0;
    }
}