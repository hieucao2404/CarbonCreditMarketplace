package com.carboncredit.service;

import org.springframework.stereotype.Service;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    private static final String SENDER_NAME = "Carbon Credit Marketplace";
    private static final String SENDER_EMAIL = "carbon-credit@example.com";
    // RFC 822 format for display name
    private static final String FROM_ADDRESS = SENDER_NAME + " <" + SENDER_EMAIL + ">";
    // ✅ Backend endpoints - no frontend needed!
    private static final String VERIFICATION_LINK_BASE = "http://localhost:8080/api/auth/verify-email";
    private static final String PASSWORD_RESET_LINK_BASE = "http://localhost:8080/api/auth/reset-password-page";

    /**
     * Send email verification link to user
     * 
     * @param email        user's email
     * @param verification Token unique token for verification
     */
    public void sendVerificationEmail(String email, String verificationToken) {
        try {
            log.info("Preparing verification email for: {}", email);

            // validate inputs
            if (email == null || email.isEmpty()) {
                throw new IllegalArgumentException("Email cannot be null or empty");
            }
            if (verificationToken == null || verificationToken.isEmpty()) {
                throw new IllegalArgumentException("Verification token cannot be nnull");
            }

            String verificationUrl = VERIFICATION_LINK_BASE + "?token=" + verificationToken;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(FROM_ADDRESS);
            message.setTo(email);
            message.setSubject("Carbon Credit Marketplace - Xác thực Email của Bạn");
            message.setText(buildVerificationEmailBody(verificationUrl));

            mailSender.send(message);
            log.info("✅ Verification email sent successfully to: {}", email);
        } catch (IllegalArgumentException e) {
            log.error("Invalid input for verification email: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error(" Failed to send verification email to: {}", email, e);
            throw new RuntimeException("Failed to send verification email: " + e.getMessage());
        }
    }

    /**
     * Send welcome email after successful email verification
     * 
     * @param email    User's email address
     * @param username User's username
     * @throws RuntimeException if email sending fails
     */
    public void sendWelcomeEmail(String email, String username) {
        try {
            log.info("📧 Preparing welcome email for: {} ({})", username, email);

            if (email == null || email.isEmpty()) {
                throw new IllegalArgumentException("Email cannot be null or empty");
            }
            if (username == null || username.isEmpty()) {
                throw new IllegalArgumentException("Username cannot be null or empty");
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(FROM_ADDRESS);
            message.setTo(email);
            message.setSubject("Chào mừng đến Carbon Credit Marketplace!");
            message.setText(buildWelcomeEmailBody(username));

            mailSender.send(message);
            log.info("✅ Welcome email sent successfully to: {}", email);

        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid input for welcome email: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Failed to send welcome email to: {}", email, e);
            throw new RuntimeException("Failed to send welcome email: " + e.getMessage(), e);
        }
    }

    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(String email, String resetToken) {
        try {
            log.info(" Preparing password reset email for: {}", email);

            if (email == null || email.isEmpty()) {
                throw new IllegalArgumentException("Email cannot be null or empty");
            }
            if (resetToken == null || resetToken.isEmpty()) {
                throw new IllegalArgumentException("Reset token cannot be null or empty");
            }

            String resetUrl = PASSWORD_RESET_LINK_BASE + "?token=" + resetToken;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(FROM_ADDRESS);
            message.setTo(email);
            message.setSubject("Carbon Credit Marketplace - Đặt lại Mật khẩu");
            message.setText(buildPasswordResetEmailBody(resetUrl));

            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", email);

        } catch (IllegalArgumentException e) {
            log.error("Invalid input for password reset email: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", email, e);
            throw new RuntimeException("Failed to send password reset email: " + e.getMessage());
        }
    }

    /**
     * Send transaction notification email
     * 
     * @param email         User's email
     * @param transactionId Transaction ID
     * @param amount        Transaction amount
     * @param status        Transaction status
     */
    public void sendTransactionNotificationEmail(String email, String transactionId,
            String amount, String status) {
        try {
            log.info("📧 Preparing transaction notification for: {}", email);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(FROM_ADDRESS);
            message.setTo(email);
            message.setSubject("Carbon Credit Marketplace - Cập nhật Giao dịch");
            message.setText(buildTransactionEmailBody(transactionId, amount, status));

            mailSender.send(message);
            log.info("✅ Transaction notification sent to: {}", email);

        } catch (Exception e) {
            log.error("❌ Failed to send transaction notification to: {}", email, e);
            throw new RuntimeException("Failed to send transaction notification", e);
        }
    }

    /**
     * Send CVA verification status email
     * 
     * @param email    CVA's email
     * @param username CVA's username
     * @param status   Verification status (APPROVED/REJECTED/PENDING)
     * @param reason   Reason for status (if any)
     */
    public void sendCvaVerificationStatusEmail(String email, String username,
            String status, String reason) {
        try {
            log.info("📧 Preparing CVA verification status email for: {}", email);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(FROM_ADDRESS);
            message.setTo(email);
            message.setSubject("Carbon Credit Marketplace - Kết quả Xác thực CVA");
            message.setText(buildCvaStatusEmailBody(username, status, reason));

            mailSender.send(message);
            log.info("✅ CVA verification status email sent to: {}", email);

        } catch (Exception e) {
            log.error("❌ Failed to send CVA status email to: {}", email, e);
            throw new RuntimeException("Failed to send CVA status email", e);
        }
    }

    /**
     * Send batch email (for admin notifications)
     * 
     * @param recipients List of email addresses
     * @param subject    Email subject
     * @param body       Email body
     */
    public void sendBatchEmail(java.util.List<String> recipients, String subject, String body) {
        try {
            log.info("📧 Sending batch email to {} recipients", recipients.size());

            for (String email : recipients) {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(FROM_ADDRESS);
                message.setTo(email);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
            }

            log.info("✅ Batch email sent to {} recipients", recipients.size());

        } catch (Exception e) {
            log.error("❌ Failed to send batch email", e);
            throw new RuntimeException("Failed to send batch email", e);
        }
    }

    // ========================================
    // EMAIL BODY BUILDERS
    // ========================================

    private String buildVerificationEmailBody(String verificationUrl) {
        return "Chào mừng đến Carbon Credit Marketplace!\n\n" +
                "Tài khoản của bạn đã được tạo thành công. " +
                "Vui lòng xác thực email của bạn bằng cách nhấp vào liên kết dưới đây:\n\n" +
                verificationUrl + "\n\n" +
                "⏰ Liên kết này sẽ hết hạn sau 24 giờ.\n\n" +
                "Nếu bạn không tạo tài khoản này, vui lòng bỏ qua email này.\n\n" +
                "Trân trọng,\n" +
                "Carbon Credit Marketplace Team\n" +
                "contact@carboncredit.com";
    }

    private String buildWelcomeEmailBody(String username) {
        return "Chào " + username + ",\n\n" +
                "🎉 Chúc mừng! Email của bạn đã được xác thực thành công.\n\n" +
                "Bạn hiện có thể đăng nhập và bắt đầu sử dụng dịch vụ của chúng tôi:\n" +
                "👉 http://localhost:3000/login\n\n" +
                "Tính năng chính:\n" +
                "✅ Mua/bán tín chỉ carbon\n" +
                "✅ Theo dõi giao dịch\n" +
                "✅ Quản lý ví điện tử\n" +
                "✅ Xem báo cáo xác thực\n\n" +
                "Nếu có bất kỳ câu hỏi nào, vui lòng liên hệ với chúng tôi.\n\n" +
                "Trân trọng,\n" +
                "Carbon Credit Marketplace Team";
    }

    private String buildPasswordResetEmailBody(String resetUrl) {
        return "Bạn đã yêu cầu đặt lại mật khẩu.\n\n" +
                "Nhấp vào liên kết dưới đây để đặt lại mật khẩu của bạn:\n\n" +
                resetUrl + "\n\n" +
                "⏰ Liên kết này sẽ hết hạn sau 1 giờ.\n\n" +
                "Nếu bạn không yêu cầu điều này, vui lòng bỏ qua email này.\n\n" +
                "Trân trọng,\n" +
                "Carbon Credit Marketplace Team";
    }

    private String buildTransactionEmailBody(String transactionId, String amount, String status) {
        return "Giao dịch của bạn đã được cập nhật.\n\n" +
                "Chi tiết giao dịch:\n" +
                "📝 ID Giao dịch: " + transactionId + "\n" +
                "💰 Số tiền: " + amount + "\n" +
                "📊 Trạng thái: " + status + "\n\n" +
                "Truy cập tài khoản của bạn để xem chi tiết đầy đủ.\n\n" +
                "Trân trọng,\n" +
                "Carbon Credit Marketplace Team";
    }

    private String buildCvaStatusEmailBody(String username, String status, String reason) {
        String statusMessage = switch (status) {
            case "APPROVED" -> "✅ Tài khoản CVA của bạn đã được phê duyệt! Bạn có thể bắt đầu xác thực tín chỉ.";
            case "REJECTED" -> "❌ Tài khoản CVA của bạn đã bị từ chối. Lý do: " + reason;
            case "PENDING" -> "⏳ Tài khoản CVA của bạn đang chờ xét duyệt.";
            default -> "📋 Trạng thái xác thực: " + status;
        };

        return "Chào " + username + ",\n\n" +
                statusMessage + "\n\n" +
                "Nếu có bất kỳ câu hỏi nào, vui lòng liên hệ với admin.\n\n" +
                "Trân trọng,\n" +
                "Carbon Credit Marketplace Team";
    }

    /**
     * ✅ NEW: Send password reset confirmation email
     */
    public void sendPasswordResetConfirmationEmail(String email, String username) {
        try {
            log.info("📧 Sending password reset confirmation to: {}", email);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(FROM_ADDRESS);
            message.setTo(email);
            message.setSubject("Carbon Credit Marketplace - Mật khẩu đã được đặt lại");
            message.setText(buildPasswordResetConfirmationBody(username));

            mailSender.send(message);
            log.info("✅ Password reset confirmation sent to: {}", email);

        } catch (Exception e) {
            log.error("❌ Failed to send password reset confirmation to: {}", email, e);
        }
    }

    /**
     * ✅ NEW: Build password reset confirmation email body
     */
    private String buildPasswordResetConfirmationBody(String username) {
        return "Chào " + username + ",\n\n" +
                "✅ Mật khẩu của bạn đã được đặt lại thành công!\n\n" +
                "Nếu bạn không thực hiện hành động này, vui lòng liên hệ với chúng tôi ngay lập tức.\n\n" +
                "Đăng nhập: http://localhost:3000/login\n\n" +
                "Trân trọng,\n" +
                "Carbon Credit Marketplace Team";
    }
}
