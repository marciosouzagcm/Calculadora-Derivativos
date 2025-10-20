package com.calculadora_derivativos.calculadora_backend;

import java.math.BigDecimal;
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
import com.calculadora_derivativos.calculadora_backend.service.ManualSpreadService;

/**
 * Testes Unitários para o ManualSpreadService utilizando Mockito.
 */
@ExtendWith(MockitoExtension.class)
public class ManualSpreadServiceTest {

    @Mock
    private OptionRepository optionRepository;

    @Mock
    private AtivoRepository ativoRepository;

    @InjectMocks
    private ManualSpreadService manualSpreadService;

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

        SpreadRequest request = new SpreadRequest("PETR4", new BigDecimal("38.00"), new BigDecimal("0.01"),
                Arrays.asList(compra, venda));

        // 3. Executar o Serviço
        // AJUSTE: Renomeado de calcularManual para calcularSpread
        SpreadResponse response = manualSpreadService.calcularSpread(request);

        // 4. Assertivas (Valores CORRIGIDOS para base 1)
        // Lucro Máximo (Esperado: 3.98)
        assertEquals(new BigDecimal("3.98"), response.lucroMaximo(), "O Lucro Máximo deve ser R$3.98.");

        // Prejuízo Máximo (Esperado: 1.02)
        assertEquals(new BigDecimal("1.02"), response.prejuizoMaximo(), "O Prejuízo Máximo deve ser R$1.02.");

        // Breakeven Point (Esperado: 36.02)
        assertEquals(new BigDecimal("36.02"), response.breakevenPoint(), "O Breakeven deve ser R$36.02.");

        // Mensagem contém fluxo inicial
        assertTrue(response.mensagem().contains("-1.02"),
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

        // AJUSTE: Renomeado de calcularManual para calcularSpread
        SpreadResponse response = manualSpreadService.calcularSpread(request);

        // Verifica a mensagem de erro esperada
        assertTrue(response.mensagem().startsWith("ERRO: Ticker de opção não encontrado"));
    }

    @Test
    void testPutOptionSingleBuy_PayoffAndCosts() {
        when(optionRepository.findByTicker("PETRP40")).thenReturn(Optional.of(putOpcao));

        PernaSpread compraPut = new PernaSpread("PETRP40", 1, "COMPRA");

        SpreadRequest request = new SpreadRequest("PETR4", new BigDecimal("38.00"), new BigDecimal("0.01"),
                Arrays.asList(compraPut));

        // AJUSTE: Renomeado de calcularManual para calcularSpread
        SpreadResponse response = manualSpreadService.calcularSpread(request);

        // Custo Líquido/Prejuízo Máximo: 2.00 (prêmio) + 0.01 (taxa) = 2.01
        // Lucro Máximo Simulado (CORRIGIDO): O serviço simula até S_min = 30.40 (38.00
        // * 0.8).
        // Lucro Máximo = (Strike 40.00 - S_min 30.40) - Custo 2.01 = 9.60 - 2.01 = 7.59
        assertEquals(new BigDecimal("7.59"), response.lucroMaximo());
        assertEquals(new BigDecimal("2.01"), response.prejuizoMaximo());
        assertTrue(response.mensagem().contains("-2.01"));
    }

    @Test
    void testInvalidOperation_ReturnsError() {
        when(optionRepository.findByTicker("PETRC35")).thenReturn(Optional.of(callOpcaoA));

        PernaSpread pernaInvalid = new PernaSpread("PETRC35", 1, "HOLD");

        SpreadRequest request = new SpreadRequest("PETR4", new BigDecimal("38.00"), new BigDecimal("0.01"),
                Arrays.asList(pernaInvalid));

        // AJUSTE: Renomeado de calcularManual para calcularSpread
        SpreadResponse response = manualSpreadService.calcularSpread(request);

        assertTrue(response.mensagem().startsWith("ERRO: Ação inválida"));
    }

    @Test
    void testNullCotacaoOrTaxas_ReturnsError() {
        PernaSpread compra = new PernaSpread("PETRC35", 1, "COMPRA");
        // cotacaoAtualAtivo null
        SpreadRequest reqNullCotacao = new SpreadRequest("PETR4", null, new BigDecimal("0.01"),
                Arrays.asList(compra));
        // AJUSTE: Renomeado de calcularManual para calcularSpread
        SpreadResponse r1 = manualSpreadService.calcularSpread(reqNullCotacao);
        assertTrue(r1.mensagem().startsWith("ERRO: O SpreadRequest deve incluir"));

        // taxasOperacionais null
        SpreadRequest reqNullTaxa = new SpreadRequest("PETR4", new BigDecimal("38.00"), null,
                Arrays.asList(compra));
        // AJUSTE: Renomeado de calcularManual para calcularSpread
        SpreadResponse r2 = manualSpreadService.calcularSpread(reqNullTaxa);
        assertTrue(r2.mensagem().startsWith("ERRO: O SpreadRequest deve incluir"));
    }

    @Test
    void testTickerNotFound_ErrorMessageIncludesTicker() {
        when(optionRepository.findByTicker("UNKNOWN")).thenReturn(Optional.empty());

        PernaSpread perna = new PernaSpread("UNKNOWN", 1, "COMPRA");

        SpreadRequest request = new SpreadRequest("PETR4", new BigDecimal("38.00"), new BigDecimal("0.01"),
                Arrays.asList(perna));

        // AJUSTE: Renomeado de calcularManual para calcularSpread
        SpreadResponse response = manualSpreadService.calcularSpread(request);

        assertTrue(response.mensagem().contains("UNKNOWN"));
        assertTrue(response.mensagem().startsWith("ERRO: Ticker de opção não encontrado"));
    }
}