package com.carboncredit.controller;

import com.carboncredit.dto.ApiResponse;
import com.carboncredit.dto.CertificateDTO; // You'll need to create this DTO
import com.carboncredit.entity.Certificate;
import com.carboncredit.entity.User;
import com.carboncredit.exception.ResourceNotFoundException;
import com.carboncredit.service.CertificateService;
import com.carboncredit.service.PdfGenerationService;
import com.carboncredit.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CertificateController {

    private final CertificateService certificateService;
    private final PdfGenerationService pdfGenerationService;
    private final UserService userService;

    private User getCurrentUser(Authentication auth) {
        return userService.findByUsername(auth.getName())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * (BUYER) Get all certificates for the logged-in user.
     */
    @GetMapping("/my-certificates")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<ApiResponse<List<CertificateDTO>>> getMyCertificates(Authentication auth) {
        User buyer = getCurrentUser(auth);
        List<Certificate> certificates = certificateService.getCertificatesForUser(buyer);
        
        // Convert to DTO
        List<CertificateDTO> dtoList = certificates.stream()
            .map(CertificateDTO::new) // Assumes CertificateDTO has a constructor
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(ApiResponse.success(dtoList));
    }

    /**
     * (BUYER) Download a specific certificate as a PDF.
     */
    @GetMapping("/{id}/download")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<byte[]> downloadCertificate(
            @PathVariable UUID id,
            Authentication auth) {
        
        User buyer = getCurrentUser(auth);
        Certificate certificate = certificateService.getCertificateByIdAndUser(id, buyer);
        
        byte[] pdfBytes = pdfGenerationService.generateCertificatePdf(certificate);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "certificate-" + certificate.getCertificateCode() + ".pdf");
        
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}