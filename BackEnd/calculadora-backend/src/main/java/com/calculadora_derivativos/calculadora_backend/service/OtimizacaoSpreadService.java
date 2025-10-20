package com.calculadora_derivativos.calculadora_backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;

import org.springframework.stereotype.Service;

import com.calculadora_derivativos.calculadora_backend.dto.OtimizacaoSpreadRequest;
import com.calculadora_derivativos.calculadora_backend.dto.SpreadResponse;

/**
 * Serviço responsável por encontrar a melhor estratégia de spread (otimização).
 * O código foi adaptado para usar o padrão Builder do Lombok, resolvendo o erro
 * de
 * construtor após a adição do campo 'nomeEstrategia' em SpreadResponse.
 */
@Service
public class OtimizacaoSpreadService {

        private static final int SCALE = 6;
        private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;
        private static final String SPREAD_NAME = "Spread Otimizado";

        public SpreadResponse otimizar(OtimizacaoSpreadRequest request) {
                // Lógica de otimização de spread... (Placeholder)

                // Simulação de que o melhor spread foi encontrado e seus dados foram
                // calculados.
                // Na prática, 'melhorSpread' seria um objeto complexo com todos os dados
                // calculados.
                SpreadResponse melhorSpread = simularMelhorResultado();

                if (melhorSpread == null) {
                        // Caso não encontre nenhuma combinação válida
                        // CORREÇÃO: Usando SpreadResponse.builder()
                        return SpreadResponse.builder()
                                        .mensagem("Nenhuma estratégia de spread otimizada foi encontrada dentro dos critérios fornecidos.")
                                        .nomeEstrategia(SPREAD_NAME)
                                        .lucroMaximo(BigDecimal.ZERO)
                                        .prejuizoMaximo(BigDecimal.ZERO)
                                        .breakevenPoint(BigDecimal.ZERO)
                                        .pernasExecutadas(Collections.emptyList())
                                        .custoLiquido(BigDecimal.ZERO)
                                        .premioLiquidoUnitario(BigDecimal.ZERO)
                                        .ganhoMaximoStrikeUnitario(BigDecimal.ZERO)
                                        .riscoMaximoTeoricoUnitario(BigDecimal.ZERO)
                                        .lucroMaximoLiquidoTotal(BigDecimal.ZERO)
                                        .riscoMaximoLiquidoTotal(BigDecimal.ZERO)
                                        .relacaoRiscoRetornoLiquida(BigDecimal.ZERO)
                                        .build();
                }

                // --- Retorno de Sucesso ---
                // CORREÇÃO: Usando SpreadResponse.builder() para construir a resposta final
                return SpreadResponse.builder()
                                .mensagem("Otimização concluída. Melhor estratégia: " + SPREAD_NAME)
                                .nomeEstrategia(SPREAD_NAME) // CAMPO ADICIONADO E SETADO
                                .lucroMaximo(melhorSpread.getLucroMaximo())
                                .prejuizoMaximo(melhorSpread.getPrejuizoMaximo())
                                .breakevenPoint(melhorSpread.getBreakevenPoint())
                                .pernasExecutadas(melhorSpread.getPernasExecutadas())
                                .custoLiquido(melhorSpread.getCustoLiquido())
                                .premioLiquidoUnitario(melhorSpread.getPremioLiquidoUnitario())
                                .ganhoMaximoStrikeUnitario(melhorSpread.getGanhoMaximoStrikeUnitario())
                                .riscoMaximoTeoricoUnitario(melhorSpread.getRiscoMaximoTeoricoUnitario())
                                .lucroMaximoLiquidoTotal(melhorSpread.getLucroMaximoLiquidoTotal())
                                .riscoMaximoLiquidoTotal(melhorSpread.getRiscoMaximoLiquidoTotal())
                                .relacaoRiscoRetornoLiquida(melhorSpread.getRelacaoRiscoRetornoLiquida())
                                .build();
        }

        /** Simula a criação de um SpreadResponse completo após a otimização. */
        private SpreadResponse simularMelhorResultado() {
                return SpreadResponse.builder()
                                .mensagem("Resultado Otimizado")
                                .nomeEstrategia(SPREAD_NAME)
                                .lucroMaximo(new BigDecimal("1000.00"))
                                .prejuizoMaximo(new BigDecimal("200.00"))
                                .breakevenPoint(new BigDecimal("15.50"))
                                .pernasExecutadas(Collections.emptyList())
                                .custoLiquido(new BigDecimal("-50.00"))
                                .premioLiquidoUnitario(new BigDecimal("-0.50"))
                                .ganhoMaximoStrikeUnitario(new BigDecimal("950.00"))
                                .riscoMaximoTeoricoUnitario(new BigDecimal("200.00"))
                                .lucroMaximoLiquidoTotal(new BigDecimal("990.00"))
                                .riscoMaximoLiquidoTotal(new BigDecimal("210.00"))
                                .relacaoRiscoRetornoLiquida(new BigDecimal("4.71"))
                                .build();
        }
}
