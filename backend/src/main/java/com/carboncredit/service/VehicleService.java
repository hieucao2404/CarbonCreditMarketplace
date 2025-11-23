package com.carboncredit.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carboncredit.entity.Vehicle;
import com.carboncredit.exception.ResourceNotFoundException;
import com.carboncredit.exception.UnauthorizedOperationException;
import com.carboncredit.dto.VehicleDTO;
import com.carboncredit.entity.User;
import com.carboncredit.repository.VehicleRepository;
import com.carboncredit.util.DTOMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VehicleService {
    private final VehicleRepository vehicleRepository;
    private final UserService userService;

  /**
   * Create a new vehicle and returns its DTO
   */
  public Vehicle createVehicle(Vehicle vehicle, User user){
    log.info("Creating vehicle(VIN: {}) for user {}", vehicle.getVin(), user.getUsername());

    // validate role
     if (user.getRole() != User.UserRole.EV_OWNER) {
        throw new UnauthorizedOperationException("Only users with role EV_OWNER can register vehicles");
    }

    // validate VIN uniqueness
    if(vehicleRepository.existsByVin(vehicle.getVin())){
        throw new IllegalArgumentException("Vehicle with VIN already exists: " + vehicle.getVin());
    }
    //Set owner ship and save
    vehicle.setUser(user);
    Vehicle savedVehicle = vehicleRepository.save(vehicle);
    log.info("Vehicle created with ID: {}", savedVehicle.getId());
    
    return savedVehicle;
  }

    @Transactional(readOnly = true)
    public Vehicle findById(UUID id) {
        return vehicleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Vehicle", "id", id));
    }

    @Transactional(readOnly = true)
    public Optional<Vehicle> findByVin(String vin) {
        return vehicleRepository.findByVin(vin);
    }

    @Transactional(readOnly = true)
    public List<Vehicle> findByModel(String model) {
        return vehicleRepository.findByModel(model);
    }

    public Vehicle updateVehicle(UUID vehicleId, Vehicle vehicleDetails, User user) {
        log.info("Updating vehicle {} for user {}", vehicleId, user.getUsername());

        Vehicle existingVehicle = findById(vehicleId);

        // Check onwnership
        if (!existingVehicle.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedOperationException(user.getId().toString(), "vehicle", vehicleId.toString(), "update");
        }
        
        // 2. Check for VIN change and uniqueness (VIN is typically immutable, but if allowed)
        if (vehicleDetails.getVin() != null && !vehicleDetails.getVin().equals(existingVehicle.getVin())) {
             if (vehicleRepository.existsByVin(vehicleDetails.getVin())) {
                throw new IllegalArgumentException("Vehicle with new VIN already exists: " + vehicleDetails.getVin());
            }
            existingVehicle.setVin(vehicleDetails.getVin());
        }

        //update mutabble fileds
        if (vehicleDetails.getModel() != null) {
            existingVehicle.setModel(vehicleDetails.getModel());
        }
        if (vehicleDetails.getRegistrationDate() != null) {
            existingVehicle.setRegistrationDate(vehicleDetails.getRegistrationDate());
        }

        Vehicle updatedVehicle = vehicleRepository.save(existingVehicle);
        log.info("Vehicle {} updated successfully", vehicleId);

        return updatedVehicle;
    }

    public void deleteVehicle(UUID vehicleId, User user) {
        log.info("Attempting to delete vehicle {} by user {}", vehicleId, user.getUsername());
        
        Vehicle vehicle = findById(vehicleId);

        // 1. Check Ownership
        if (!vehicle.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedOperationException(user.getId().toString(), "vehicle", vehicleId.toString(), "delete");
        }

        // 2. Add business logic check (e.g., cannot delete if has verified journeys)
        // (This check is optional but recommended)
        
        // 3. Delete
        vehicleRepository.delete(vehicle);
        log.info("Vehicle {} deleted successfully", vehicleId);
    }

    public void deleteVehicleAdmin(UUID vehicleId) {
        log.info("Attempting to delete vehicle {} by admin", vehicleId);
        
        Vehicle vehicle = findById(vehicleId);
        vehicleRepository.delete(vehicle);
        log.info("Vehicle {} deleted successfully by admin", vehicleId);
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
