package com.carboncredit.tool;

import com.carboncredit.CarbonCreditMarketplaceApplication;
import com.carboncredit.entity.User;
import com.carboncredit.service.UserService;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Optional;
import java.util.Scanner;

public class LoginTestRunner {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(CarbonCreditMarketplaceApplication.class, args);

        UserService userService = context.getBean(UserService.class);
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter username: ");
        String username = scanner.nextLine();

        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        // Try simple authentication first
        boolean authenticated = userService.authenticateUser(username, password);
        
        if (authenticated) {
            Optional<User> foundUser = userService.findByUsername(username);
            if (foundUser.isPresent()) {
                User user = foundUser.get();
                System.out.println("✅ Login successful!");
                System.out.println("Username: " + user.getUsername());
                System.out.println("Role: " + user.getRole());
                System.out.println("Email: " + user.getEmail());
                System.out.println("Full Name: " + user.getFullName());
            }
        } else {
            System.out.println("❌ Invalid username or password!");
        }

        scanner.close();
        context.close();
    }
}
