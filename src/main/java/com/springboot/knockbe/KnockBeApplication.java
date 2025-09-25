package com.springboot.knockbe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class KnockBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnockBeApplication.class, args);
    }

}