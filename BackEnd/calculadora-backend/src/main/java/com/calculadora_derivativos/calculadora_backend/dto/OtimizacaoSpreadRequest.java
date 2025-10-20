package com.calculadora_derivativos.calculadora_backend.dto;

import java.math.BigDecimal;

/**
 * Record que representa os dados de entrada para o serviço de otimização de estratégias de spread.
 * Contém os critérios que o serviço utilizará para buscar a melhor combinação de spread.
 */
public record OtimizacaoSpreadRequest(
    // Ativo objeto da opção (Ex: "VALE3")
    String ativoSubjacente,

    // Critérios de otimização
    BigDecimal riscoMaximoAceitavel,
    BigDecimal retornoMinimoDesejado,
    
    // Outros parâmetros de filtro
    String dataVencimento
) {}
