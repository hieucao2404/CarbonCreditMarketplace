package com.carboncredit.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.carboncredit.dto.ApiResponse;
import com.carboncredit.dto.CreateVehicleRequest;
import com.carboncredit.dto.UpdateVehicleRequest;
import com.carboncredit.dto.VehicleDTO;
import com.carboncredit.entity.User;
import com.carboncredit.entity.Vehicle;
import com.carboncredit.exception.ResourceNotFoundException;
import com.carboncredit.exception.UnauthorizedOperationException;
import com.carboncredit.service.UserService;
import com.carboncredit.service.VehicleService;
import com.carboncredit.util.DTOMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;

@Slf4j
@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "*")
public class VehicleController {
    private final VehicleService vehicleService;
    private final UserService userService;

    /**
     * Helper to get the authenticated User entity
     */
    private User getCurrentUser(Authentication authentication) {
        return userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", authentication.getName()));
    }

    /**
     * (EV_ONWER) Register a new vehicle
     */
    @PostMapping
    @PreAuthorize("hasRole('EV_OWNER')")
    public ResponseEntity<ApiResponse<VehicleDTO>> createVehicle(@Valid @RequestBody CreateVehicleRequest request,
            Authentication authentication) {
        // TODO: process POST request
        log.info("Received request to create vehicle with VIN: {}", request.getVin());
        try {
            User currentUser = getCurrentUser(authentication);

            Vehicle newVehicle = new Vehicle();
            newVehicle.setVin(request.getVin());
            newVehicle.setModel(request.getModel());
            newVehicle.setRegistrationDate(request.getRegistrationDate());

            Vehicle savedVehicle = vehicleService.createVehicle(newVehicle, currentUser);

            VehicleDTO vehicleDTO = DTOMapper.toVehicleDTO(savedVehicle);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Vehivle registered successfully", vehicleDTO));
        } catch (IllegalArgumentException | UnauthorizedOperationException e) {
            log.warn("Failed to create vehicle: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Internal; error creating vehicle: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred: " + e.getMessage()));
        }

    }

    /**
     * (EV_OWNER) get all vehicles for the authenticated user
     */
    @GetMapping("/my-vehicles")
    @PreAuthorize("hasRole('EV_OWNER')")
    public ResponseEntity<ApiResponse<List<VehicleDTO>>> getMyVehicles(Authentication authentication) {
        log.info("Fetching vehicles for user: {}", authentication.getName());

        try {
            User currentUser = getCurrentUser(authentication);
            List<Vehicle> vehicles = vehicleService.findByUser(currentUser);

            // Convert list to DTO list using mapper
            List<VehicleDTO> vehicleDTOs = DTOMapper.toVehicleDTOList(vehicles);

            return ResponseEntity.ok(ApiResponse.success(vehicleDTOs));
        } catch (Exception e) {
            log.warn("Error fetching user vehicles: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch vehicles: " + e.getMessage()));
        }
    }

    /**
     * (EV_OWNER / ADMIN / CVA) get a single vehicles by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EV_OWNER', 'ADMIN', 'CVA')")
    public ResponseEntity<ApiResponse<VehicleDTO>> getVeicleById(@PathVariable UUID id, Authentication authentication) {
        log.info("Fetching vehicle by ID: {}", id);
        try {
            User currentUser = getCurrentUser(authentication);
            Vehicle vehicle = vehicleService.findById(id);

            // Security check: If the user is an EV_OWNER, they must own this vehicle
            if (currentUser.getRole() == User.UserRole.EV_OWNER
                    && !vehicle.getUser().getId().equals(currentUser.getId())) {
                log.warn("User {} attempted to access vehicle {} without permission", currentUser.getUsername(), id);

                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("You do not have permission to view this vehicle"));
            }

            VehicleDTO vehicleDTO = DTOMapper.toVehicleDTO(vehicle);
            return ResponseEntity.ok(ApiResponse.success(vehicleDTO));
        } catch (ResourceNotFoundException e) {
            log.warn("Vehicle not found with ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching vehicle {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occured: " + e.getMessage()));
        }
    }

    /**
     * (EV_OWNER)  update an existing vehicle's details
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('EV_OWNER')")
    public ResponseEntity<ApiResponse<VehicleDTO>> updateVehicle(@PathVariable UUID id,@Valid @RequestBody UpdateVehicleRequest request, Authentication authentication) {
        log.info("Received request to update vehicle: {}", id);
        try {
            User currentUser = getCurrentUser(authentication);

          Vehicle vehicleDetails = new Vehicle();
            vehicleDetails.setModel(request.getModel());
            vehicleDetails.setRegistrationDate(request.getRegistrationDate());

            Vehicle updatedVehicle = vehicleService.updateVehicle(id, vehicleDetails, currentUser);
            VehicleDTO vehicleDTO = DTOMapper.toVehicleDTO(updatedVehicle);

            return ResponseEntity.ok(ApiResponse.success("Vehicle updated successfully", vehicleDTO));
        } catch (ResourceNotFoundException e) {
            log.warn("Failed to update vehicle, not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException | UnauthorizedOperationException e) {
            log.warn("Failed to update vehicle {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Internal error updating vehicle {}: ", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred: " + e.getMessage()));
        }
    }

    /**
     * (EV_OWNER) delete a vehicle
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('EV_OWNER')")
    public ResponseEntity<ApiResponse<Void>> deleteVehicle(
            @PathVariable UUID id,
            Authentication authentication) {
        
        log.info("Received request to delete vehicle: {}", id);
        try {
            User currentUser = getCurrentUser(authentication);
            vehicleService.deleteVehicle(id, currentUser);
            return ResponseEntity.ok(ApiResponse.success("Vehicle deleted successfully", null));
        } catch (ResourceNotFoundException e) {
            log.warn("Failed to delete vehicle, not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (UnauthorizedOperationException e) {
            log.warn("Failed to delete vehicle {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting vehicle {}: ", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete vehicle: " + e.getMessage()));
        }
    }

    /**
     * (ADMIN) Get all vehicles in the system.
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<VehicleDTO>>> getAllVehicles() {
        log.info("Admin fetching all vehicles");
        try {
            List<Vehicle> vehicles = vehicleService.getAllVehicles();
            List<VehicleDTO> vehicleDTOs = DTOMapper.toVehicleDTOList(vehicles);
            return ResponseEntity.ok(ApiResponse.success(vehicleDTOs));
        } catch (Exception e) {
            log.error("Error fetching all vehicles: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch all vehicles: " + e.getMessage()));
        }
    }
}


