package ru.ntdev.srhr.requisition;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RequisitionApplication {
    public static void main(String[] args) {
        SpringApplication.run(RequisitionApplication.class, args);
    }
}
