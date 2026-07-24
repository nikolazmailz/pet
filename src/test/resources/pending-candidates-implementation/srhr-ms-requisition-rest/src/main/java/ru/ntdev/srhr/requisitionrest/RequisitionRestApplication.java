package ru.ntdev.srhr.requisitionrest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RequisitionRestApplication {
    public static void main(String[] args) {
        SpringApplication.run(RequisitionRestApplication.class, args);
    }
}
