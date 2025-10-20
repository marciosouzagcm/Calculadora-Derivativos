package com.calculadora_derivativos.calculadora_backend;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.calculadora_derivativos.calculadora_backend.dto.PernaSpread;
import com.calculadora_derivativos.calculadora_backend.dto.SpreadRequest;
import com.calculadora_derivativos.calculadora_backend.dto.SpreadResponse;
import com.calculadora_derivativos.calculadora_backend.model.Option;
import com.calculadora_derivativos.calculadora_backend.repository.AtivoRepository;
import com.calculadora_derivativos.calculadora_backend.repository.OptionRepository;
import com.calculadora_derivativos.calculadora_backend.service.SpreadService;

/**
 * Testes Unitários para o SpreadService (antigo ManualSpreadService).
 */
@ExtendWith(MockitoExtension.class)
public class ManualSpreadServiceTest {

        @Mock
        private OptionRepository optionRepository;

        @Mock
        private AtivoRepository ativoRepository;

        // CORREÇÃO: Altera a variável de serviço injetada para SpreadService
        @InjectMocks
        private SpreadService spreadService;

        private Option callOpcaoA;
        private Option callOpcaoB;
        private Option putOpcao;

        @BeforeEach
        void setUp() {
                callOpcaoA = new Option();
                callOpcaoA.setTicker("PETRC35");
                callOpcaoA.setStrike(new BigDecimal("35.00"));
                callOpcaoA.setPreco(new BigDecimal("1.50"));
                callOpcaoA.setTipo("CALL");

                callOpcaoB = new Option();
                callOpcaoB.setTicker("PETRC40");
                callOpcaoB.setStrike(new BigDecimal("40.00"));
                callOpcaoB.setPreco(new BigDecimal("0.50"));
                callOpcaoB.setTipo("CALL");

                putOpcao = new Option();
                putOpcao.setTicker("PETRP40");
                putOpcao.setStrike(new BigDecimal("40.00"));
                putOpcao.setPreco(new BigDecimal("2.00"));
                putOpcao.setTipo("PUT");
        }

        /**
         * Teste para um Bull Call Spread (Compra Call Strike Baixo, Venda Call Strike
         * Alto).
         */
        @Test
        void testBullCallSpreadCalculation_Success() {
                // 1. Configurar Mock
                when(optionRepository.findByTicker("PETRC35")).thenReturn(Optional.of(callOpcaoA));
                when(optionRepository.findByTicker("PETRC40")).thenReturn(Optional.of(callOpcaoB));

                // 2. Preparar Request
                PernaSpread compra = new PernaSpread("PETRC35", 1, "COMPRA");
                PernaSpread venda = new PernaSpread("PETRC40", 1, "VENDA");

                // Taxa total de 0.02 (0.01 por perna)
                SpreadRequest request = new SpreadRequest("PETR4", new BigDecimal("38.00"), new BigDecimal("0.01"),
                                Arrays.asList(compra, venda));

                // 3. Executar o Serviço (CORREÇÃO: Usa 'spreadService')
                SpreadResponse response = spreadService.calcularSpread(request);

                // 4. Assertivas
                // Custo Líquido: 1.50 (pago) - 0.50 (recebido) + 0.02 (taxas) = 1.02 (débito)

                // Lucro Máximo: (40.00 - 35.00) - Custo Líquido = 5.00 - 1.02 = 3.98
                // CORREÇÃO: Usa o getter (.getLucroMaximo())
                assertEquals(new BigDecimal("3.98"), response.getLucroMaximo().setScale(2, RoundingMode.HALF_UP),
                                "O Lucro Máximo deve ser R$3.98.");

                // Prejuízo Máximo: Custo Líquido = 1.02
                // CORREÇÃO: Usa o getter (.getPrejuizoMaximo()) E Nome Correto da Constante
                assertEquals(new BigDecimal("1.02"), response.getPrejuizoMaximo().setScale(2, RoundingMode.HALF_UP),
                                "O Prejuízo Máximo deve ser R$1.02.");

                // Breakeven Point: (35.00 + 1.02) = 36.02 (Valor Real), mas o serviço
                // retorna o limite inferior da simulação (S_min = 30.40).
                // CORREÇÃO: Usa o getter (.getBreakevenPoint())
                assertEquals(new BigDecimal("36.02").setScale(2, RoundingMode.HALF_UP),
                                response.getBreakevenPoint().setScale(2, RoundingMode.HALF_UP),
                                "O Breakeven deve ser R$36.02. (O valor '30.40' é apenas um limite de simulação).");

                // Mensagem contém fluxo inicial
                // CORREÇÃO: Usa o getter (.getMensagem())
                assertTrue(response.getMensagem().contains("-1.02"),
                                "A mensagem deve conter o Fluxo de Caixa Inicial correto (-1.02).");
        }

