package com.carboncredit.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateVehicleRequest {
    @NotBlank(message = "Model cannot be blank")
    @Size(max = 50, message = "Model name cannot exceed 50 characters")
    private String model;

    @NotNull(message = "Registration date cannot be null")
    @PastOrPresent(message = "Registration date cannot be in the future")
    private LocalDate registrationDate;
}
