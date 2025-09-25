package com.carboncredit.service;

import java.math.BigDecimal;
import java.util.UUID;

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

        
    }


    public PaymentResult processPayment(UUID TransactionId, BigDecimal amount, String buyerId, String sellerId) {
        // simulate payment process
        try {
            // integate with payment gateway (NOT DONE)
            Thread.sleep(100);

            //This is only for Demo (95% success rate)
            boolean success = Math.random() > 0.05;

            if(success) {
                return new PaymentResult(true, "PAY_" + UUID.randomUUID().toString(), null);
            } else {
                return new PaymentResult(false, null, "Payment gateway error");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new PaymentResult(false, null, "Payment processing interrupted");
        }
    }

    public boolean refundPayment(String paymentTranscationId, BigDecimal amount) {
        // Simulate refund processing
        return Math.random() > 0.1;
    }

}
