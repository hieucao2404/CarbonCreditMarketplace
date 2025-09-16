package com.carboncredit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class CarbonCreditMarketplaceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CarbonCreditMarketplaceApplication.class, args);
    }
}
