package com.carboncredit.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    public static class PaymentResult {
        private boolean success;
        private String transactionId;
        private String errorMessage;
        
        public PaymentResult(boolean success, String transactionId, String errorMessage) {
            this.success = success;
            this.transactionId = transactionId;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getTransactionId() {
            return transactionId;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    public PaymentResult processPayment(UUID transactionId, BigDecimal amount, String buyerId, String sellerId) {
        // simulate payment process
        try {
            // integrate with payment gateway (NOT YET IMPLEMENTED)
            Thread.sleep(100);

            // This is only for Demo (95% success rate)
            boolean success = Math.random() > 0.05;

            if (success) {
                return new PaymentResult(true, "PAY_" + UUID.randomUUID().toString(), null);
            } else {
                return new PaymentResult(false, null, "Payment gateway error");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new PaymentResult(false, null, "Payment processing interrupted");
        }
    }

    public boolean refundPayment(String paymentTransactionId, BigDecimal amount) {
        // Simulate refund processing
        return Math.random() > 0.1;
    }
}
