package com.carboncredit.dto;

import com.carboncredit.entity.VerificationStation;
import lombok.Data;
import java.util.UUID;

@Data
public class VerificationStationDTO {
    private UUID id;
    private String name;
    private String address;
    private String operatingHours;

    public VerificationStationDTO(VerificationStation station) {
        this.id = station.getId();
        this.name = station.getName();
        this.address = station.getAddress();
        this.operatingHours = station.getOperatingHours();
    }
}