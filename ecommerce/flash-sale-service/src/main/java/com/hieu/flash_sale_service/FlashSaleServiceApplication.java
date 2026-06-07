package com.hieu.flash_sale_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

/** Flash Sale Service entry point. */
@SpringBootApplication
@EnableRetry
public class FlashSaleServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlashSaleServiceApplication.class, args);
    }
}
