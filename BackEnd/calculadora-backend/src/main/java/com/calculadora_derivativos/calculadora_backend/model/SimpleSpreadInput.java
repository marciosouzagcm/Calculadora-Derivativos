package com.calculadora_derivativos.calculadora_backend.model;

import lombok.Data; 
import java.math.BigDecimal; // Importação necessária

/**
 * Classe que representa os dados mínimos de entrada do usuário para que o backend 
 * possa iniciar a análise e encontrar o melhor Spread.
 */
@Data
public class SimpleSpreadInput {
    
    // 1. O Ativo para filtrar as opções no DB.
    private String nomeAtivo;      // Ex: BOVA11
    
    // 2. A Cotação atual do ativo, essencial para calcular o prêmio monetário.
    private BigDecimal valorAtivo; // ALTERADO: Double -> BigDecimal
    
    // 3. Taxas operacionais para calcular o lucro líquido.
    private BigDecimal taxas;      // ALTERADO: Double -> BigDecimal
    
    // 4. Quantidade de contratos, com um default sugerido (o usuário pode alterar).
    private Integer quantidade = 1000;
}