package com.carboncredit.controller;

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

@RestController
@RequestMapping("/carbon-credits")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CarbonCreditController {
    
    private final CarbonCreditService carbonCreditService;
    private final UserService userService;
    
    @GetMapping
    public ResponseEntity<List<CarbonCredit>> getAllCarbonCredits() {
        List<CarbonCredit> credits = carbonCreditService.findAvailableCredits();
        return ResponseEntity.ok(credits);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<CarbonCredit> getCarbonCreditById(@PathVariable UUID id) {
        return carbonCreditService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/pending")
    public ResponseEntity<List<CarbonCredit>> getPendingCredits() {
        List<CarbonCredit> credits = carbonCreditService.findPendingCredits();
        return ResponseEntity.ok(credits);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CarbonCredit>> getCreditsByUser(@PathVariable UUID userId) {
        return userService.findById(userId)
            .map(user -> {
                List<CarbonCredit> credits = carbonCreditService.findByUser(user);
                return ResponseEntity.ok(credits);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/{creditId}/verify")
    public ResponseEntity<CarbonCredit> verifyCredit(@PathVariable UUID creditId, @RequestBody(required = false) VerifyRequest request, Authentication authentication) {
        // In a real app, you'd get the user from the authentication
        // Implement user authentiation
        User verifier = userService.findByUsername(authentication.getName()).orElse(null);
        if(verifier == null || verifier.getRole() != User.UserRole.CVA) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            String comments = request == null ? null : request.getComments();
            CarbonCredit verified = carbonCreditService.verifyCarbonCredit(creditId, verifier, comments) ;
            return ResponseEntity.ok(verified);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{creditId}/reject")
    public ResponseEntity<CarbonCredit> rejectCredit(@PathVariable UUID creditId, @RequestBody(required = false) VerifyRequest request, Authentication authentication) {
        User verifier = userService.findByUsername(authentication.getName()).orElse(null);
        if (verifier == null || verifier.getRole() != User.UserRole.CVA) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            String comments = request == null ? null : request.getComments();
            CarbonCredit rejected = carbonCreditService.rejectCarbonCredit(creditId, verifier, comments);
            return ResponseEntity.ok(rejected);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
