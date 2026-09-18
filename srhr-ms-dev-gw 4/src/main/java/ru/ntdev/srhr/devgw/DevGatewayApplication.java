package ru.ntdev.srhr.devgw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ru.ntdev.srhr.devgw.jwt.DevJwtProperties;
import ru.ntdev.srhr.devgw.session.DevSessionProperties;

@SpringBootApplication
@EnableConfigurationProperties({DevJwtProperties.class, DevSessionProperties.class})
public class DevGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevGatewayApplication.class, args);
    }
}
