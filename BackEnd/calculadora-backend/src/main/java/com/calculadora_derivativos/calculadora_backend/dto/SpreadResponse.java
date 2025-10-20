package com.calculadora_derivativos.calculadora_backend.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO (Data Transfer Object) de resposta contendo o resultado do cálculo de uma estratégia de spread.
 * O uso de 'record' (Java 16+) garante que os campos sejam acessados como métodos (ex: lucroMaximo()),
 * resolvendo erros de compilação no serviço de otimização.
 */
public record SpreadResponse(
    String mensagem,                            // Mensagem de status ou erro
    BigDecimal lucroMaximo,                     // Lucro máximo potencial da estratégia
    BigDecimal prejuizoMaximo,                  // Prejuízo máximo potencial da estratégia
    BigDecimal breakevenPoint,                  // Ponto de equilíbrio (valor do ativo onde o lucro é zero)
    List<PernaSpread> pernasExecutadas,         // As pernas utilizadas na estratégia
    BigDecimal custoLiquido                     // Custo líquido total da montagem do spread
) {}
