package com.carboncredit.service;

import com.carboncredit.entity.User;
import com.carboncredit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public User createUser(User user) {
        log.info("Creating user: {}", user.getUsername());
        log.info("Password is null? {}", user.getPassword() == null);  // Debug
        
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + user.getUsername());
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + user.getEmail());
        }

        // Validate required fields
        if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
            throw new IllegalArgumentException("Full name is required");
        }

        if (user.getPhone() == null || user.getPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("Phone is required");
        }

        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        // Hash password
        String hashedPassword = passwordEncoder.encode(user.getPassword());
        user.setPasswordHash(hashedPassword);
        log.info("Password hashed successfully");

        //save user first
        User savedUser = userRepository.save(user);
        log.info("User saved with ID: {}", savedUser.getId());

        //Create a wallet for user
        walletService.createWalletForUser(savedUser);
        log.info("Wallet create for user {}: ", savedUser.getUsername());

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

        if (userDetails.getEmail() != null && !userDetails.getEmail().equals(existing.getEmail())
                && userRepository.existsByEmail(userDetails.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + userDetails.getEmail());
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

        //handle password update 
        if(userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
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
        log.info("   - Password Hash Length: {}", user.getPasswordHash() != null ? user.getPasswordHash().length() : 0);
        log.info("   - Password Hash Starts With: {}", user.getPasswordHash() != null ? user.getPasswordHash().substring(0, 7) : "null");
    } else {
        log.info("🔍 DEBUG User NOT found: {}", username);
    }
}


}
