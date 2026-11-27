package com.poolhawk.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto de entrada de la API REST de PoolHawk.
 *
 * <p>Levanta un servidor embebido con Spring Boot y expone los endpoints
 * para gestionar partidas y eventos de tiro.</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@SpringBootApplication
@EnableScheduling
public class BilliardApplication {

    public static void main(String[] args) {
        SpringApplication.run(BilliardApplication.class, args);
    }
}