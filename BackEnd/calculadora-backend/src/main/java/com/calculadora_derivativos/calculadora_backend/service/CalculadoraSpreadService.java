package com.calculadora_derivativos.calculadora_backend.service;

import java.math.BigDecimal;
import com.calculadora_derivativos.calculadora_backend.dto.SpreadRequest;
import com.calculadora_derivativos.calculadora_backend.dto.SpreadResponse;

/**
 * Interface que define os métodos de cálculo e otimização de spreads.
 */
public interface CalculadoraSpreadService {

    /**
     * Realiza o cálculo detalhado de um spread com base nas pernas fornecidas.
     * @param request Dados do spread (pernas, ativo, cotação, taxas).
     * @return SpreadResponse contendo métricas calculadas.
     */
    SpreadResponse calcularSpread(SpreadRequest request);

    /**
     * Otimiza e retorna o melhor Bull Call Spread para o ativo.
     */
    SpreadResponse otimizarBullCallSpread(String ativoSubjacente, BigDecimal cotacaoAtualAtivo,
            BigDecimal taxasOperacionais);
            
    /**
     * Otimiza e retorna o melhor Bear Put Spread para o ativo.
     */
    SpreadResponse otimizarBearPutSpread(String ativoSubjacente, BigDecimal cotacaoAtualAtivo,
            BigDecimal taxasOperacionais);
            
    /**
     * Otimiza e retorna o melhor Bull Put Spread para o ativo.
     */
    SpreadResponse otimizarBullPutSpread(String ativoSubjacente, BigDecimal cotacaoAtualAtivo,
            BigDecimal taxasOperacionais);
            
    /**
     * Otimiza e retorna o melhor Bear Call Spread para o ativo.
     */
    SpreadResponse otimizarBearCallSpread(String ativoSubjacente, BigDecimal cotacaoAtualAtivo,
            BigDecimal taxasOperacionais);
            
    /**
     * Compara as 4 estratégias verticais e retorna a que oferece o melhor resultado
     * (maior Relação Risco/Retorno Líquida).
     * * ESTE É O MÉTODO QUE ESTAVA FALTANDO NA SUA INTERFACE.
     */
    SpreadResponse otimizarMelhorEstrategia(String ativoSubjacente, BigDecimal cotacaoAtualAtivo,
            BigDecimal taxasOperacionais);
}