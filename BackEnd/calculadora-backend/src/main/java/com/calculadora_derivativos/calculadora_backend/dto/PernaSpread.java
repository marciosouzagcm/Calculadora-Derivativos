package com.calculadora_derivativos.calculadora_backend.dto;

/**
 * DTO simplificado usado nos requests/tests do projeto.
 */
public record PernaSpread(
        String ticker, // ex: "PETRC35"
        int quantidade, // ex: 1
        String operacao // "COMPRA" ou "VENDA"
) {
}
