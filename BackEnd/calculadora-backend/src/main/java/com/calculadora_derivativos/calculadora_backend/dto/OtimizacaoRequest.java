package com.calculadora_derivativos.calculadora_backend.dto;

import java.math.BigDecimal;

/**
 * Record que representa os dados de entrada para os endpoints de Otimização de Spread.
 * Este record é necessário para resolver o erro "cannot find symbol: class OtimizacaoRequest".
 */
public record OtimizacaoRequest(
    String ativoSubjacente,
    BigDecimal cotacaoAtualAtivo,
    BigDecimal taxasOperacionais
) {}
