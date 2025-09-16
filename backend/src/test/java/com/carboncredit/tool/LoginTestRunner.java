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
        System.out.flush(); // Force output
        String username = scanner.nextLine();
        
        System.out.print("Enter password: ");
        System.out.flush(); // Force output
        String password = scanner.nextLine();
        
        Optional<User> foundUser = userService.findByUsername(username);
        
        if (foundUser.isPresent()) {
            User user = foundUser.get();
            if (user.getPasswordHash().equals(password)) {
                System.out.println("✅ Login successful! User role: " + user.getRole());
            } else {
                System.out.println("❌ Wrong password");
            }
        } else {
            System.out.println("❌ User not found");
        }
        
        context.close();
    }
}
