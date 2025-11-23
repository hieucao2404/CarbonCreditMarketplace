package com.carboncredit.controller;

import com.carboncredit.dto.ApiResponse;
import com.carboncredit.dto.LoginRequest;
import com.carboncredit.dto.LoginResponse;
import com.carboncredit.dto.RegisterRequest;
import com.carboncredit.dto.UserDTO;
import com.carboncredit.entity.User;
import com.carboncredit.exception.ResourceNotFoundException;
import com.carboncredit.service.JwtService;
import com.carboncredit.service.UserService;
import com.carboncredit.util.DTOMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for User Management
 * Handles user registration, profile management, and admin operations
 * 
 * Base URL: /api/users
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    private final UserService userService;

    // ==================== PUBLIC ENDPOINTS ====================

    /**
     * Login endpoint
     * POST /api/users/login
     * 
     * @param loginRequest username and password
     * @return JWT token and user info
     */

    @PostMapping("/login")
     public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login attempt for user: {}", loginRequest.getUsername());

        try {
            //Authenticate
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            //load user details
            User user = userService.findByUsername(loginRequest.getUsername()).orElseThrow(() -> new ResourceNotFoundException("User not found"));

            // ✅ CHECK EMAIL VERIFICATION
            if (user.getIsEmailVerified() == null || !user.getIsEmailVerified()) {
                log.warn("❌ Login blocked - Email not verified for user: {}", user.getUsername());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.<LoginResponse>builder()
                        .success(false)
                        .message("Email chưa được xác thực. Vui lòng kiểm tra email và xác thực tài khoản trước khi đăng nhập.")
                        .build());
            }

            //generate JWT token
            String jwtToken = jwtService.generateToken(user);

            //Build response
             LoginResponse response = LoginResponse.builder()
                .token(jwtToken)
                .tokenType("Bearer")
                .user(DTOMapper.toUserDTO(user))
                .expiresIn(jwtService.getExpirationTime())
                .build();
        
        log.info("✅ Login successful for verified user: {}", loginRequest.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
        
        } catch (Exception e) {
            log.error("Login failed for user {}: {}", loginRequest.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.<LoginResponse>builder().success(false).message("Invalid username or password").build());
        }
     }

    /**
     * Register new user (Public)
     * POST /api/users/register
     * 
     * @param user User registration data
     * @return Created user with 201 status
     * 
     *         Request Body Example:
     *         {
     *         "username": "evowner1",
     *         "email": "evowner1@example.com",
     *         "password": "password123",
     *         "fullName": "John Doe",
     *         "phone": "0123456789",
     *         "role": "EV_OWNER"
     *         }
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDTO>> registerUser(@Valid @RequestBody RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());
        
        try {
            // Create User entity from request
            // User user = new User();
            // user.setUsername(request.getUsername());
            // user.setEmail(request.getEmail());
            // user.setPassword(request.getPassword());
            // user.setFullName(request.getFullName());
            // user.setPhone(request.getPhone());
            // user.setRole(User.UserRole.valueOf(request.getRole()));
            // user.setCreatedAt(LocalDateTime.now());
            // user.setUpdatedAt(LocalDateTime.now());
            
            
            User createdUser = userService.createUser(request);
            UserDTO userDTO = DTOMapper.toUserDTO(createdUser);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("User registered successfully", userDTO));
        } catch (IllegalArgumentException e) {
            log.error("Registration error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<UserDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Unexpected registration error: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<UserDTO>builder()
                            .success(false)
                            .message("Registration failed: " + e.getMessage())
                            .build());
        }
    }

    // ==================== AUTHENTICATED USER ENDPOINTS ====================

    /**
     * Get current authenticated user profile
     * GET /api/users/me
     * 
     * @param authentication Current authenticated user
     * @return User profile data
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUser(Authentication authentication) {
        log.info("Fetching current user profile: {}", authentication.getName());

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserDTO userDTO = DTOMapper.toUserDTO(user);
        return ResponseEntity.ok(ApiResponse.success(userDTO));
    }

    /**
     * Update user profile
     * PUT /api/users/{id}
     * 
     * Users can update their own profile
     * Admins can update any user profile
     * 
     * @param id          User ID to update
     * @param userDetails Updated user data
     * @return Updated user data
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<ApiResponse<UserDTO>> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody User userDetails,
            Authentication authentication) {

        log.info("User {} updating profile: {}", authentication.getName(), id);

        User updatedUser = userService.updateUser(id, userDetails);
        UserDTO userDTO = DTOMapper.toUserDTO(updatedUser);

        return ResponseEntity.ok(ApiResponse.success("User updated successfully", userDTO));
    }

    // ==================== USER QUERY ENDPOINTS ====================

    /**
     * Get user by ID
     * GET /api/users/{id}
     * 
     * Accessible by:
     * - Admin (any user)
     * - CVA (any user for verification)
     * - Self (own profile)
     * 
     * @param id User ID
     * @return User profile data
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CVA') or #id == authentication.principal.id")
    public ResponseEntity<ApiResponse<UserDTO>> getUserById(@PathVariable UUID id, Authentication authentication) {
        log.info("Fetching user with ID: {}", id);

        // manual authorization check
        User currentUser = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isAdmin = currentUser.getRole() == User.UserRole.ADMIN;
        boolean isCVA = currentUser.getRole() == User.UserRole.CVA;
        boolean isSelf = currentUser.getId().equals(id);

        if (!isAdmin && !isCVA && !isSelf) {
            log.warn("User {} attempted to access user {} without permission", currentUser.getUsername(), id);

            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.<UserDTO>builder().success(false)
                    .message("You do not have permission to access this user").build());
        }
        // return 404 if user nout found
        User user = userService.findById(id).orElse(null);
        if (user == null) {
            log.warn("User not found with ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.<UserDTO>builder().success(false).message("User not found with id: " + id).build());
        }

        UserDTO userDTO = DTOMapper.toUserDTO(user);
        return ResponseEntity.ok(ApiResponse.success(userDTO));

    }

    /**
     * Get user by username
     * GET /api/users/username/{username}
     * 
     * Accessible by authenticated users
     * 
     * @param username Username to search
     * @return User profile data
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<ApiResponse<UserDTO>> getUserByUsername(@PathVariable String username) {
        log.info("Fetching user by username: {}", username);

        User user = userService.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        UserDTO userDTO = DTOMapper.toUserDTO(user);
        return ResponseEntity.ok(ApiResponse.success(userDTO));
    }

    // ==================== ADMIN ONLY ENDPOINTS ====================

    /**
     * Get all users (Admin only)
     * GET /api/users
     * 
     * @return List of all users
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getAllUsers(Authentication authentication) {
        log.info("Admin fetching all users");
        // Manual authorization check for admin
        User currentUser = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (currentUser.getRole() != User.UserRole.ADMIN) {
            log.warn("Non-admin user {} attempted to access all users", currentUser.getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.<List<UserDTO>>builder()
                            .success(false)
                            .message("Only admins can access all users")
                            .build());
        }
        List<UserDTO> users = userService.getAllUsers().stream()
                .map(DTOMapper::toUserDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(users));
    }

    /**
     * Get users by role (Admin only)
     * GET /api/users/role/{role}
     * 
     * Valid roles: EV_OWNER, BUYER, CVA, ADMIN
     * 
     * @param role User role
     * @return List of users with specified role
     */
    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getUsersByRole(@PathVariable String role,
            Authentication authentication) {
        log.info("Admin fetching users with role: {}", role);

        // Manual authorization check
        User currentUser = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (currentUser.getRole() != User.UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.<List<UserDTO>>builder().success(false)
                    .message("Only admins can fileter users by role").build());
        }

        try {
            User.UserRole userRole = User.UserRole.valueOf(role.toUpperCase());
            List<User> users = userService.findByRole(userRole);
            List<UserDTO> userDTOs = DTOMapper.toUserDTOList(users);

            return ResponseEntity.ok(ApiResponse.success(userDTOs));
        } catch (IllegalArgumentException e) {
            log.error("Invalid role: {}", role);
            throw new IllegalArgumentException("Invalid role: " + role + ". Valid roles: EV_OWNER, BUYER, CVA, ADMIN");
        }
    }

    /**
     * Delete user (Admin only)
     * DELETE /api/users/{id}
     * 
     * @param id User ID to delete
     * @return No content (204)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id, Authentication authentication) {
        log.info("Admin deleting user: {}", id);
        // Manual authorization check
        User currentUser = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (currentUser.getRole() != User.UserRole.ADMIN) {
            log.warn("Non-admin user {} attempted to delete user {}", currentUser.getUsername(), id);

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.<Void>builder().success(false).message("Only admins can delete users").build());

        }
        // Verify user exists
        userService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        userService.deleteUser(id);

        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }

    @GetMapping("/debug/{username}")
    public ResponseEntity<String> debugUser(@PathVariable String username) {
        userService.debugPrintUser(username);
        return ResponseEntity.ok("Check logs");
    }

}
