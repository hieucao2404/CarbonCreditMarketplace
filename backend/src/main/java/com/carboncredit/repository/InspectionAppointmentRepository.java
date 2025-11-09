package com.carboncredit.repository;

import com.carboncredit.entity.InspectionAppointment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InspectionAppointmentRepository extends JpaRepository<InspectionAppointment, UUID> {
    Optional<InspectionAppointment> findByJourneyId(UUID journeyId);
    List<InspectionAppointment> findByEvOwnerId(UUID evOwnerId);
    List<InspectionAppointment> findByCvaId(UUID cvaId);
}