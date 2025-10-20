package com.calculadora_derivativos.calculadora_backend.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO (Data Transfer Object) para a resposta do serviço de otimização de spreads.
 * Contém o resultado da estratégia ideal (melhor Lucro/Prejuízo) e a lista
 * de SpreadResponses que foram avaliados para chegar a esse resultado.
 *
 * Usa Record (Java 16+) para gerar métodos de acesso.
 */
public record OtimizacaoResponse(
    String ativoSubjacente,              // Ativo que foi otimizado (Ex: PETR4)
    String tipoOtimizacao,               // Tipo de spread otimizado (Ex: "CALL SPREAD DE ALTA")
    BigDecimal resultadoOtimizacao,      // Valor máximo (Lucro Máximo ou Prejuízo Mínimo)
    SpreadResponse melhorEstrategia,     // O DTO de resposta que gerou o melhor resultado
    List<SpreadResponse> estrategiasAvaliadas // Lista opcional de todas as estratégias testadas
) {}
