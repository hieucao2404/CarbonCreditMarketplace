package com.carboncredit.controller;

import com.carboncredit.dto.CarbonCreditDTO;
import com.carboncredit.dto.VerifyRequest;
import com.carboncredit.entity.CarbonCredit;
import com.carboncredit.entity.User;
import com.carboncredit.service.CarbonCreditService;
import com.carboncredit.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/carbon-credits")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CarbonCreditController {

    private final CarbonCreditService carbonCreditService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<CarbonCreditDTO>> getAllCarbonCredits() {
        List<CarbonCredit> credits = carbonCreditService.findAvailableCredits();
        List<CarbonCreditDTO> dtos = credits.stream()
                .map(CarbonCreditDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CarbonCreditDTO> getCarbonCreditById(@PathVariable UUID id) {
        return carbonCreditService.findById(id)
                .map(CarbonCreditDTO::new)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/pending")
    public ResponseEntity<List<CarbonCreditDTO>> getPendingCredits() {
        List<CarbonCredit> credits = carbonCreditService.findPendingCredits();
        List<CarbonCreditDTO> dtos = credits.stream()
                .map(CarbonCreditDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // FIX: Change to return DTO instead of entity
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CarbonCreditDTO>> getCreditsByUser(@PathVariable UUID userId) {
        return userService.findById(userId)
                .map(user -> {
                    List<CarbonCredit> credits = carbonCreditService.findByUser(user);
                    List<CarbonCreditDTO> dtos = credits.stream()
                            .map(CarbonCreditDTO::new)
                            .collect(Collectors.toList());
                    return ResponseEntity.ok(dtos);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // FIX: Change to return DTO instead of entity
    @PostMapping("/{creditId}/verify")
    public ResponseEntity<CarbonCreditDTO> verifyCredit(@PathVariable UUID creditId,
            @RequestBody(required = false) VerifyRequest request, Authentication authentication) {
        User verifier = userService.findByUsername(authentication.getName()).orElse(null);
        if (verifier == null || verifier.getRole() != User.UserRole.CVA) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            String comments = request == null ? null : request.getComments();
            CarbonCredit verified = carbonCreditService.verifyCarbonCredit(creditId, verifier, comments);
            return ResponseEntity.ok(new CarbonCreditDTO(verified)); // Convert to DTO
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // FIX: Change to return DTO instead of entity
    @PostMapping("/{creditId}/reject")
    public ResponseEntity<CarbonCreditDTO> rejectCredit(@PathVariable UUID creditId,
            @RequestBody(required = false) VerifyRequest request, Authentication authentication) {
        User verifier = userService.findByUsername(authentication.getName()).orElse(null);
        if (verifier == null || verifier.getRole() != User.UserRole.CVA) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            String comments = request == null ? null : request.getComments();
            CarbonCredit rejected = carbonCreditService.rejectCarbonCredit(creditId, verifier, comments);
            return ResponseEntity.ok(new CarbonCreditDTO(rejected)); // Convert to DTO
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
