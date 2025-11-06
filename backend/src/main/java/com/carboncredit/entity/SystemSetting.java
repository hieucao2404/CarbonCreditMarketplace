package com.carboncredit.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * System Setting Entity
 * Stores platform-wide configuration values
 */
@Entity
@Table(name = "system_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "setting_id")
    private UUID settingId;

    @Column(name = "setting_key", unique = true, nullable = false, length = 100)
    private String settingKey;

    @Column(name = "setting_value", nullable = false, columnDefinition = "TEXT")
    private String settingValue;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "data_type", length = 20)
    private String dataType; // STRING, INTEGER, DECIMAL, BOOLEAN, PERCENTAGE

    @Column(name = "is_editable")
    private Boolean isEditable = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Constructor for creating new settings
     */
    public SystemSetting(String settingKey, String settingValue, String description, String dataType) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.description = description;
        this.dataType = dataType;
        this.isEditable = true;
    }

    /**
     * Validate setting value matches data type
     */
    public boolean isValidValue(String value) {
        if (value == null || dataType == null) {
            return false;
        }

        try {
            switch (dataType.toUpperCase()) {
                case "INTEGER":
                    Integer.parseInt(value);
                    return true;
                case "DECIMAL":
                case "PERCENTAGE":
                    Double.parseDouble(value);
                    return true;
                case "BOOLEAN":
                    return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
                case "STRING":
                    return true;
                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return "SystemSetting{" +
                "settingId=" + settingId +
                ", settingKey='" + settingKey + '\'' +
                ", settingValue='" + settingValue + '\'' +
                ", dataType='" + dataType + '\'' +
                '}';
    }
}