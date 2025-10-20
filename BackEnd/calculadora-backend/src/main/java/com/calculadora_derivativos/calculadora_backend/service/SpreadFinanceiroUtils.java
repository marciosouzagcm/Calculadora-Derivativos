package com.calculadora_derivativos.calculadora_backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SpreadFinanceiroUtils {

    // --- CONSTANTES DO PYTHON (Linhas 16-30 do seu código Python) ---
    public static final int QUANTIDADE_CONTRATOS = 100;
    public static final BigDecimal TAXAS_TOTAIS_OPERACAO = new BigDecimal("44.00");
    public static final BigDecimal MIN_RISCO_RETORNO_LIQUIDO = new BigDecimal("1.0");

    // Configuração de Precisão para Cálculos Internos
    private static final int CASAS_DECIMAIS_CALCULO = 6;
    
    // VISIBILIDADE ALTERADA PARA PUBLIC para resolver o erro de acesso
    public static final RoundingMode MODO_ARREDONDAMENTO = RoundingMode.HALF_EVEN; 

    /**
     * Arredonda valores para uso em cálculos internos (alta precisão).
     */
    public static BigDecimal arredondar(BigDecimal valor) {
        if (valor == null) return BigDecimal.ZERO;
        return valor.setScale(CASAS_DECIMAIS_CALCULO, MODO_ARREDONDAMENTO);
    }
    
    /**
     * Arredonda para duas casas decimais (padrão de moeda) para exibição/valores finais.
     */
    public static BigDecimal arredondarParaMoeda(BigDecimal valor) {
        if (valor == null) return BigDecimal.ZERO;
        return valor.setScale(2, MODO_ARREDONDAMENTO);
    }

    /**
     * Retorna o valor absoluto.
     */
    public static BigDecimal toAbsolute(BigDecimal valor) {
        if (valor == null) return BigDecimal.ZERO;
        return valor.abs();
    }
}