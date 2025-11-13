package com.carboncredit.repository;

import com.carboncredit.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificateRepository extends JpaRepository<Certificate, UUID> {
    List<Certificate> findByBuyerId(UUID buyerId);
    Optional<Certificate> findByIdAndBuyerId(UUID id, UUID buyerId);
}