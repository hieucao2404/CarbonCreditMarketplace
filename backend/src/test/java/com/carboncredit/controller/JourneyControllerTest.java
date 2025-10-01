package com.carboncredit.controller;

import com.carboncredit.entity.JourneyData;
import com.carboncredit.entity.User;
import com.carboncredit.service.JourneyDataService;
import com.carboncredit.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JourneyControllerTest {

    @Mock private JourneyDataService journeyDataService;
    @Mock private UserService userService;
    @Mock private Authentication authentication;

    private JourneyController journeyController;
    private User testUser;
    private JourneyData testJourney;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        journeyController = new JourneyController(journeyDataService, userService);

        // Create test user
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setRole(User.UserRole.EV_OWNER);

        // Create test journey
        testJourney = new JourneyData();
        testJourney.setId(UUID.randomUUID());
        testJourney.setUser(testUser);
        testJourney.setDistanceKm(new BigDecimal("50"));
        testJourney.setEnergyConsumedKwh(new BigDecimal("10"));
        testJourney.setCo2ReducedKg(new BigDecimal("5.5"));
    }

    @Test
    void createJourney_shouldReturnCreated_whenValidJourney() {
        // Given
        when(authentication.getName()).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(journeyDataService.createJourney(any(JourneyData.class))).thenReturn(testJourney);

        // When
        ResponseEntity<JourneyData> response = journeyController.createJourney(testJourney, authentication);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        JourneyData responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(testJourney.getId(), responseBody.getId());
        verify(journeyDataService).createJourney(any(JourneyData.class));
    }

    @Test
    void createJourney_shouldReturnBadRequest_whenUserNotFound() {
        // Given
        when(authentication.getName()).thenReturn("nonexistent");
        when(userService.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When
        ResponseEntity<JourneyData> response = journeyController.createJourney(testJourney, authentication);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(journeyDataService, never()).createJourney(any());
    }

    @Test
    void getJourney_shouldReturnJourney_whenOwnerRequests() {
        // Given
        UUID journeyId = testJourney.getId();
        when(authentication.getName()).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(journeyDataService.findById(journeyId)).thenReturn(testJourney);

        // When
        ResponseEntity<JourneyData> response = journeyController.getJourney(journeyId, authentication);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JourneyData responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(journeyId, responseBody.getId());
    }

    @Test
    void getJourney_shouldReturnForbidden_whenNonOwnerRequests() {
        // Given
        UUID journeyId = testJourney.getId();
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        otherUser.setUsername("otheruser");

        when(authentication.getName()).thenReturn("otheruser");
        when(userService.findByUsername("otheruser")).thenReturn(Optional.of(otherUser));
        when(journeyDataService.findById(journeyId)).thenReturn(testJourney);

        // When
        ResponseEntity<JourneyData> response = journeyController.getJourney(journeyId, authentication);

        // Then
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void getJourney_shouldReturnNotFound_whenJourneyDoesNotExist() {
        // Given
        UUID journeyId = UUID.randomUUID();
        when(authentication.getName()).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(journeyDataService.findById(journeyId)).thenReturn(null);

        // When
        ResponseEntity<JourneyData> response = journeyController.getJourney(journeyId, authentication);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}