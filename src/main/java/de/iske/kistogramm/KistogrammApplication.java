package de.iske.kistogramm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KistogrammApplication {

    public static void main(String[] args) {
        SpringApplication.run(KistogrammApplication.class, args);
    }

}
