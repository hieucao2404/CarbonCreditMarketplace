package com.carboncredit.tool;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.carboncredit.CarbonCreditMarketplaceApplication;
import com.carboncredit.entity.User;
import com.carboncredit.entity.Vehicle;
import com.carboncredit.repository.UserRepository;
import com.carboncredit.repository.VehicleRepository;

public class CreateVehicleTestRunner {

    public static void main(String[] args) {
        // Khởi động Spring Boot context
        ConfigurableApplicationContext context = SpringApplication.run(CarbonCreditMarketplaceApplication.class, args);

        VehicleRepository vehicleRepository = context.getBean(VehicleRepository.class);
        UserRepository userRepository = context.getBean(UserRepository.class);

        Scanner sc = new Scanner(System.in);

        System.out.print("Enter User ID: ");
        String userIdInput = sc.nextLine().trim();

        UUID userId;
        try {
            userId = UUID.fromString(userIdInput);
        } catch (IllegalArgumentException e) {
            System.out.println("❌ Invalid User ID format!");
            context.close();
            return;
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            System.out.println("❌ User not found with ID: " + userId);
            context.close();
            return;
        }

        System.out.print("Enter VIN (17 characters): ");
        String vin = sc.nextLine().trim();

        if (vin.length() != 17) {
            System.out.println("❌ VIN must be exactly 17 characters!");
            context.close();
            return;
        }

        if (vehicleRepository.existsByVin(vin)) {
            System.out.println("❌ Vehicle with VIN already exists: " + vin);
            context.close();
            return;
        }

        System.out.print("Enter Model: ");
        String model = sc.nextLine().trim();

        System.out.print("Enter Registration Date (yyyy-MM-dd): ");
        String dateInput = sc.nextLine().trim();
        LocalDate registrationDate;
        try {
            registrationDate = LocalDate.parse(dateInput);
        } catch (Exception e) {
            System.out.println("❌ Invalid date format! Use yyyy-MM-dd");
            context.close();
            return;
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setUser(userOpt.get());
        vehicle.setVin(vin);
        vehicle.setModel(model);
        vehicle.setRegistrationDate(registrationDate);

        vehicleRepository.save(vehicle);

        System.out.println("✅ Vehicle created successfully!");
        System.out.println("=== Vehicle Info ===");
        System.out.println("Vehicle ID       : " + vehicle.getId());
        System.out.println("VIN              : " + vehicle.getVin());
        System.out.println("Model            : " + vehicle.getModel());
        System.out.println("Registration Date: " + vehicle.getRegistrationDate());
        System.out.println("Owner (User)     : " + vehicle.getUser().getFullName());
        System.out.println("====================");

        context.close();
    }
}