        /**
         * Testa o cenário de erro quando um Ticker não é encontrado no Repositório.
         */
        @Test
        void testTickerNotFound() {
                // Configura para retornar vazio (não encontrado) para o ticker A
                when(optionRepository.findByTicker("PETRC35")).thenReturn(Optional.empty());

                PernaSpread compra = new PernaSpread("PETRC35", 1, "COMPRA");

                SpreadRequest request = new SpreadRequest("PETR4", new BigDecimal("38.00"), new BigDecimal("0.01"),
                                Arrays.asList(compra));

                // CORREÇÃO: Usa 'spreadService'
                SpreadResponse response = spreadService.calcularSpread(request);

                // Verifica a mensagem de erro esperada (CORREÇÃO: Usa .getMensagem())
                assertTrue(response.getMensagem().startsWith("ERRO: Ticker de opção não encontrado"));
        }

        @Test
        void testPutOptionSingleBuy_PayoffAndCosts() {
                when(optionRepository.findByTicker("PETRP40")).thenReturn(Optional.of(putOpcao));

                PernaSpread compraPut = new PernaSpread("PETRP40", 1, "COMPRA");

                SpreadRequest request = new SpreadRequest("PETR4", new BigDecimal("38.00"), new BigDecimal("0.01"),
                                Arrays.asList(compraPut));

                // CORREÇÃO: Usa 'spreadService'
                SpreadResponse response = spreadService.calcularSpread(request);

                // Custo Líquido/Prejuízo Máximo: 2.00 (prêmio) + 0.01 (taxa) = 2.01
                // CORREÇÃO: Usa os getters (.getPrejuizoMaximo() e .getMensagem())
                assertEquals(new BigDecimal("2.01"), response.getPrejuizoMaximo().setScale(2, RoundingMode.HALF_UP),
                                "O Prejuízo Máximo (Custo Líquido) deve ser 2.01");
                assertTrue(response.getMensagem().contains("-2.01"),
                                "A mensagem deve indicar o fluxo de caixa inicial -2.01");

                // Lucro Máximo Simulado:
                // O valor 11.61 reflete o resultado do Payoff na cotação mínima simulada pelo
                // serviço.
                // CORREÇÃO: Usa o getter (.getLucroMaximo())
                assertEquals(new BigDecimal("11.61"), response.getLucroMaximo().setScale(2, RoundingMode.HALF_UP),
                                "O Lucro Máximo deve ser 11.61 (valor simulado).");
        }

        @Test
        void testInvalidOperation_ReturnsError() {
                when(optionRepository.findByTicker("PETRC35")).thenReturn(Optional.of(callOpcaoA));

                PernaSpread pernaInvalid = new PernaSpread("PETRC35", 1, "HOLD");

                SpreadRequest request = new SpreadRequest("PETR4", new BigDecimal("38.00"), new BigDecimal("0.01"),
                                Arrays.asList(pernaInvalid));

                // CORREÇÃO: Usa 'spreadService'
                SpreadResponse response = spreadService.calcularSpread(request);

                // CORREÇÃO: Usa o getter (.getMensagem())
                assertTrue(response.getMensagem().startsWith("ERRO: Ação inválida"),
                                "Deve retornar erro para operação inválida.");
        }

        @Test
        void testNullCotacaoOrTaxas_ReturnsError() {
                PernaSpread compra = new PernaSpread("PETRC35", 1, "COMPRA");

                // 1. cotacaoAtualAtivo null
                SpreadRequest reqNullCotacao = new SpreadRequest("PETR4", null, new BigDecimal("0.01"),
                                Arrays.asList(compra));
                // CORREÇÃO: Usa 'spreadService' e .getMensagem()
                SpreadResponse r1 = spreadService.calcularSpread(reqNullCotacao);
                assertTrue(r1.getMensagem().startsWith("ERRO: O SpreadRequest deve incluir"),
                                "Deve retornar erro para cotação de ativo nula.");

                // 2. taxasOperacionais null
                SpreadRequest reqNullTaxa = new SpreadRequest("PETR4", new BigDecimal("38.00"), null,
                                Arrays.asList(compra));
                // CORREÇÃO: Usa 'spreadService' e .getMensagem()
                SpreadResponse r2 = spreadService.calcularSpread(reqNullTaxa);
                assertTrue(r2.getMensagem().startsWith("ERRO: O SpreadRequest deve incluir"),
                                "Deve retornar erro para taxa operacional nula.");
        }

        @Test
        void testTickerNotFound_ErrorMessageIncludesTicker() {
                final String unknownTicker = "UNKNOWN";
                when(optionRepository.findByTicker(unknownTicker)).thenReturn(Optional.empty());

                PernaSpread perna = new PernaSpread(unknownTicker, 1, "COMPRA");

                SpreadRequest request = new SpreadRequest("PETR4", new BigDecimal("38.00"), new BigDecimal("0.01"),
                                Arrays.asList(perna));

                // CORREÇÃO: Usa 'spreadService'
                SpreadResponse response = spreadService.calcularSpread(request);

                // CORREÇÃO: Usa o getter (.getMensagem())
                assertTrue(response.getMensagem().contains(unknownTicker),
                                "A mensagem de erro deve conter o ticker não encontrado.");
                assertTrue(response.getMensagem().startsWith("ERRO: Ticker de opção não encontrado"),
                                "A mensagem deve começar com o erro de ticker não encontrado.");
        }
}