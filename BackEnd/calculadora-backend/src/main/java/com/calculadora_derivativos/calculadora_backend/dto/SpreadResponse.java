package com.calculadora_derivativos.calculadora_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Builder;
import lombok.Value;

// Assumindo que você usa Lombok @Value e @Builder
@Value
@Builder(toBuilder = true)
public class SpreadResponse {
    
    String mensagem;
    String nomeEstrategia;
    
    // Métricas Brutas Totais
    BigDecimal lucroMaximo;
    BigDecimal prejuizoMaximo;
    BigDecimal breakevenPoint;
    
    // Detalhe da Operação
    List<PernaSpread> pernasExecutadas;
    BigDecimal custoLiquido; // Fluxo de Caixa Inicial (Pode ser receita se for positivo)
    
    // Métricas Unitárias (Para 2 pernas)
    BigDecimal premioLiquidoUnitario;
    BigDecimal ganhoMaximoStrikeUnitario;
    BigDecimal riscoMaximoTeoricoUnitario;
    
    // Métricas Líquidas Totais (considerando taxas)
    BigDecimal lucroMaximoLiquidoTotal;
    BigDecimal riscoMaximoLiquidoTotal;
    BigDecimal relacaoRiscoRetornoLiquida;
    
    // 🟢 Novos Campos para Gregas Líquidas (Net Grega)
    BigDecimal deltaTotal;
    BigDecimal gammaTotal;
    BigDecimal thetaTotal;
    BigDecimal vegaTotal;
    // Campo opcional para otimizações
    LocalDate vencimento;
}