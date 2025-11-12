package com.carboncredit.repository;

import com.carboncredit.entity.InspectionAppointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // <-- 1. ADD THIS IMPORT
import org.springframework.data.repository.query.Param; // <-- 2. ADD THIS IMPORT
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InspectionAppointmentRepository extends JpaRepository<InspectionAppointment, UUID> {
    
    // Find by the 'id' field OF the 'journey' object
    Optional<InspectionAppointment> findByJourney_Id(UUID journeyId);

    // Find by the 'id' field OF the 'evOwner' object
    List<InspectionAppointment> findByEvOwner_Id(UUID evOwnerId);

    // --- THIS IS THE NEW, DIFFERENT APPROACH ---
    // We are replacing the magic 'findByCva_Id' with a direct query.
    @Query("SELECT ia FROM InspectionAppointment ia WHERE ia.cva.id = :cvaId")
    List<InspectionAppointment> findAppointmentsByCvaId(@Param("cvaId") UUID cvaId);
}