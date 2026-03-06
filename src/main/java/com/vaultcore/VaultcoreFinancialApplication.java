package com.vaultcore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class VaultcoreFinancialApplication {
    public static void main(String[] args) {
        SpringApplication.run(VaultcoreFinancialApplication.class, args);
    }
}