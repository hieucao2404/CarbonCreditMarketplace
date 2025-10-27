package com.carboncredit.service;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

import com.carboncredit.config.VNPayConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class VNPayService {

    private final VNPayConfig vnPayConfig;
    private final CurrencyService currencyService;

    /**
     * create VNPay payment URL
     * 
     * @param userId      User ID
     * @param amountInUsd Amount in USD (will be converted to VND)
     * @param ipAddress   Client IP Address
     * @return VNPAY payment URL
     */
    public String createPaymentUrl(UUID userId, BigDecimal amountInUsd, String ipAddress, String txnRef)
            throws UnsupportedEncodingException {
        log.info("🏦 Creating VNPAY payment URL");
        log.info("   TmnCode: {}", vnPayConfig.getTmnCode());
        log.info("   User: {}", userId);
        log.info("   Amount: ${} USD", amountInUsd);
        log.info("   TxnRef: {}", txnRef);

        // Convert USD to VND
        BigDecimal amountInVnd = currencyService.convertUsdToVnd(amountInUsd);
        log.info("   Converted: {} VND (rate: 1 USD = {} VND)",
                amountInVnd, currencyService.getExchangeRate());

        // Convert to VND cents (VNPAY requires amount * 100)
        long amountInCents = amountInVnd.multiply(new BigDecimal("100")).longValue();

        // generate unique transaction reference
        // String txnRef = "DEP_" + System.currentTimeMillis();

        /// build param
        Map<String, String> vnpParams = new TreeMap<>();
        vnpParams.put("vnp_Version", vnPayConfig.getVersion());
        vnpParams.put("vnp_Command", vnPayConfig.getCommand());
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(amountInCents));
        vnpParams.put("vnp_CurrCode", vnPayConfig.getCurrencyCode());
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", String.format("Deposit $%.2f USD (%.0f VND) - User: %s",
                amountInUsd, amountInVnd, userId));
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", ipAddress);

        // Add timestamps (Vietnam timezone)
        TimeZone vietnamTz = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
        Calendar calendar = Calendar.getInstance(vietnamTz);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(vietnamTz);
        String createDate = formatter.format(calendar.getTime());
        vnpParams.put("vnp_CreateDate", createDate);

        // Payment expires in 15 minutes
        calendar.add(Calendar.MINUTE, 15);
        String expireDate = formatter.format(calendar.getTime());
        vnpParams.put("vnp_ExpiredDate", expireDate);

        // Build query string and hash data
        StringBuilder query = new StringBuilder();
        StringBuilder hashData = new StringBuilder();

        for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                String encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII.toString());

                hashData.append(entry.getKey()).append('=').append(encodedValue).append('&');
                query.append(URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII.toString()))
                        .append('=').append(encodedValue).append('&');
            }
        }

        // Remove trailing '&'
        hashData.setLength(hashData.length() - 1);
        query.setLength(query.length() - 1);

        // Generate secure hash using HMAC SHA512
        String vnpSecureHash = hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());

        log.info("🔍 DEBUG - VNPAY Parameters:");
        for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
            log.info("   {} = {}", entry.getKey(), entry.getValue());
        }
        log.info("🔍 DEBUG - Hash Data: {}", hashData.toString());
        log.info("🔍 DEBUG - Secure Hash: {}", vnpSecureHash);
        
        // Build final payment URL
        String paymentUrl = vnPayConfig.getApiUrl() + "?" + query + "&vnp_SecureHash=" + vnpSecureHash;

        log.info("✅ Payment URL created successfully");
        log.info("   TxnRef: {}", txnRef);
        log.info("   Return URL: {}", vnPayConfig.getReturnUrl());
        log.info("   URL length: {} characters", paymentUrl.length());

        return paymentUrl;

    }

    /**
     * verify callback signature from VNPAY
     */
    public boolean verifyReturnUrl(Map<String, String> params) {
        String vnpSecureHash = params.get("vnp_SecureHash");

        if (vnpSecureHash == null || vnpSecureHash.isEmpty()) {
            log.warn("No signature found in callback");
            return false;
        }

        // Remove has params before verification
        Map<String, String> paramsToVerify = new TreeMap<>(params);
        paramsToVerify.remove("vnp_SecureHash");
        paramsToVerify.remove("vnp_SecureHashType");

        // Build hash data (alphabetically sorted)
        StringBuilder hashData = new StringBuilder();
        for (Map.Entry<String, String> entry : paramsToVerify.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                try {
                    hashData.append(entry.getKey()).append('=')
                            .append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII.toString()))
                            .append('&');
                } catch (UnsupportedEncodingException e) {
                    log.error("Error encoding parameter: {}", entry.getKey());
                }
            }
        }

        // Remove trailign '&'
        if (hashData.length() > 0) {
            hashData.setLength(hashData.length() - 1);
        }

        // Calculate hash and compare
        String calculatedHash = hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        boolean isValid = calculatedHash.equals(vnpSecureHash);

        log.info("🔐 Signature verification: {}", isValid ? "✅ VALID" : "❌ INVALID");
        if (!isValid) {
            log.warn("   Expected: {}", calculatedHash);
            log.warn("   Received: {}", vnpSecureHash);
        }

        return isValid;
    }

    /**
     * HMAC SHA512 encryption
     */
    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("❌ Error generating HMAC SHA512", e);
            return "";
        }
    }
}
