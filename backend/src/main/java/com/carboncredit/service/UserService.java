package com.carboncredit.service;

import com.carboncredit.dto.RegisterRequest;
import com.carboncredit.entity.User;
import com.carboncredit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletService walletService;
    private final EmailService emailService;

    /**
     * Register fnew user with email verfication requriement
     * 
     * @param request
     * @return
     */
    @Transactional
    public User registerUser(RegisterRequest request) {
        log.info(" Regigetering new user: {}", request.getUsername());

        // validate uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        // Create user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.UserRole.valueOf(request.getRole()));
        user.setFullName(request.getFullName() != null ? request.getFullName() : request.getUsername());
        user.setPhone(request.getPhone());

        // ✅ Set email verification fields
        user.setIsEmailVerified(false);
        user.setEmailVerificationToken(UUID.randomUUID().toString());

        User savedUser = userRepository.save(user);
        // Create wallet
        try {
            walletService.createWalletForUser(savedUser);
            log.info("✅ Wallet created for user: {}", savedUser.getUsername());
        } catch (Exception e) {
            log.error("⚠️ Failed to create wallet for user: {}", savedUser.getUsername());
        }

        // ✅ Send verification email
        try {
            log.info("📧 Attempting to send verification email to: {}", user.getEmail());
            emailService.sendVerificationEmail(user.getEmail(), user.getEmailVerificationToken());
            log.info("✅ Verification email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("❌ Email sending failed: {} - User {} still created (please check email config)", 
                    e.getMessage(), user.getUsername(), e);
            // Continue anyway - user is still created, they can resend verification email later
        }

        log.info("✅ User registered successfully: {} (awaiting email verification)", savedUser.getUsername());
        return savedUser;
    }

    // =======================
    // EMAIL VERIFICATION
    // =======================
    /**
     * Verify user email using token
     * 
     * @param request
     * @return
     */
    @Transactional
    public User verifyUserEmail(String token) {
        log.info("Verifying email with token: {}", token.substring(0, 10) + "..");

        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> {
                    log.warn("❌ Invalid verification token: {}", token);
                    return new IllegalArgumentException("Invalid or expired verification token");
                });

        if (user.getIsEmailVerified()) {
            log.warn("⚠️ User {} already verified", user.getUsername());
            return user;
        }

        // Mark as verified
        user.setIsEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        user.setEmailVerificationToken(null);

        User verifiedUser = userRepository.save(user);

        // Send welcome email
        try {
            emailService.sendWelcomeEmail(user.getEmail(), user.getUsername());
        } catch (Exception e) {
            log.warn("⚠️ Welcome email failed for: {}", user.getEmail());
        }

        log.info("✅ Email verified successfully for user: {}", user.getUsername());
        return verifiedUser;
    }

    /**
     * Resend verification email
     * 
     * @param request
     * @return
     */
    @Transactional
    public void resendVerificationEmail(String email) {
        log.info("Resending verification email to: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

        if (user.getIsEmailVerified()) {
            throw new IllegalArgumentException("Email already verified");
        }

        // Generate new token
        user.setEmailVerificationToken(UUID.randomUUID().toString());
        userRepository.save(user);

        // Send email
        emailService.sendVerificationEmail(user.getEmail(), user.getEmailVerificationToken());
        log.info("✅ Verification email resent to: {}", email);
    }

    // =====================
    // PASSWORD RESET
    // ======================
    /**
     * Requst password reset (generate token and sends email)
     * 
     * @param request
     * @return
     */
    @Transactional
    public void requestPasswordReset(String email) {
        log.info("Passwrod reset requested for: {}", email);

        User user = userRepository.findByEmail(email).orElseThrow(() -> {
            log.warn("Password reset requested for non-existent email: {}", email);
            return new IllegalArgumentException("If the email exists, a resetlink will be sent");
        });

        // Generate reset token (valid for 1 hour)
        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetExpiresAt(LocalDateTime.now().plusHours(1));
        user.setPasswordResetRequestedAt(LocalDateTime.now());

        userRepository.save(user);

        // send reset email
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
            log.info("Password reset email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", email);
            throw new RuntimeException("Failed to send password reset email");
        }
    }

    /**
     * Verify password reset token
     * 
     * @param request
     * @return
     */

    @Transactional(readOnly = true)
    public User verifyPasswordResetToken(String token) {
        log.info("🔐 Verifying password reset token: {}", token.substring(0, 10) + "...");

        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> {
                    log.warn("❌ Invalid password reset token: {}", token);
                    return new IllegalArgumentException("Invalid or expired reset token");
                });

        if (!user.isPasswordResetTokenValid()) {
            log.warn("❌ Expired password reset token for user: {}", user.getUsername());
            throw new IllegalArgumentException("Reset token has expired. Please request a new one.");
        }

        log.info("✅ Password reset token valid for user: {}", user.getUsername());
        return user;
    }

    /**
     * ✅ Reset password using token
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        log.info("🔐 Resetting password with token: {}", token.substring(0, 10) + "...");

        // Verify token first
        User user = verifyPasswordResetToken(token);

        // Validate new password
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.clearPasswordResetToken(); // Clear token after use

        userRepository.save(user);

        log.info("✅ Password reset successfully for user: {}", user.getUsername());

        // Send confirmation email
        try {
            emailService.sendPasswordResetConfirmationEmail(user.getEmail(), user.getUsername());
        } catch (Exception e) {
            log.warn("⚠️ Password reset confirmation email failed for: {}", user.getEmail());
        }
    }
    // ========================================
    // SCHEDULED CLEANUP TASKS
    // ========================================

    /**
     * ✅ Clean up expired password reset tokens (runs daily at 2 AM)
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredPasswordResetTokens() {
        log.info("🧹 Starting cleanup of expired password reset tokens");

        List<User> usersWithExpiredTokens = userRepository.findExpiredPasswordResetTokens();

        for (User user : usersWithExpiredTokens) {
            user.clearPasswordResetToken();
        }

        userRepository.saveAll(usersWithExpiredTokens);
        log.info("✅ Cleaned up {} expired password reset tokens", usersWithExpiredTokens.size());
    }

    /**
     * ✅ Delete unverified users older than 7 days (runs daily at 3 AM)
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteUnverifiedOldAccounts() {
        log.info("🧹 Starting cleanup of unverified old accounts");

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<User> unverifiedUsers = userRepository.findUnverifiedUsersOlderThan(sevenDaysAgo);

        for (User user : unverifiedUsers) {
            log.info("🗑️ Deleting unverified user: {} (created: {})",
                    user.getUsername(), user.getCreatedAt());
            userRepository.delete(user);
        }

        log.info("✅ Deleted {} unverified old accounts", unverifiedUsers.size());
    }

    @Transactional
    public User createUser(RegisterRequest request) {
        log.info("Creating user: {}", request.getUsername());

        // Validate username and email uniqueness
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Create User entity
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setRole(User.UserRole.valueOf(request.getRole()));

        // Encode password and set passwordHash directly
        String plainPassword = request.getPassword();
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        log.info("Encoding password for user: {}", user.getUsername());
        String encodedPassword = passwordEncoder.encode(plainPassword);
        user.setPasswordHash(encodedPassword);

        // Set timestamps
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        // Save user
        User savedUser = userRepository.save(user);
        log.info("User created successfully with encoded password");

        // Create wallet for the user
        try {
            walletService.createWalletForUser(savedUser);
            log.info("Wallet created for user: {}", savedUser.getUsername());
        } catch (Exception e) {
            log.error("Failed to create wallet for user {}: {}", savedUser.getUsername(), e.getMessage());
        }

        return savedUser;
    }

    public User createUser(User user) {
        log.info("Creating user from User entity: {}", user.getUsername());

        // Validate username and email uniqueness
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Get password from transient field
        String plainPassword = user.getPassword();
        log.info("Password from transient field is null? {}", plainPassword == null);

        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        // Encode password
        String encodedPassword = passwordEncoder.encode(plainPassword);
        user.setPasswordHash(encodedPassword);
        user.setPassword(null); // Clear transient field

        // Set timestamps if not already set
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(LocalDateTime.now());
        }
        if (user.getUpdatedAt() == null) {
            user.setUpdatedAt(LocalDateTime.now());
        }

        // Save user
        User savedUser = userRepository.save(user);
        log.info("User created successfully");

        // Create wallet
        try {
            walletService.createWalletForUser(savedUser);
        } catch (Exception e) {
            log.error("Failed to create wallet: {}", e.getMessage());
        }

        return savedUser;
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public List<User> findByRole(User.UserRole role) {
        return userRepository.findByRole(role);
    }

    public User updateUser(User user) {
        return userRepository.save(user);
    }

    public void deleteUser(UUID id) {
        userRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmailOrPhone(String input) {
        return userRepository.findByEmailOrPhone(input);
    }

    // Simple login method using plain text password
    public boolean authenticateUser(String username, String password) {
        Optional<User> user = findByUsername(username);
        if (user.isPresent()) {
            // First try simple password comparison
            if (user.get().getPassword() != null && user.get().getPassword().equals(password)) {
                return true;
            }
            // Fallback to BCrypt if using password_hash
            if (user.get().getPasswordHash() != null
                    && passwordEncoder.matches(password, user.get().getPasswordHash())) {
                return true;
            }
        }
        return false;
    }

    // Method to create user with simple password
    public User createUserWithSimplePassword(String username, String email, String password,
            String fullName, String phone, User.UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password); // Store as plain text
        user.setPasswordHash(passwordEncoder.encode(password)); // Also store hashed version
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setRole(role);

        return createUser(user);
    }

    public User updateUser(UUID id, User userDetails) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        // validate username/email/phone uniqueness if changed
        if (userDetails.getUsername() != null && !userDetails.getUsername().equals(existing.getUsername())
                && userRepository.existsByUsername(userDetails.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + userDetails.getUsername());
        }

         if (userDetails.getEmail() != null && !userDetails.getEmail().equals(existing.getEmail())) {
            if (userRepository.existsByEmail(userDetails.getEmail())) {
                throw new IllegalArgumentException("Email already exists");
            }
            existing.setEmail(userDetails.getEmail());
            // ✅ If email changed, require re-verification
            existing.setIsEmailVerified(false);
            existing.setEmailVerificationToken(UUID.randomUUID().toString());
            try {
                emailService.sendVerificationEmail(existing.getEmail(), existing.getEmailVerificationToken());
            } catch (Exception e) {
                log.warn("Failed to send verification email after email change");
            }
        }
        if (userDetails.getPhone() != null && !userDetails.getPhone().equals(existing.getPhone())
                && userRepository.existsByPhone(userDetails.getPhone())) {
            throw new IllegalArgumentException("Phone already exists: " + userDetails.getPhone());
        }

        // merge allowed fields
        if (userDetails.getUsername() != null)
            existing.setUsername(userDetails.getUsername());
        if (userDetails.getEmail() != null)
            existing.setEmail(userDetails.getEmail());
        if (userDetails.getFullName() != null)
            existing.setFullName(userDetails.getFullName());
        if (userDetails.getPhone() != null)
            existing.setPhone(userDetails.getPhone());
        if (userDetails.getRole() != null)
            existing.setRole(userDetails.getRole());

        // handle password update
        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            existing.setPassword(userDetails.getPassword());
            existing.setPasswordHash(passwordEncoder.encode(userDetails.getPassword()));
        }
        return userRepository.save(existing);

    }

    public void debugPrintUser(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            log.info("🔍 DEBUG User: {}", username);
            log.info("   - ID: {}", user.getId());
            log.info("   - Email: {}", user.getEmail());
            log.info("   - Role: {}", user.getRole());
            log.info("   - Password Hash Exists: {}", user.getPasswordHash() != null);
            log.info("   - Password Hash Length: {}",
                    user.getPasswordHash() != null ? user.getPasswordHash().length() : 0);
            log.info("   - Password Hash Starts With: {}",
                    user.getPasswordHash() != null ? user.getPasswordHash().substring(0, 7) : "null");
        } else {
            log.info("🔍 DEBUG User NOT found: {}", username);
        }
    }

}
