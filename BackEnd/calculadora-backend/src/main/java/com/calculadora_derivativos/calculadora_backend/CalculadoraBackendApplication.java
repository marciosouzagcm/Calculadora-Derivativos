package com.calculadora_derivativos.calculadora_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Classe principal da aplicação Spring Boot.
 * A anotação @EnableJpaRepositories força o Spring a escanear o pacote 'repository'
 * para encontrar e inicializar o OpcaoRepository.
 */
@EnableJpaRepositories(basePackages = "com.calculadora_derivativos.calculadora_backend.repository")
@SpringBootApplication
public class CalculadoraBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CalculadoraBackendApplication.class, args);
    }
}
