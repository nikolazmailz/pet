package ru.vtb.srhr.devrouter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DevRouterApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevRouterApplication.class, args);
    }
}
