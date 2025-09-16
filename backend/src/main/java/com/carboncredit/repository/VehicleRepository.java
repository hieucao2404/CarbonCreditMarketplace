package com.carboncredit.repository;

import com.carboncredit.entity.Vehicle;
import com.carboncredit.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    
    List<Vehicle> findByUser(User user);
    
    Optional<Vehicle> findByVin(String vin);
    
    boolean existsByVin(String vin);
    
    List<Vehicle> findByModel(String model);
}
