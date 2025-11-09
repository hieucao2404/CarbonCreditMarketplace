package com.carboncredit.repository;

import com.carboncredit.entity.VerificationStation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface VerificationStationRepository extends JpaRepository<VerificationStation, UUID> {
    List<VerificationStation> findByIsActive(boolean isActive);
}