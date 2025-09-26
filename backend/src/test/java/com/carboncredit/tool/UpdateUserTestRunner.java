package com.carboncredit.tool;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.carboncredit.CarbonCreditMarketplaceApplication;
import com.carboncredit.service.UserService;
import com.carboncredit.entity.User;

import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;

public class UpdateUserTestRunner {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(CarbonCreditMarketplaceApplication.class, args);

        UserService userService = context.getBean(UserService.class);
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Update User by ID ===");
        System.out.print("Enter User ID (UUID): ");
        String input = scanner.nextLine();

        try {
            UUID userId = UUID.fromString(input);
            Optional<User> foundUser = userService.findById(userId);

            if (foundUser.isPresent()) {
                User user = foundUser.get();
                System.out.println("✅ Found user: ");

                // Nhập thông tin mới
                System.out.print("Enter new full name (leave blank to skip): ");
                String newFullName = scanner.nextLine();
                if (!newFullName.isBlank()) {
                    user.setFullName(newFullName);
                }

                System.out.print("Enter new phone (leave blank to skip): ");
                String newPhone = scanner.nextLine();
                if (!newPhone.isBlank()) {
                    user.setPhone(newPhone);
                }

                System.out.print("Enter new email (leave blank to skip): ");
                String newEmail = scanner.nextLine();
                if (!newEmail.isBlank()) {
                    user.setEmail(newEmail);
                }

                // Cập nhật database
                User updatedUser = userService.updateUser(user);
                System.out.println("✅ User updated: " + updatedUser);

            } else {
                System.out.println("❌ No user found with ID: " + userId);
            }
        } catch (IllegalArgumentException e) {
            System.out.println("⚠️ Invalid UUID format. Please enter a valid UUID.");
        }

        context.close();
    }
}
