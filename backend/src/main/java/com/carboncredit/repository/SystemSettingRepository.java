package com.carboncredit.repository;

import com.carboncredit.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, UUID> {
    
    /**
     * Find setting by unique key
     */
    Optional<SystemSetting> findBySettingKey(String settingKey);
    
    /**
     * Check if setting exists by key
     */
    boolean existsBySettingKey(String settingKey);
    
    /**
     * Get all settings ordered by key for consistent display
     */
    @Query("SELECT s FROM SystemSetting s ORDER BY s.settingKey")
    List<SystemSetting> findAllOrderedByKey();
}
