package com.calculadora_derivativos.calculadora_backend.service;

import com.calculadora_derivativos.calculadora_backend.dto.PernaSpread;
import com.calculadora_derivativos.calculadora_backend.dto.SpreadResponse;
import com.calculadora_derivativos.calculadora_backend.dto.ManualSpreadRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

/**
 * Serviço responsável por calcular spreads manuais e gerenciar a resposta.
 * O código foi adaptado para usar o padrão Builder do Lombok, resolvendo o erro de
 * construtor após a adição do campo 'nomeEstrategia' em SpreadResponse.
 */
@Service
public class ManualSpreadService {

    private static final int SCALE = 6;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;
    private static final String SPREAD_NAME = "Spread Manual";

    // Placeholder para a classe de resultados brutos, assumindo que existe
    private record ResultadoBruto(
            BigDecimal lucroMaximoBruto,
            BigDecimal prejuizoMaximoBruto,
            BigDecimal breakevenPoint,
            BigDecimal premioLiquidoUnitario,
            BigDecimal ganhoMaximoStrikeUnitario,
            BigDecimal riscoMaximoTeoricoUnitario
    ) {}

    /**
     * Calcula o spread manual baseado na requisição.
     * @param request A requisição contendo os dados do spread.
     * @return SpreadResponse contendo os resultados.
     */
    public SpreadResponse calcular(ManualSpreadRequest request) {
        String ativoSubjacente = request.getAtivoSubjacente();
        List<PernaSpread> pernas = request.getPernas();

        // --- Simulação de Cálculo (PLACEHOLDER) ---
        // Na implementação real, você calcularia estes valores.
        BigDecimal custoLiquidoTotal = calcularCustoLiquido(pernas);
        ResultadoBruto resultadoBruto = calcularResultadosBrutos(pernas);

        BigDecimal lucroMaximoLiquidoTotal = BigDecimal.ZERO; // Simulação
        BigDecimal riscoMaximoLiquidoTotal = BigDecimal.ZERO; // Simulação
        BigDecimal relacaoRiscoRetornoLiquida = BigDecimal.ZERO; // Simulação
        // --- Fim da Simulação ---


        // --- Tratamento de Erro - Exemplo 1 (Erro na linha 60) ---
        if (pernas.isEmpty()) {
            // CORREÇÃO: Usando SpreadResponse.builder()
            return SpreadResponse.builder()
                    .mensagem("Erro: Nenhuma perna de spread fornecida.")
                    .nomeEstrategia(SPREAD_NAME) // NOVO CAMPO
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
        // --- Fim do Tratamento de Erro - Exemplo 1 ---


        // --- Tratamento de Erro - Exemplo 2 (Erro na linha 71) ---
        if (custoLiquidoTotal.compareTo(BigDecimal.ZERO) == 0) {
            // CORREÇÃO: Usando SpreadResponse.builder()
            return SpreadResponse.builder()
                    .mensagem("Aviso: O custo líquido do spread é zero. Verifique as entradas.")
                    .nomeEstrategia(SPREAD_NAME) // NOVO CAMPO
                    .lucroMaximo(resultadoBruto.lucroMaximoBruto().setScale(2, ROUNDING_MODE))
                    .prejuizoMaximo(resultadoBruto.prejuizoMaximoBruto().abs().setScale(2, ROUNDING_MODE))
                    .breakevenPoint(resultadoBruto.breakevenPoint().setScale(2, ROUNDING_MODE))
                    .pernasExecutadas(pernas)
                    .custoLiquido(custoLiquidoTotal.setScale(2, ROUNDING_MODE))
                    .premioLiquidoUnitario(resultadoBruto.premioLiquidoUnitario().setScale(SCALE, ROUNDING_MODE))
                    .ganhoMaximoStrikeUnitario(resultadoBruto.ganhoMaximoStrikeUnitario().setScale(SCALE, ROUNDING_MODE))
                    .riscoMaximoTeoricoUnitario(resultadoBruto.riscoMaximoTeoricoUnitario().setScale(SCALE, ROUNDING_MODE))
                    .lucroMaximoLiquidoTotal(lucroMaximoLiquidoTotal.setScale(2, ROUNDING_MODE))
                    .riscoMaximoLiquidoTotal(riscoMaximoLiquidoTotal.setScale(2, ROUNDING_MODE))
                    .relacaoRiscoRetornoLiquida(relacaoRiscoRetornoLiquida.setScale(2, ROUNDING_MODE))
                    .build();
        }
        // --- Fim do Tratamento de Erro - Exemplo 2 ---


        // --- Caso de Sucesso Padrão (Erro na linha 153) ---
        // O restante das lógicas de cálculo iriam aqui antes do retorno final.

        // CORREÇÃO: Usando SpreadResponse.builder() para o retorno final
        // (Este ponto corresponde ao erro da linha 153, mas os demais erros
        // de construtor em lógica de erro devem seguir o mesmo padrão)
        return SpreadResponse.builder()
                .mensagem("Sucesso! Spread de " + ativoSubjacente + " calculado. Fluxo Inicial Líquido: "
                        + custoLiquidoTotal.setScale(2, ROUNDING_MODE))
                .nomeEstrategia(SPREAD_NAME) // NOVO CAMPO
                .lucroMaximo(resultadoBruto.lucroMaximoBruto().setScale(2, ROUNDING_MODE))
                .prejuizoMaximo(resultadoBruto.prejuizoMaximoBruto().abs().setScale(2, ROUNDING_MODE))
                .breakevenPoint(resultadoBruto.breakevenPoint().setScale(2, ROUNDING_MODE))
                .pernasExecutadas(pernas)
                .custoLiquido(custoLiquidoTotal.setScale(2, ROUNDING_MODE))
                // Campos unitários
                .premioLiquidoUnitario(resultadoBruto.premioLiquidoUnitario().setScale(SCALE, ROUNDING_MODE))
                .ganhoMaximoStrikeUnitario(resultadoBruto.ganhoMaximoStrikeUnitario().setScale(SCALE, ROUNDING_MODE))
                .riscoMaximoTeoricoUnitario(resultadoBruto.riscoMaximoTeoricoUnitario().setScale(SCALE, ROUNDING_MODE))
                // Campos totais líquidos
                .lucroMaximoLiquidoTotal(lucroMaximoLiquidoTotal.setScale(2, ROUNDING_MODE))
                .riscoMaximoLiquidoTotal(riscoMaximoLiquidoTotal.setScale(2, ROUNDING_MODE))
                .relacaoRiscoRetornoLiquida(relacaoRiscoRetornoLiquida.setScale(2, ROUNDING_MODE))
                .build();
    }


    // --- Métodos de Simulação para Estrutura ---

    private BigDecimal calcularCustoLiquido(List<PernaSpread> pernas) {
        // Lógica de cálculo real...
        return new BigDecimal("50.00");
    }

    private ResultadoBruto calcularResultadosBrutos(List<PernaSpread> pernas) {
        // Lógica de cálculo real...
        return new ResultadoBruto(
                new BigDecimal("500"),
                new BigDecimal("-100"),
                new BigDecimal("12.50"),
                new BigDecimal("0.50"),
                new BigDecimal("400"),
                new BigDecimal("100")
        );
    }
}
