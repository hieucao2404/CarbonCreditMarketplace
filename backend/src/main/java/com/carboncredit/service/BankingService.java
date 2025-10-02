package com.carboncredit.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BankingService {

    //Mock implementation for now - Phase 1 as documentaiton
    public boolean processDeposit(UUID userId, BigDecimal amount, String paymentMethodId) {
        try {
            //simulate banking API call
            log.info("Processing deposit for user {}: ${} via payment method {}", userId, amount, paymentMethodId);

            //Mock 95% success rate (same as PaymentService pattern)
            boolean success = Math.random() > 0.05;

            if(success) {
                log.info("Banking deposit successful for user {}: ${}", userId, amount);
            } else {
                log.warn("Banking deposit failed for user {}: ${}", userId, amount);
            }
            return success;
        } catch (Exception e) {
            log.error("Banking deposit error for user {}: {}", userId, e.getMessage());
            return false;
        }
    }

    public boolean processWithdrawal(UUID userId, BigDecimal amount, String bankAccountInfo) {
        try {
            // simulate banking API call
            log.info("Processing withdrawal for user {}: ${} to account {}", userId, amount, maskBankAccount(bankAccountInfo));

            //mock 90% success rate (slightly lower for withdrawal)
            boolean success = Math.random() > 0.10;

            if(success) {
                log.info("Banking withdrawal successful for user {}: ${}", userId, amount);
            } else {
                log.warn("Banking withdrawal failed for user {}: ${}", userId, amount);
            }
            return success;
        } catch (Exception e) {
            log.error("Banking withdrawal error for user {}: {}", userId, e.getMessage());
            return false;
        }
    }


    private String maskBankAccount(String accountInfo) {
        if(accountInfo == null || accountInfo.length() < 4) {
            return "****";
        }
        return "****" + accountInfo.substring(accountInfo.length() - 4);
    }
    
}
