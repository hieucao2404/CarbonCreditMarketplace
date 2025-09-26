package com.carboncredit.tool;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.carboncredit.CarbonCreditMarketplaceApplication;
import com.carboncredit.service.UserService;
import com.carboncredit.entity.User;

import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;

public class ReadUserTestRunner {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(CarbonCreditMarketplaceApplication.class, args);

        UserService userService = context.getBean(UserService.class);
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Read User Information by ID ===");
        System.out.print("Enter User ID (UUID): ");
        System.out.flush();

        String input = scanner.nextLine();

        try {
            UUID userId = UUID.fromString(input);
            Optional<User> foundUser = userService.findById(userId);

            if (foundUser.isPresent()) {
                User user = foundUser.get();
                System.out.println("✅ User found:");
                System.out.println("ID       : " + user.getId());
                System.out.println("Username : " + user.getUsername());
                System.out.println("FullName : " + user.getFullName());
                System.out.println("Email    : " + user.getEmail());
                System.out.println("Phone    : " + user.getPhone());
                System.out.println("Role     : " + user.getRole());
                System.out.println("CreatedAt: " + user.getCreatedAt());
                System.out.println("UpdatedAt: " + user.getUpdatedAt());
            } else {
                System.out.println("❌ No user found with ID: " + userId);
            }
        } catch (IllegalArgumentException e) {
            System.out.println("⚠️ Invalid UUID format. Please enter a valid UUID.");
        }

        context.close();
    }
}
