package com.carboncredit.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MonthlyReportDTO {

    private String title;
    private String period;
    private int approvedCount;
    private String approved;
    private int rejectedCount;
    private String rejected;
    private String rate;
}
