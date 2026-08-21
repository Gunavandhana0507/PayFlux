package com.payflux.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PayFluxApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayFluxApplication.class, args);
    }
}
