package com.carboncredit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CompleteInspectionRequest {
    @NotNull
    private Boolean isApproved;
    @NotBlank
    private String notes; // CVA's final verification notes or rejection reason
}