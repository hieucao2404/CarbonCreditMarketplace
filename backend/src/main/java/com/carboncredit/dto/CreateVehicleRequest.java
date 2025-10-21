package com.carboncredit.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data
public class CreateVehicleRequest {
    @NotBlank(message = "VIN cannot be blank")
    private String vin;

    @NotBlank(message = "Model cannot be blank")
    private String model;

    @NotNull(message = "Registration date cannot be null")
    private LocalDate registrationDate;
}
