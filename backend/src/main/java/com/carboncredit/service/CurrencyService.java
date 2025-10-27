package com.carboncredit.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CurrencyService {
    @Value("${currency.usd-to-vnd-rate}")
    private BigDecimal usdToVndRate;

    /**
     * Convert USD to VND
     * 
     * @param usdAmount in USD
     * @return Amount in VND (no decimals)
     */
    public BigDecimal convertUsdToVnd(BigDecimal usdAmount) {
        BigDecimal vndAmount = usdAmount.multiply(usdToVndRate).setScale(0, RoundingMode.HALF_UP);

        log.debug("Current conversion: ${} USD -> {} VND (rate: {})", usdAmount, vndAmount, usdToVndRate);

        return vndAmount;
    }

    /**
     * Convert VND to USD
     * 
     * @param vndAmount Amount in VND
     * @return Amount in USD (2 decimal places)
     */
    public BigDecimal convertVndToUsd(BigDecimal vndAmount) {
        BigDecimal usdAmount = vndAmount.divide(usdToVndRate, 2, RoundingMode.HALF_UP);

        log.debug("Currency conversion: {} VND → ${} USD (rate: {})",
                vndAmount, usdAmount, usdToVndRate);

        return usdAmount;
    }

    /**
     * Get current exchange rate
     */
    public BigDecimal getExchangeRate() {
        return usdToVndRate;
    }

}
