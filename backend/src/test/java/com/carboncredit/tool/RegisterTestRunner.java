package com.carboncredit.tool;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.carboncredit.CarbonCreditMarketplaceApplication;
import com.carboncredit.service.UserService;
import com.carboncredit.entity.User;

import java.util.Scanner;

public class RegisterTestRunner {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(CarbonCreditMarketplaceApplication.class, args);

        UserService userService = context.getBean(UserService.class);
        Scanner scanner = new Scanner(System.in);

        try {
            System.out.println("=== User Registration ===");

            System.out.print("Enter full name: ");
            String fullName = scanner.nextLine();

            System.out.print("Enter username: ");
            String username = scanner.nextLine();

            System.out.print("Enter phone: ");
            String phone = scanner.nextLine();

            System.out.print("Enter email: ");
            String email = scanner.nextLine();

            System.out.print("Enter password: ");
            String password = scanner.nextLine();

            System.out.print("Confirm password: ");
            String confirmPassword = scanner.nextLine();

            if (!password.equals(confirmPassword)) {
                System.out.println("❌ Passwords do not match!");
                return;
            }

            // Tạo user mới
            User user = new User();
            user.setFullName(fullName);
            user.setUsername(username);
            user.setPhone(phone);
            user.setEmail(email);
            user.setPasswordHash(password); // sẽ được encode trong UserService
            user.setRole(User.UserRole.EV_OWNER); // mặc định EV_OWNER khi register

            User savedUser = userService.createUser(user);
            System.out.println("✅ Registration successful! User ID: " + savedUser.getId());

        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
        } finally {
            context.close();
        }
    }
}
