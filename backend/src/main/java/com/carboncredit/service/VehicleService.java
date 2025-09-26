package com.carboncredit.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carboncredit.entity.Vehicle;
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

    public Vehicle createVehicle(Vehicle vehicle) {
        if (vehicleRepository.existsByVin(vehicle.getVin())) {
            throw new RuntimeException("Vehicle with VIN already exists");
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
}
