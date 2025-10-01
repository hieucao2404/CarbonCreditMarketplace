package com.carboncredit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisputeRequest {
    private String reason;
    private String description;
    private String evidence;
}
