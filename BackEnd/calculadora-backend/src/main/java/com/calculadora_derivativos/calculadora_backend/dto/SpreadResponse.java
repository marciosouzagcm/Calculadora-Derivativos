package com.calculadora_derivativos.calculadora_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate; // Importa LocalDate
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/**
 * DTO (Data Transfer Object) de resposta contendo o resultado do cálculo de uma estratégia de spread.
 * Alterado de @Value para @Getter e @AllArgsConstructor para permitir o uso do .vencimento() no builder.
 */
@Getter // Gera todos os Getters
@Builder(toBuilder = true) // Habilita o uso do padrão Builder E o método toBuilder()
@AllArgsConstructor // Construtor com todos os argumentos
@Jacksonized
public class SpreadResponse {

    String mensagem;
    
    String nomeEstrategia;

    // --- CAMPOS BRUTOS TEÓRICOS ORIGINAIS (Unitários e Totais de Payoff) ---
    BigDecimal lucroMaximo; // Lucro máximo potencial BRUTO unitário
    BigDecimal prejuizoMaximo; // Prejuízo máximo potencial BRUTO unitário
    BigDecimal breakevenPoint; // Ponto de equilíbrio (Corrigido)
    
    List<PernaSpread> pernasExecutadas;
    
    BigDecimal custoLiquido; // Custo líquido total da montagem do spread (Fluxo de Caixa Inicial)

    // --- CAMPOS UNITÁRIOS CALCULADOS (Ajustes da Simulação) ---
    BigDecimal premioLiquidoUnitario;
    BigDecimal ganhoMaximoStrikeUnitario;
    BigDecimal riscoMaximoTeoricoUnitario;

    // --- CAMPOS LÍQUIDOS TOTAIS (Foco da Otimização e Exibição Final) ---
    BigDecimal lucroMaximoLiquidoTotal;
    BigDecimal riscoMaximoLiquidoTotal;
    BigDecimal relacaoRiscoRetornoLiquida;
    
    // 👈 CAMPO ADICIONADO PARA RESOLVER O ERRO ANTERIOR DO SPREADSERVICE
    LocalDate vencimento; 

    // --- CAMPOS EXPANDIDOS PARA REPLICAR O RELATÓRIO DETALHADO DO PYTHON (GREGAS E VI) ---
    
    BigDecimal netDelta;
    BigDecimal netGamma;
    BigDecimal netTheta;
    BigDecimal netVega;

    @Builder.Default
    List<GegasResponseDTO> gregasPorPerna = List.of();

    BigDecimal viVendaPercentual;
    BigDecimal viCompraPercentual;
}