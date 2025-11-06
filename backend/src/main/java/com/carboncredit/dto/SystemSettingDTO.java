package com.carboncredit.dto;

import com.carboncredit.entity.SystemSetting;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettingDTO {
    private UUID settingId;
    private String settingKey;
    private String settingValue;
    private String description;
    private String dataType;
    private Boolean isEditable;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Constructor that maps from SystemSetting entity
     */
    public SystemSettingDTO(SystemSetting setting) {
        if (setting != null) {
            this.settingId = setting.getSettingId();
            this.settingKey = setting.getSettingKey();
            this.settingValue = setting.getSettingValue();
            this.description = setting.getDescription();
            this.dataType = setting.getDataType();
            this.isEditable = setting.getIsEditable();
            this.createdAt = setting.getCreatedAt();
            this.updatedAt = setting.getUpdatedAt();
        }
    }

    /**
     * Get typed value based on dataType
     */
    public Object getTypedValue() {
        if (settingValue == null || dataType == null) {
            return settingValue;
        }

        try {
            switch (dataType.toUpperCase()) {
                case "INTEGER":
                    return Integer.parseInt(settingValue);
                case "DECIMAL":
                case "PERCENTAGE":
                    return Double.parseDouble(settingValue);
                case "BOOLEAN":
                    return Boolean.parseBoolean(settingValue);
                case "STRING":
                default:
                    return settingValue;
            }
        } catch (NumberFormatException e) {
            return settingValue;
        }
    }
}