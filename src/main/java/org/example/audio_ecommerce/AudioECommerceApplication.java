package org.example.audio_ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;


@EnableAsync
@SpringBootApplication
public class AudioECommerceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AudioECommerceApplication.class, args);
    }

}
