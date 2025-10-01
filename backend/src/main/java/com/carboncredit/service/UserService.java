package com.carboncredit.service;

import com.carboncredit.entity.User;
import com.carboncredit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User createUser(User user) {
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
        
        // Validate phone if provided
        if (user.getPhone() != null && userRepository.existsByPhone(user.getPhone())) {
            throw new IllegalArgumentException("Phone number already exists: " + user.getPhone());
        }

        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        return userRepository.save(user);
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
            if (user.get().getPasswordHash() != null && passwordEncoder.matches(password, user.get().getPasswordHash())) {
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

}
