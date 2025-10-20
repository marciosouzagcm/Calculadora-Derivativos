package com.calculadora_derivativos.calculadora_backend.dto;

/**
 * Representa uma perna individual em uma estratégia de spread (Ex: Compra de PETRC30).
 * O uso de 'record' (Java 16+) simplifica a criação de DTOs, gerando automaticamente 
 * construtor, getters, toString(), equals() e hashCode().
 */
public record PernaSpread(
    String ticker,      // Ex: PETRC30
    int quantidade,     // Geralmente 100 para um lote
    String operacao     // COMPRA ou VENDA
) {}
