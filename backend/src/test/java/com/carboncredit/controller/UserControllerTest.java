package com.carboncredit.controller;

import com.carboncredit.entity.User;
import com.carboncredit.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for UserController
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("UserController Integration Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private UUID testUserId;
    private User adminUser;
    private User regularUser;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        
        // Setup test user (EV_OWNER)
        testUser = new User();
        testUser.setId(testUserId);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");
        testUser.setPhone("0123456789");
        testUser.setRole(User.UserRole.EV_OWNER);
        testUser.setCreatedAt(LocalDateTime.now());
        
        // Setup admin user
        adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setFullName("Admin User");
        adminUser.setPhone("0987654321");
        adminUser.setRole(User.UserRole.ADMIN);
        adminUser.setCreatedAt(LocalDateTime.now());
        
        // Setup regular user (non-admin)
        regularUser = new User();
        regularUser.setId(UUID.randomUUID());
        regularUser.setUsername("user");
        regularUser.setEmail("user@example.com");
        regularUser.setFullName("Regular User");
        regularUser.setPhone("1234567890");
        regularUser.setRole(User.UserRole.EV_OWNER);
        regularUser.setCreatedAt(LocalDateTime.now());
    }

    // ==================== REGISTRATION TESTS ====================

    @Test
    @WithMockUser
    @DisplayName("Should register new user successfully")
    void registerUser_Success() throws Exception {
        // Given
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("new@example.com");
        newUser.setPassword("password123");
        newUser.setFullName("New User");
        newUser.setPhone("0987654321");
        newUser.setRole(User.UserRole.BUYER);

        when(userService.createUser(any(User.class))).thenReturn(testUser);

        // When & Then
        mockMvc.perform(post("/api/users/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.username").value("testuser"));

        verify(userService, times(1)).createUser(any(User.class));
    }

    // ==================== AUTHENTICATED USER TESTS ====================

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("Should get current user profile")
    void getCurrentUser_Success() throws Exception {
        // Given
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "EV_OWNER")
    @DisplayName("Should update own profile")
    void updateUser_Self_Success() throws Exception {
        // Given
        User updateRequest = new User();
        updateRequest.setFullName("Updated Name");
        updateRequest.setPhone("9999999999");
        
        testUser.setFullName("Updated Name");
        when(userService.updateUser(eq(testUserId), any(User.class))).thenReturn(testUser);

        // When & Then
        mockMvc.perform(put("/api/users/{id}", testUserId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Updated Name"));
    }

    // ==================== ADMIN TESTS ====================

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("Should get all users as admin")
    void getAllUsers_AsAdmin_Success() throws Exception {
        // Given
        User user2 = new User();
        user2.setId(UUID.randomUUID());
        user2.setUsername("buyer1");
        user2.setRole(User.UserRole.BUYER);
        user2.setEmail("buyer@example.com");
        user2.setFullName("Buyer User");

        // Mock the current user lookup
        when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userService.getAllUsers()).thenReturn(Arrays.asList(testUser, user2));

        // When & Then
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].username").exists());
    }

    @Test
    @WithMockUser(username = "user", roles = "EV_OWNER")
    @DisplayName("Should deny access to get all users for non-admin")
    void getAllUsers_AsNonAdmin_Forbidden() throws Exception {
        // Given - Mock regular user lookup
        when(userService.findByUsername("user")).thenReturn(Optional.of(regularUser));

        // When & Then
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Only admins can access all users"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("Should get user by ID as admin")
    void getUserById_AsAdmin_Success() throws Exception {
        // Given
        when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userService.findById(testUserId)).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(get("/api/users/{id}", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testUserId.toString()))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("Should return 404 when user not found")
    void getUserById_NotFound() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userService.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/users/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User not found with id: " + nonExistentId));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("Should delete user as admin")
    void deleteUser_AsAdmin_Success() throws Exception {
        // Given
        when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userService.findById(testUserId)).thenReturn(Optional.of(testUser));
        doNothing().when(userService).deleteUser(testUserId);

        // When & Then
        mockMvc.perform(delete("/api/users/{id}", testUserId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User deleted successfully"));

        verify(userService, times(1)).deleteUser(testUserId);
    }
}
