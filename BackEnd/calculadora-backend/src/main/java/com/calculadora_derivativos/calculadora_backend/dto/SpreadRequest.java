package com.calculadora_derivativos.calculadora_backend.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO (Data Transfer Object) para requisição de cálculo de spread.
 * Contém o ativo subjacente, cotação atual, taxas e a lista de pernas da estratégia.
 * Usa Record (Java 16+) para gerar construtor e métodos de acesso automaticamente.
 */
public record SpreadRequest(
    String ativoSubjacente,         // Ticker da ação (Ex: PETR4)
    BigDecimal cotacaoAtualAtivo,   // Preço atual do ativo (necessário para calcular o ponto de equilíbrio)
    BigDecimal taxasOperacionais,   // Taxas e custos por operação
    List<PernaSpread> pernas        // Lista de pernas (compra/venda) que compõem o spread
) {}
