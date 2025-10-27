package com.carboncredit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "momo")
public class MoMoConfig {
    private String partnerCode;
    private String accessKey;
    private String secretKey;
    private String apiEndpoint;
    private String returnUrl;
    private String notifyUrl;
    private String requestType;
}
