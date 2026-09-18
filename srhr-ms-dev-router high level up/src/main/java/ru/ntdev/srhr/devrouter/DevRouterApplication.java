package ru.ntdev.srhr.devrouter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ru.ntdev.srhr.devrouter.jwt.DevJwtProperties;

@SpringBootApplication
@EnableConfigurationProperties(DevJwtProperties.class)
public class DevRouterApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevRouterApplication.class, args);
    }
}
