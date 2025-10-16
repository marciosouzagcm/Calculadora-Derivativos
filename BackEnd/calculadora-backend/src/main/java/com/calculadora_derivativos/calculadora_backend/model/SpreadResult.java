package com.calculadora_derivativos.calculadora_backend.model;

import lombok.Data;
import java.math.BigDecimal; // IMPORTANTE

@Data
public class SpreadResult {
    
    // --- Identificação do Spread ---
    private String operacaoTipo; // Ex: Bull Call Spread (Débito)
    private String status = "Sucesso";
    private String ativoBase;
    private String vencimento;
    private int diasUteis;
    private BigDecimal cotacaoAtual;
    private int quantidadeContratos;
    
    // --- Perfil de Risco/Retorno (TUDO EM BigDecimal) ---
    private BigDecimal fluxoCaixaInicialBruto; // Prêmio Bruto Total (Crédito ou Custo)
    private BigDecimal premioOuCustoUnitarioLiquido; // O valor do spread por contrato
    private BigDecimal ganhoMaximoTotalLiquido; // lucro_maximo_total do Python
    private BigDecimal prejuizoMaximoTotalLiquido; // risco_maximo_total_liquido do Python
    private BigDecimal relacaoRiscoRetornoLiquida; // Fator de qualidade (lucro / risco)
    private BigDecimal valorNocionalTotal; // Exposiçao Máxima - Diferença de Strikes * Qtd

    // --- Parâmetros de Mercado ---
    private BigDecimal pontoEquilibrio; // Breakeven
    
    // --- Perna de Venda ---
    private String tickerVenda;
    private BigDecimal strikeVenda;
    private BigDecimal premioVenda; // Prêmio unitário de Venda
    
    // --- Perna de Compra ---
    private String tickerCompra;
    private BigDecimal strikeCompra;
    private BigDecimal premioCompra; // Prêmio unitário de Compra
    
    // --- Análise de Gregas Líquidas (NET Gregas) ---
    private BigDecimal netDelta; 
    private BigDecimal netGamma;
    private BigDecimal netTheta;
    private BigDecimal netVega;
    private BigDecimal viVenda; // viCallVendida original
    private BigDecimal viCompra; // viCallComprada original

    // Construtor para falha
    public SpreadResult(String status) {
        this.status = status;
    }
    
    public SpreadResult() {
        // Construtor padrão
    }
}