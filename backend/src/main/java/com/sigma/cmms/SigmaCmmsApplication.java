package com.sigma.cmms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SigmaCmmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SigmaCmmsApplication.class, args);
    }
}
