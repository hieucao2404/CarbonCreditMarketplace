package com.carboncredit.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Configuration
@Getter
public class VNPayConfig {

    @Value("${vnpay.tmn-code}")
    private String tmnCode;

    @Value("${vnpay.hash-secret}")
    private String hashSecret;

    @Value("${vnpay.api-url}")
    private String apiUrl;

    @Value("${vnpay.return-url}")
    private String returnUrl;

    @Value("${vnpay.version:2.1.0}")
    private String version;

    @Value("${vnpay.command}")
    private String command;

    @Value("${vnpay.currency-code}")
    private String currencyCode;
}
