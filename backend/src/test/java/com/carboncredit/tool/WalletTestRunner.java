package com.carboncredit.tool;

import com.carboncredit.CarbonCreditMarketplaceApplication; // ✅ import application chính
import com.carboncredit.CarbonCreditMarketplaceApplication;
import com.carboncredit.entity.Wallet;
import com.carboncredit.service.WalletService;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;

public class WalletTestRunner {
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(CarbonCreditMarketplaceApplication.class, args);

        WalletService walletService = context.getBean(WalletService.class);

        Scanner scanner = new Scanner(System.in);
        System.out.println("=== WALLET TEST RUNNER ===");
        System.out.print("Nhập User ID để xem ví: ");
        String userIdInput = scanner.nextLine();

        try {
            UUID userId = UUID.fromString(userIdInput);

            Optional<Wallet> walletOpt = walletService.findByUserId(userId);

            if (walletOpt.isPresent()) {
                Wallet wallet = walletOpt.get();
                System.out.println(">>> Ví tìm thấy:");
                System.out.println("Wallet ID      : " + wallet.getId());
                System.out.println("User ID        : " + wallet.getUser().getId());
                System.out.println("Credit Balance : " + wallet.getCreditBalance());
                System.out.println("Cash Balance   : " + wallet.getCashBalance());
                System.out.println("Last Updated   : " + wallet.getUpdatedAt());
            } else {
                System.out.println("⚠ Không tìm thấy ví cho User ID: " + userId);
            }

        } catch (IllegalArgumentException e) {
            System.out.println("❌ User ID không hợp lệ! Vui lòng nhập UUID hợp lệ.");
        }
    }
}
