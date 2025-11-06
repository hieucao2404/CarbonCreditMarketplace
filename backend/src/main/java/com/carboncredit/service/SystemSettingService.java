package com.carboncredit.service;

import com.carboncredit.dto.SystemSettingDTO;
import com.carboncredit.entity.SystemSetting;
import com.carboncredit.repository.SystemSettingRepository;
import com.carboncredit.util.DTOMapper;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemSettingService {

    private final SystemSettingRepository settingRepository;

    // ========================================
    // 1. READ OPERATIONS (Public API)
    // ========================================

    /**
     * Get all system settings (cached)
     */
    @Cacheable(value = "systemSettings", key = "'all'")
    @Transactional(readOnly = true)
    public List<SystemSettingDTO> getAllSettings() {
        log.debug("📋 Fetching all system settings");
        List<SystemSetting> entities = settingRepository.findAllOrderedByKey();
        log.debug("✅ Retrieved {} settings from database", entities.size());
        return DTOMapper.toSystemSettingDTOList(entities);
    }

    /**
     * Get a specific setting by key (cached)
     */
    @Cacheable(value = "systemSettings", key = "#key")
    @Transactional(readOnly = true)
    public SystemSettingDTO getSettingByKey(String key) {
        log.debug("🔍 Fetching setting: {}", key);

        SystemSetting setting = settingRepository.findBySettingKey(key)
                .orElseThrow(() -> new EntityNotFoundException("System setting not found: " + key));

        log.debug("✅ Found setting: {} = {}", key, setting.getSettingValue());
        return DTOMapper.toSystemSettingDTO(setting);
    }

    /**
     * Get raw setting value (for convenience)
     */
    @Transactional(readOnly = true)
    public String getSettingValue(String key) {
        SystemSettingDTO setting = getSettingByKey(key);
        return setting.getSettingValue();
    }

    /**
     * Get setting as Double
     */
    @Transactional(readOnly = true)
    public Double getSettingAsDouble(String key) {
        String value = getSettingValue(key);
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            log.error("❌ Cannot parse setting '{}' as Double: {}", key, value);
            throw new IllegalStateException("Setting '" + key + "' is not a valid decimal: " + value);
        }
    }

    /**
     * Get setting as Boolean
     */
    @Transactional(readOnly = true)
    public Boolean getSettingAsBoolean(String key) {
        String value = getSettingValue(key);
        return Boolean.parseBoolean(value);
    }

    /**
     * Update a setting value (clears cache)
     */
    @CacheEvict(value = "systemSettings", allEntries = true)
    @Transactional
    public SystemSettingDTO updateSetting(String key, String newValue) {
        log.debug("🔄 Updating setting: {} = {}", key, newValue);

        SystemSetting setting = settingRepository.findBySettingKey(key)
                .orElseThrow(() -> new EntityNotFoundException("System setting not found: " + key));

        if (Boolean.FALSE.equals(setting.getIsEditable())) {
            log.error("❌ Attempted to modify non-editable setting: {}", key);
            throw new IllegalStateException("Setting '" + key + "' is not editable");
        }

        String oldValue = setting.getSettingValue();
        setting.setSettingValue(newValue);
        SystemSetting updated = settingRepository.save(setting);

        log.info("✅ Updated setting: {} | {} → {}", key, oldValue, newValue);
        return DTOMapper.toSystemSettingDTO(updated);
    }

    /**
     * Create new setting (clears cache)
     */
    @CacheEvict(value = "systemSettings", allEntries = true)
    @Transactional
    public SystemSettingDTO createSetting(String key, String value, String description, String dataType) {
        log.debug("➕ Creating setting: {}", key);

        if (settingRepository.existsBySettingKey(key)) {
            throw new IllegalStateException("Setting with key '" + key + "' already exists");
        }

        SystemSetting setting = new SystemSetting(key, value, description, dataType);
        SystemSetting saved = settingRepository.save(setting);

        log.info("✅ Created setting: {} = {}", key, value);
        return DTOMapper.toSystemSettingDTO(saved);
    }
}
