package com.carboncredit.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carboncredit.entity.Vehicle;
import com.carboncredit.entity.User;
import com.carboncredit.repository.VehicleRepository;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Transactional
public class VehicleService {
    private final VehicleRepository vehicleRepository;
    private final UserService userService;

    public Vehicle createVehicle(Vehicle vehicle) {
        if (vehicleRepository.existsByVin(vehicle.getVin())) {
            throw new IllegalArgumentException("Vehicle with VIN already exists: " + vehicle.getVin());
        }
        
        UUID userId = vehicle.getUser().getId();
        User user = userService.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if(user.getRole() != User.UserRole.EV_OWNER) {
            throw new IllegalArgumentException("Associated EVOwner only");
        }
        // Validate required field
        if (vehicle.getUser() == null) {
            throw new IllegalArgumentException("Vehicle must be associated with a user");
        }
        
        return vehicleRepository.save(vehicle);
    }

    @Transactional(readOnly = true)
    public Optional<Vehicle> findById(UUID id) {
        return vehicleRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Vehicle> findByVin(String vin) {
        return vehicleRepository.findByVin(vin);
    }

    @Transactional(readOnly = true)
    public List<Vehicle> findByModel(String model) {
        return vehicleRepository.findByModel(model);
    }

    public Vehicle updateVehicle(Vehicle vehicle) {
        return vehicleRepository.save(vehicle);
    }

    public void deleteVehicle(UUID id) {
        vehicleRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public List<Vehicle> findByUser(User user) {
        return vehicleRepository.findByUser(user);
    }
    
    @Transactional(readOnly = true)
    public List<Vehicle> findByUserId(UUID userId) {
        // This would require the user to be fetched, but for now we'll keep it simple
        return vehicleRepository.findAll().stream()
            .filter(vehicle -> vehicle.getUser().getId().equals(userId))
            .toList();
    }
}
