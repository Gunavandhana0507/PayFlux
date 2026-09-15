package com.payflux;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PayfluxApplication {
    public static void main(String[] args) {
        SpringApplication.run(PayfluxApplication.class, args);
    }
}
