package com.calculadora_derivativos.calculadora_backend.service;

import com.calculadora_derivativos.calculadora_backend.dto.SpreadRequest;
import com.calculadora_derivativos.calculadora_backend.dto.SpreadResponse;

/**
 * Interface que define o contrato para os serviços de cálculo de Spreads.
 */
public interface CalculadoraSpreadService {

    /**
     * Calcula o spread com base em uma requisição.
     * * @param request O objeto de requisição contendo as pernas do spread.
     * 
     * @return O objeto de resposta contendo os resultados do cálculo (Lucro Máximo,
     *         Prejuízo Máximo, etc.).
     */
    SpreadResponse calcularSpread(SpreadRequest request);
}