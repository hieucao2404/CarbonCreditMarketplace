package com.carboncredit.repository;

import com.carboncredit.entity.JourneyData;
import com.carboncredit.entity.User;
import com.carboncredit.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface JourneyDataRepository extends JpaRepository<JourneyData, UUID> {
    
    List<JourneyData> findByUser(User user);
    
    List<JourneyData> findByVehicle(Vehicle vehicle);
    
    List<JourneyData> findByUserAndStartTimeBetween(User user, LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT SUM(j.co2ReducedKg) FROM JourneyData j WHERE j.user = :user")
    BigDecimal getTotalCo2ReductionByUser(User user);
    
    @Query("SELECT j FROM JourneyData j WHERE j.carbonCredit IS NULL ORDER BY j.createdAt DESC")
    List<JourneyData> findJourneysWithoutCredits();
}
