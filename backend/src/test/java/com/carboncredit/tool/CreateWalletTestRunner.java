package com.carboncredit.tool;

import com.carboncredit.entity.User;
import com.carboncredit.entity.Wallet;
import com.carboncredit.repository.UserRepository;
import com.carboncredit.repository.WalletRepository;
import com.carboncredit.service.WalletService;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

import java.util.Optional;
import java.util.Scanner;

public class CreateWalletTestRunner {
    public static void main(String[] args) {
        // Load Spring context
        ApplicationContext context = SpringApplication.run(com.carboncredit.CarbonCreditMarketplaceApplication.class,
                args);

        UserRepository userRepository = context.getBean(UserRepository.class);
        WalletRepository walletRepository = context.getBean(WalletRepository.class);
        WalletService walletService = context.getBean(WalletService.class);

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter keyword (username/email/phone/full name): ");
        String keyword = scanner.nextLine().trim();

        // 1. Tìm user theo keyword
        Optional<User> userOpt = userRepository.findByUsername(keyword);
        if (!userOpt.isPresent())
            userOpt = userRepository.findByEmail(keyword);
        if (!userOpt.isPresent())
            userOpt = userRepository.findByEmailOrPhone(keyword);
        if (!userOpt.isPresent())
            userOpt = userRepository.findByFullName(keyword);

        if (!userOpt.isPresent()) {
            System.out.println("? User not found with keyword: " + keyword);
            SpringApplication.exit(context);
            return;
        }

        User user = userOpt.get();
        System.out.println("✅ Found user: " + user.getUsername() + " (" + user.getEmail() + ")");

        // 2. Kiểm tra đã có ví chưa
        Optional<Wallet> existingWallet = walletRepository.findByUserId(user.getId());
        if (existingWallet.isPresent()) {
            System.out.println("⚠️ Wallet already exists for user!");
            System.out.println(existingWallet.get());
            SpringApplication.exit(context);
            return;
        }

        // 3. Tạo ví mới
        Wallet newWallet = walletService.createWalletForUser(user);
        System.out.println("🎉 Wallet created successfully for user!");
        System.out.println(newWallet);

        SpringApplication.exit(context);
    }
}
