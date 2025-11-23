package com.carboncredit.controller;

import com.carboncredit.dto.ApiResponse;
import com.carboncredit.dto.RegisterRequest;
import com.carboncredit.entity.User;
import com.carboncredit.service.UserService;
import com.carboncredit.util.DTOMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication Controller
 * Handles user registration, email verification, and password reset
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;

    // ========================================
    // USER REGISTRATION
    // ========================================

    /**
     * Register new user (requires email verification)
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(
            @Valid @RequestBody RegisterRequest request) {
        try {
            log.info("📝 Registration request for: {}", request.getUsername());

            User user = userService.registerUser(request);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đăng ký thành công! Vui lòng kiểm tra email để xác thực tài khoản.");
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("emailVerified", user.getIsEmailVerified());
            response.put("userId", user.getId());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(true, "User registered successfully", response));

        } catch (IllegalArgumentException e) {
            log.warn("❌ Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("❌ Registration error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Registration failed", null));
        }
    }

    // ========================================
    // EMAIL VERIFICATION
    // ========================================

    /**
     * Verify email with token (returns HTML page)
     * GET /api/auth/verify-email?token={token}
     */
    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(
            @RequestParam @NotBlank String token) {
        try {
            log.info("🔐 Email verification request with token: {}", token.substring(0, 10) + "...");

            User user = userService.verifyUserEmail(token);
            log.info("✅ Email verified successfully for user: {}", user.getUsername());

            // Return success HTML page
            String html = loadHtmlTemplate("verify-success.html");
            return ResponseEntity.ok()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(html);

        } catch (IllegalArgumentException e) {
            log.warn("❌ Email verification failed: {}", e.getMessage());
            
            // Return error HTML page
            String html = loadHtmlTemplate("verify-error.html")
                    .replace("Link xác thực không hợp lệ hoặc đã hết hạn.", e.getMessage());
            return ResponseEntity.badRequest()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(html);
                    
        } catch (Exception e) {
            log.error("❌ Verification error", e);
            String html = loadHtmlTemplate("verify-error.html");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(html);
        }
    }
    
    /**
     * Helper method to load HTML template from resources
     */
    private String loadHtmlTemplate(String filename) {
        try {
            return new String(getClass().getClassLoader()
                    .getResourceAsStream("templates/" + filename)
                    .readAllBytes());
        } catch (Exception e) {
            log.error("Failed to load HTML template: {}", filename, e);
            return "<html><body><h1>Error loading page</h1></body></html>";
        }
    }

    /**
     * Resend verification email
     * POST /api/auth/resend-verification
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {
        try {
            log.info("📨 Resending verification email to: {}", request.getEmail());

            userService.resendVerificationEmail(request.getEmail());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Email xác thực đã được gửi lại thành công");
            response.put("email", request.getEmail());

            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Verification email resent", response));

        } catch (IllegalArgumentException e) {
            log.warn("❌ Resend verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("❌ Resend error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to resend verification email", null));
        }
    }

    // ========================================
    // PASSWORD RESET
    // ========================================

    /**
     * Request password reset (sends email with reset link)
     * POST /api/auth/forgot-password
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        try {
            log.info("🔐 Password reset requested for: {}", request.getEmail());

            userService.requestPasswordReset(request.getEmail());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Nếu email tồn tại, liên kết đặt lại mật khẩu đã được gửi.");
            response.put("email", request.getEmail());

            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Password reset email sent", response));

        } catch (Exception e) {
            // Don't reveal if email exists (security)
            log.warn("❌ Password reset error for: {}", request.getEmail());
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Nếu email tồn tại, liên kết đặt lại mật khẩu đã được gửi.");
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Password reset email sent", response));
        }
    }

    /**
     * Verify password reset token
     * GET /api/auth/verify-reset-token?token={token}
     */
    @GetMapping("/verify-reset-token")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyResetToken(
            @RequestParam @NotBlank String token) {
        try {
            log.info("🔐 Verifying password reset token: {}", token.substring(0, 10) + "...");

            User user = userService.verifyPasswordResetToken(token);

            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("email", user.getEmail());
            response.put("username", user.getUsername());

            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Token is valid", response));

        } catch (IllegalArgumentException e) {
            log.warn("❌ Invalid reset token: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Display password reset form (HTML page)
     * GET /api/auth/reset-password-page?token={token}
     */
    @GetMapping("/reset-password-page")
    public ResponseEntity<String> showResetPasswordPage(@RequestParam @NotBlank String token) {
        try {
            log.info("🔐 Loading password reset page for token: {}", token.substring(0, 10) + "...");

            // Verify token is valid before showing the form
            userService.verifyPasswordResetToken(token);

            // Load and return the HTML form
            String html = loadHtmlTemplate("reset-password-form.html");
            return ResponseEntity.ok()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(html);

        } catch (IllegalArgumentException e) {
            log.warn("❌ Invalid reset token: {}", e.getMessage());
            
            // Return error HTML page
            String html = loadHtmlTemplate("verify-error.html")
                    .replace("Link xác thực không hợp lệ hoặc đã hết hạn.", 
                            "Link đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.")
                    .replace("Gửi lại email", "Yêu cầu đặt lại mật khẩu mới")
                    .replace("/api/auth/resend-verification", "http://localhost:5173/forgot-password");
            return ResponseEntity.badRequest()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(html);
        } catch (Exception e) {
            log.error("❌ Error loading reset password page", e);
            String html = loadHtmlTemplate("verify-error.html");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(html);
        }
    }

    /**
     * Reset password using token
     * POST /api/auth/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        try {
            log.info("🔐 Resetting password with token: {}", request.getToken().substring(0, 10) + "...");

            userService.resetPassword(request.getToken(), request.getNewPassword());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Mật khẩu đã được đặt lại thành công! Bạn có thể đăng nhập ngay.");

            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Password reset successful", response));

        } catch (IllegalArgumentException e) {
            log.warn("❌ Password reset failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("❌ Password reset error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Password reset failed", null));
        }
    }

    // ========================================
    // REQUEST DTOS
    // ========================================

    @Data
    public static class ResendVerificationRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;
    }

    @Data
    public static class ForgotPasswordRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank(message = "Token is required")
        private String token;

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        private String newPassword;
    }
}

