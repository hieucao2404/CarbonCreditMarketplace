package com.carboncredit.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.carboncredit.config.MoMoConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MoMoService {

    private final MoMoConfig momoConfig;
    private final CurrencyService currencyService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Create MoMo payment
     */
    public String createPayment(UUID userId, BigDecimal amountUsd, String txnRef) {
        try {
            log.info("🏦 Creating MoMo payment");
            log.info("   Partner Code: {}", momoConfig.getPartnerCode());
            log.info("   User: {}", userId);
            log.info("   Amount: ${} USD", amountUsd);
            log.info("   TxnRef: {}", txnRef);

            // Convert USD to VND
            BigDecimal amountVnd = currencyService.convertUsdToVnd(amountUsd);
            long amount = amountVnd.longValue();
            
            log.info("   Converted: {} VND", amount);

            String requestId = txnRef;
            String orderId = txnRef;
            String orderInfo = String.format("Deposit $%.2f USD - User: %s", amountUsd, userId);
            String extraData = "";

            // Build raw signature
            String rawSignature = String.format(
                "accessKey=%s&amount=%d&extraData=%s&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=%s",
                momoConfig.getAccessKey(),
                amount,
                extraData,
                momoConfig.getNotifyUrl(),
                orderId,
                orderInfo,
                momoConfig.getPartnerCode(),
                momoConfig.getReturnUrl(),
                requestId,
                momoConfig.getRequestType()
            );

            log.info("🔐 Raw signature: {}", rawSignature);

            // Generate signature
            String signature = hmacSHA256(rawSignature, momoConfig.getSecretKey());
            log.info("🔐 Signature: {}", signature);

            // Build request body
            String requestBody = String.format(
                "{\"partnerCode\":\"%s\",\"accessKey\":\"%s\",\"requestId\":\"%s\",\"amount\":%d,\"orderId\":\"%s\",\"orderInfo\":\"%s\",\"redirectUrl\":\"%s\",\"ipnUrl\":\"%s\",\"extraData\":\"%s\",\"requestType\":\"%s\",\"signature\":\"%s\",\"lang\":\"en\"}",
                momoConfig.getPartnerCode(),
                momoConfig.getAccessKey(),
                requestId,
                amount,
                orderId,
                orderInfo,
                momoConfig.getReturnUrl(),
                momoConfig.getNotifyUrl(),
                extraData,
                momoConfig.getRequestType(),
                signature
            );

            log.info("📤 Sending request to MoMo: {}", requestBody);

            // Send request to MoMo
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            String response = restTemplate.postForObject(
                momoConfig.getApiEndpoint(),
                entity,
                String.class
            );

            log.info("📥 MoMo response: {}", response);

            // Parse response
            JsonNode jsonResponse = objectMapper.readTree(response);
            
            if (jsonResponse.has("payUrl")) {
                String payUrl = jsonResponse.get("payUrl").asText();
                log.info("✅ Payment URL created successfully: {}", payUrl);
                return payUrl;
            } else {
                log.error("❌ Failed to create payment URL: {}", response);
                throw new RuntimeException("MoMo payment creation failed: " + response);
            }

        } catch (Exception e) {
            log.error("❌ Error creating MoMo payment", e);
            throw new RuntimeException("MoMo payment error: " + e.getMessage(), e);
        }
    }

    /**
     * Verify MoMo callback signature
     */
    public boolean verifySignature(String rawSignature, String signature) {
        try {
            String calculatedSignature = hmacSHA256(rawSignature, momoConfig.getSecretKey());
            boolean isValid = calculatedSignature.equals(signature);
            
            log.info("🔐 Signature verification: {}", isValid ? "✅ VALID" : "❌ INVALID");
            if (!isValid) {
                log.warn("   Expected: {}", calculatedSignature);
                log.warn("   Received: {}", signature);
            }
            
            return isValid;
        } catch (Exception e) {
            log.error("❌ Error verifying signature", e);
            return false;
        }
    }

    /**
     * HMAC SHA256 encryption
     */
    private String hmacSHA256(String data, String key) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac hmacSHA256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmacSHA256.init(secretKey);
        byte[] hash = hmacSHA256.doFinal(data.getBytes(StandardCharsets.UTF_8));
        
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
