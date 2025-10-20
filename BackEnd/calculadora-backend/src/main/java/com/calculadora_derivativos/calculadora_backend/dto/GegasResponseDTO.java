package com.calculadora_derivativos.calculadora_backend.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;
import lombok.AllArgsConstructor;
import lombok.extern.jackson.Jacksonized;

/**
 * DTO auxiliar para armazenar os detalhes das Gregas e indicadores de Volatilidade 
 * Implícita (VI) de uma única perna do spread, para replicação do formato de saída Python.
 */
@Value // Torna a classe imutável e gera os getters
@Builder // Habilita o padrão Builder
@AllArgsConstructor // Construtor com todos os argumentos
@Jacksonized // Ajuda o Jackson na serialização/desserialização
public class GegasResponseDTO {
    
    // Gregas Unitárias
    BigDecimal delta;
    BigDecimal gamma;
    BigDecimal theta;
    BigDecimal vega;

    // Indicadores de Exposição/VI (Para replicar o último item do Python)
    BigDecimal viPerna; // Exemplo: Volatilidade Implícita (VI) da Opção
    String acaoPerna; // "COMPRA" ou "VENDA"
}