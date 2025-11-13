package com.carboncredit.service;



import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carboncredit.entity.Certificate;
import com.carboncredit.entity.Transaction;
import com.carboncredit.entity.User;
import com.carboncredit.exception.ResourceNotFoundException;
import com.carboncredit.repository.CertificateRepository;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateRepository certificateRepository;

    /**
     * Called by TransactionService after a purchase is completed
     */
    @Transactional
    public Certificate generateCertificate(Transaction transaction) {
        
        log.info("Generating certificate for transaction {}", transaction.getId());

        Certificate certificate = new Certificate();
        certificate.setTransaction(transaction);
        certificate.setBuyer(transaction.getBuyer());
        certificate.setCredit(transaction.getCredit());
        certificate.setIssueDate(LocalDateTime.now());
        certificate.setCo2ReducedKg(transaction.getCredit().getCo2ReducedKg());

        // Generate a unique, human-readable certificate code
        String code = "CCM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        certificate.setCertificateCode(code);

        Certificate savedCertificate = certificateRepository.save(certificate);
        log.info("Certificate {} created with code {}", savedCertificate.getId(), code);
        return savedCertificate;
    }

    /**
     * Get all certificates for a specific buyer
     */
    @Transactional(readOnly = true)
    public List<Certificate> getCertificatesForUser(User buyer){
        return certificateRepository.findByBuyerId(buyer.getId());
    }

    /**
     * Get a singgle certificate, ensuring the user owns it
     */
    @Transactional(readOnly = true)
    public Certificate getCertificateByIdAndUser(UUID id, User buyer) {
        return certificateRepository.findByIdAndBuyerId(id, buyer.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Certificate not found or you do not have permission."));
    }
}
