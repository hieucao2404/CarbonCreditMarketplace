package com.carboncredit.controller;

import com.carboncredit.dto.ApiResponse;
import com.carboncredit.dto.CarbonCreditDTO;
import com.carboncredit.dto.VerifyRequest;
import com.carboncredit.entity.CarbonCredit;
import com.carboncredit.entity.User;
import com.carboncredit.exception.ResourceNotFoundException;
import com.carboncredit.service.CarbonCreditService;
import com.carboncredit.service.UserService;
import com.carboncredit.util.DTOMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/credits")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CarbonCreditController {

  private final CarbonCreditService carbonCreditService;
  private final UserService userService;


  /**
   * Helper to get the authenticated User entity
   */

  private User getCurrentUser(Authentication authentication) {
    return userService.findByUsername(authentication.getName())
        .orElseThrow(() -> new ResourceNotFoundException("User", "username", authentication.getName()));
  }

  /**
   * (EV_OWNER) Get all credits for the authenticated user, paginated
   */
  @GetMapping("/my-credits")
  @PreAuthorize("hasRole('EV_OWNER')")
  public ResponseEntity<ApiResponse<Page<CarbonCreditDTO>>> getMyCredits(
      Authentication authentication,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    log.info("Fetching credits for user: {}", authentication.getName());
    try {
      User currentUser = getCurrentUser(authentication);
      Page<CarbonCreditDTO> dtoPage = carbonCreditService.findCreditsByUser(currentUser, page, size);
      return ResponseEntity.ok(ApiResponse.success(dtoPage));
    } catch (Exception e) {
      log.error("Error fetching user credits: ", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to fetch credits: " + e.getMessage()));
    }
  }

  /**
   * (EV_OWNER / ADMIN / CVA) get a single carbon credit by its ID
   */

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<CarbonCreditDTO>> getCreditById(@PathVariable UUID id,
      Authentication authentication) {
    log.info("Fetching credit by IDL {}", id);

    try {
      User currentUser = getCurrentUser(authentication);

      CarbonCreditDTO creditDTO = carbonCreditService.findCreditDtoById(id)
          .orElseThrow(() -> new ResourceNotFoundException("CarbonCredit not found"));

      // Security check
      if (currentUser.getRole() == User.UserRole.EV_OWNER
          && !Objects.equals(creditDTO.getOwnerId(), currentUser.getId())) {
        log.warn("User {} attemted to access credit {} without permission", currentUser.getUsername(), id);
        throw new AccessDeniedException("You do not have permission to view this carbon credit");
      }
      return ResponseEntity.ok(ApiResponse.success(creditDTO));
    } catch (ResourceNotFoundException e) {
      log.warn("Credit not found with ID: {}", id);
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
    } catch (AccessDeniedException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
    } catch (Exception e) {
      log.error("Error fetching credit {}: ", id, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("An unexpected error occurred: " + e.getMessage()));
    }
  }

  /**
   * (ADMIN / CVA) Get all credits in the system.paginated
   */
  @GetMapping("/admin/all")
  public ResponseEntity<ApiResponse<Page<CarbonCreditDTO>>> getAllCredits(@RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    log.info("Admin fetching all credits");
    try {
      Page<CarbonCreditDTO> dtoPage = carbonCreditService.findAllCredits(page, size);
      return ResponseEntity.ok(ApiResponse.success(dtoPage));
    } catch (Exception e) {
      log.error("Error fetching all credits: ", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to fetch all credits: " + e.getMessage()));
    }
  }

  /**
   * (ADMIN / CVA) Get credits by status, paginated
   */
  @GetMapping("/admin/by-status")
  @PreAuthorize("hasAnyRole('ADMIN', 'CVA')")
  public ResponseEntity<ApiResponse<Page<CarbonCreditDTO>>> getCreditByStatus(@RequestParam String status,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
    log.info("Admin fetching credits with status: {}", status);
    try {
      Page<CarbonCreditDTO> dtoPage = carbonCreditService.findCreditsByStatus(status, page, size);
      return ResponseEntity.ok(ApiResponse.success(dtoPage));
    } catch (IllegalArgumentException e) {
      log.error("Invalid status provied: {}", status);
      return ResponseEntity.badRequest().body(ApiResponse.error("Invalid status"));
    } catch (Exception e) {
      log.error("Error fetching credits by status: ", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to fetch credits: " + e.getMessage()));
    }
  }

}
