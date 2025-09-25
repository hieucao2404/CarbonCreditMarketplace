package com.carboncredit.tool;

import com.carboncredit.CarbonCreditMarketplaceApplication;
import com.carboncredit.entity.User;
import com.carboncredit.service.UserService;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Scanner;

public class LoginTestRunner {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(CarbonCreditMarketplaceApplication.class, args);

        UserService userService = context.getBean(UserService.class);
        Scanner scanner = new Scanner(System.in);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        System.out.print("Enter email or phone: ");
        String username = scanner.nextLine();

        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        Optional<User> foundUser = userService.findByEmailOrPhone(username);

        if (foundUser.isPresent()) {
            User user = foundUser.get();
            if (passwordEncoder.matches(password, user.getPasswordHash())) {
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
