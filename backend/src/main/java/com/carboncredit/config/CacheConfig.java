package com.carboncredit.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration for system settings
 * Uses Spring's simple in-memory cache by default
 */
@Configuration
@EnableCaching
public class CacheConfig {
    // Spring Boot auto-configures SimpleCacheManager with default TTL

}
