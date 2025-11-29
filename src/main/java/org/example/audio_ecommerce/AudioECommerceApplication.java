package org.example.audio_ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableAsync
@SpringBootApplication
@EnableScheduling
@EnableJpaAuditing
public class AudioECommerceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AudioECommerceApplication.class, args);
    }
}
