package com.calculadora_derivativos.calculadora_backend.model;

import lombok.Data; 

/**
 * Classe que representa os dados mínimos de entrada do usuário para que o backend 
 * possa iniciar a análise e encontrar o melhor Spread (Bear Call Spread) 
 * na tabela 'opcoes'.
 */
@Data
public class SimpleSpreadInput {
    
    // 1. O Ativo para filtrar as opções no DB.
    private String nomeAtivo;      // Ex: BOVA11
    
    // 2. A Cotação atual do ativo, essencial para calcular o prêmio monetário.
    private Double valorAtivo;     // Ex: 120.50
    
    // 3. Taxas operacionais para calcular o lucro líquido.
    private Double taxas;          // Taxas e emolumentos por contrato (Ex: 0.05)
    
    // 4. Quantidade de contratos, com um default sugerido (o usuário pode alterar).
    private Integer quantidade = 1000;
}