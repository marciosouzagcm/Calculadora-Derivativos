package com.calculadora_derivativos.calculadora_backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.calculadora_derivativos.calculadora_backend.dto.PernaSpread;
import com.calculadora_derivativos.calculadora_backend.dto.SpreadRequest;
import com.calculadora_derivativos.calculadora_backend.dto.SpreadResponse;
import com.calculadora_derivativos.calculadora_backend.model.Option;
import com.calculadora_derivativos.calculadora_backend.repository.AtivoRepository;
import com.calculadora_derivativos.calculadora_backend.repository.OptionRepository;

@Service
public class SpreadService implements CalculadoraSpreadService { // Implementando a interface

    private final OptionRepository optionRepository;
    private final AtivoRepository ativoRepository;

    // Constante para arredondamento, comum em cálculos financeiros
    private static final int SCALE = 4;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    // --- CLASSES AUXILIARES (Record) ---
    private record PernaCalculada(
            String ticker, String tipoOpcao, BigDecimal strike, int quantidade,
            String acao, BigDecimal preco // Renomeado para 'preco'
    ) {
    }

    private record ResultadoPayoff(
            BigDecimal lucroMaximo, BigDecimal prejuizoMaximo, BigDecimal breakevenPoint) {
    }

    // --- CONSTRUTOR ATUALIZADO (Injetando OptionRepository e AtivoRepository) ---
    public SpreadService(OptionRepository optionRepository, AtivoRepository ativoRepository) {
        this.optionRepository = optionRepository;
        this.ativoRepository = ativoRepository;
    }

    // --- MÉTODO PRINCIPAL DA API ---
    @Override // Anotação adicionada pois implementamos a interface
    public SpreadResponse calcularSpread(SpreadRequest request) {

        String ativoSubjacente = request.ativoSubjacente();
        List<PernaSpread> pernas = request.pernas();

        // Validação do Request
        if (request.cotacaoAtualAtivo() == null || request.taxasOperacionais() == null) {
            return new SpreadResponse(
                        "ERRO: O SpreadRequest deve incluir a cotação atual do ativo e as taxas operacionais.",
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        List.of(), BigDecimal.ZERO);
        }

        if (pernas == null || pernas.isEmpty()) {
            return new SpreadResponse(
                        "ERRO: A lista de pernas (opções) está vazia.",
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        List.of(), BigDecimal.ZERO);
        }

        // 1. OBTENDO DADOS
        BigDecimal precoAtualSubjacente = request.cotacaoAtualAtivo().setScale(SCALE, ROUNDING_MODE);

        // --- INÍCIO DO CÁLCULO DE CUSTO LÍQUIDO ---
        List<PernaCalculada> pernasParaCalculo = new ArrayList<>();

        // O custo líquido (fluxo de caixa inicial) deve ser inicializado como ZERO
        BigDecimal custoLiquidoTotal = BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE); // AJUSTE DE ESCALA
        BigDecimal taxaUnitario = request.taxasOperacionais().setScale(SCALE, ROUNDING_MODE);

        for (PernaSpread perna : pernas) {
            String ticker = perna.ticker();

            // Busca a opção (RETORNA OPTIONAL)
            Optional<Option> optionOptional = optionRepository.findByTicker(ticker);

            if (optionOptional.isEmpty()) {
                return new SpreadResponse(
                        "ERRO: Ticker de opção não encontrado no banco de dados: " + ticker,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        List.of(), BigDecimal.ZERO);
            }

            Option dadosOpcao = optionOptional.get();

            BigDecimal precoPremio = dadosOpcao.getPreco().setScale(SCALE, ROUNDING_MODE);
            BigDecimal quantidade = new BigDecimal(perna.quantidade());

            // Cálculo do Custo/Crédito Bruto (Prêmio * Quantidade)
            BigDecimal valorPernaBruto = precoPremio.multiply(quantidade).setScale(SCALE, ROUNDING_MODE);

            // Cálculo do Custo de Taxas (Taxa Unitário * Quantidade)
            // Taxas sempre são um custo (Subtrai o valor final do spread)
            BigDecimal taxasTotalPerna = taxaUnitario.multiply(quantidade).abs().setScale(SCALE, ROUNDING_MODE); 

            // LÓGICA DO FLUXO DE CAIXA INICIAL (Custo Líquido Total)
            if ("COMPRA".equalsIgnoreCase(perna.operacao())) {
                // COMPRA (PAGAMENTO): CUSTO (sai do caixa)
                // Subtrai o prêmio e subtrai a taxa (ambos são saída de caixa)
                custoLiquidoTotal = custoLiquidoTotal
                        .subtract(valorPernaBruto)
                        .subtract(taxasTotalPerna);
            } else if ("VENDA".equalsIgnoreCase(perna.operacao())) {
                // VENDA (RECEBIMENTO): CRÉDITO (entra no caixa)
                // Adiciona o prêmio e subtrai a taxa (entrada de caixa - saída de caixa)
                custoLiquidoTotal = custoLiquidoTotal
                        .add(valorPernaBruto)
                        .subtract(taxasTotalPerna);
            } else {
                return new SpreadResponse(
                        "ERRO: Ação inválida para o ticker " + ticker + ". Use 'COMPRA' ou 'VENDA'.",
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        List.of(), BigDecimal.ZERO);
            }

            // A perna calculada registra a ação para o cálculo de Payoff
            pernasParaCalculo.add(new PernaCalculada(
                    ticker,
                    dadosOpcao.getTipo(), // Tipo: CALL ou PUT
                    dadosOpcao.getStrike().setScale(SCALE, ROUNDING_MODE),
                    perna.quantidade(),
                    perna.operacao(), // Ação: COMPRA ou VENDA
                    precoPremio // O prêmio da perna é o 'preco'
            ));
        }

        // 2. CHAMADA DA LÓGICA DE PAYOFF
        ResultadoPayoff resultado = this.calcularOtimizacao(pernasParaCalculo, custoLiquidoTotal, precoAtualSubjacente);

        // 3. Retorno Final
        return new SpreadResponse(
                "Sucesso! Spread de " + ativoSubjacente + " calculado. Fluxo Inicial Líquido: "
                        + custoLiquidoTotal.setScale(2, ROUNDING_MODE),
                resultado.lucroMaximo(),
                resultado.prejuizoMaximo(),
                resultado.breakevenPoint(),
                pernas, // lista original de pernas
                custoLiquidoTotal.setScale(2, ROUNDING_MODE) // custo líquido
        );
    }

    // --- FUNÇÃO RESPONSÁVEL PELA LÓGICA DE PAYOFF E OTIMIZAÇÃO (CORRIGIDA) ---
    private ResultadoPayoff calcularOtimizacao(List<PernaCalculada> pernas, BigDecimal custoLiquido,
            BigDecimal precoAtualSubjacente) {

        // AJUSTE DE ESCALA NA INICIALIZAÇÃO
        BigDecimal lucroMaximo = new BigDecimal("-999999999.00").setScale(SCALE, ROUNDING_MODE);
        BigDecimal prejuizoMaximo = new BigDecimal("999999999.00").setScale(SCALE, ROUNDING_MODE);
        BigDecimal breakevenPoint = BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE);
        BigDecimal menorDiferencaBreakeven = new BigDecimal("999999999.00").setScale(SCALE, ROUNDING_MODE);

        // 1. Definir o Intervalo de Simulação (+/- 20% do preço atual)
        BigDecimal rangeSimulacao = precoAtualSubjacente.multiply(new BigDecimal("0.2")).setScale(SCALE, ROUNDING_MODE);

        BigDecimal precoMinimo = precoAtualSubjacente.subtract(rangeSimulacao).max(BigDecimal.ZERO).setScale(SCALE,
                ROUNDING_MODE);
        BigDecimal precoMaximo = precoAtualSubjacente.add(rangeSimulacao).setScale(SCALE, ROUNDING_MODE);

        // 2. Iterar sobre os preços simulados
        BigDecimal precoSimulado = precoMinimo;
        BigDecimal passoSimulacao = new BigDecimal("0.01").setScale(SCALE, ROUNDING_MODE);

        while (precoSimulado.compareTo(precoMaximo) <= 0) {

            // O lucro inicial é o NEGATIVO do custo líquido (Fluxo de Caixa Inicial)
            BigDecimal lucroTotalNoPonto = custoLiquido.negate();

            // CÁLCULO DO PAYOFF DE CADA PERNA
            for (PernaCalculada perna : pernas) {

                BigDecimal payoffPerna;

                // Payoff no Vencimento (Intrinsic Value)
                if ("CALL".equalsIgnoreCase(perna.tipoOpcao())) {
                    // Call: max(S - K, 0)
                    payoffPerna = precoSimulado.subtract(perna.strike()).max(BigDecimal.ZERO);
                } else { // PUT
                    // Put: max(K - S, 0)
                    payoffPerna = perna.strike().subtract(precoSimulado).max(BigDecimal.ZERO);
                }

                // Multiplica o Payoff pela quantidade de contratos
                payoffPerna = payoffPerna.multiply(new BigDecimal(perna.quantidade()));

                // LÓGICA DE LIQUIDAÇÃO: Se é uma VENDA, você liquida o Payoff (paga o valor)
                if ("VENDA".equalsIgnoreCase(perna.acao())) {
                    payoffPerna = payoffPerna.negate();
                }

                // Adiciona o payoff da perna ao lucro total
                lucroTotalNoPonto = lucroTotalNoPonto.add(payoffPerna);
            }

            // Força a escala após a soma de todas as pernas.
            lucroTotalNoPonto = lucroTotalNoPonto.setScale(SCALE, ROUNDING_MODE);

            // ATUALIZAR MÁXIMOS, MÍNIMOS E BREAKEVEN (Simulação)
            if (lucroTotalNoPonto.compareTo(lucroMaximo) > 0) {
                lucroMaximo = lucroTotalNoPonto;
            }
            if (lucroTotalNoPonto.compareTo(prejuizoMaximo) < 0) {
                prejuizoMaximo = lucroTotalNoPonto;
            }

            // Busca o Breakeven Point
            BigDecimal diferencaZero = lucroTotalNoPonto.abs();
            if (diferencaZero.compareTo(menorDiferencaBreakeven) < 0) {
                menorDiferencaBreakeven = diferencaZero;
                breakevenPoint = precoSimulado;
            }

            // Move para o próximo ponto simulado
            precoSimulado = precoSimulado.add(passoSimulacao).setScale(SCALE, ROUNDING_MODE);
        }

        // -------------------------------------------------------------------------
        // --- NOVO PASSO 3: APLICAÇÃO DA LÓGICA DE PAYOFF LIMITADO (PARA SPREADS VERTICAIS) ---
        // --- ESSA LÓGICA CORRIGE IMPRECISÕES NA SIMULAÇÃO DE RISCO MÁXIMO ---
        // -------------------------------------------------------------------------

        // Aplicamos a lógica limitada se for um Spread Vertical de 2 pernas.
        if (pernas.size() == 2) {
            PernaCalculada perna1 = pernas.get(0);
            PernaCalculada perna2 = pernas.get(1);

            // Verifica se é um Spread Vertical (mesmo tipo e mesma quantidade/lote)
            boolean isVerticalSpread = perna1.tipoOpcao().equals(perna2.tipoOpcao())
                    && perna1.quantidade() == perna2.quantidade();

            if (isVerticalSpread) {
                
                // Variáveis para simplificar o código
                BigDecimal strikeAlto;
                BigDecimal strikeBaixo;
                
                if (perna1.strike().compareTo(perna2.strike()) > 0) {
                    strikeAlto = perna1.strike();
                    strikeBaixo = perna2.strike();
                } else {
                    strikeAlto = perna2.strike();
                    strikeBaixo = perna1.strike();
                }

                BigDecimal diferencaStrikes = strikeAlto.subtract(strikeBaixo).abs();
                BigDecimal nocionalTotal = diferencaStrikes.multiply(new BigDecimal(perna1.quantidade()));
                
                // O custoLíquido (Cash Flow Inicial) já está em escala
                BigDecimal fluxoCaixaInicial = custoLiquido;
                
                // Se custoLiquido < 0 (DÉBITO) -> Compra é mais cara que Venda (CALL SPREAD DE ALTA / PUT SPREAD DE BAIXA)
                if (fluxoCaixaInicial.compareTo(BigDecimal.ZERO) < 0) { 
                    // Spread de DÉBITO (Cash Out): Prejuízo Máximo = Custo. Lucro Máximo = Nocional - Custo.
                    BigDecimal riscoMaximo = fluxoCaixaInicial.negate().abs().setScale(SCALE, ROUNDING_MODE);
                    BigDecimal lucroMaximoTeorico = nocionalTotal.subtract(riscoMaximo).setScale(SCALE, ROUNDING_MODE);
                    
                    if (lucroMaximoTeorico.compareTo(lucroMaximo) > 0) {
                        lucroMaximo = lucroMaximoTeorico;
                    }
                    // O prejuízo máximo é o risco teórico (negativo)
                    prejuizoMaximo = riscoMaximo.negate();
                    
                } 
                // Se custoLiquido > 0 (CRÉDITO) -> Venda é mais cara que Compra (CALL SPREAD DE BAIXA / PUT SPREAD DE ALTA)
                else if (fluxoCaixaInicial.compareTo(BigDecimal.ZERO) > 0) {
                    // Spread de CRÉDITO (Cash In): Lucro Máximo = Crédito. Risco Máximo = Nocional - Crédito.
                    
                    BigDecimal lucroMaximoTeorico = fluxoCaixaInicial.setScale(SCALE, ROUNDING_MODE);
                    BigDecimal riscoMaximo = nocionalTotal.subtract(lucroMaximoTeorico).setScale(SCALE, ROUNDING_MODE);
                    
                    // O lucro máximo do Spread de Crédito é o valor do Crédito
                    lucroMaximo = lucroMaximoTeorico;

                    // O prejuízo máximo é o risco teórico (nocional - crédito), expresso como um valor negativo
                    prejuizoMaximo = riscoMaximo.negate(); 

                } else {
                    // Custo zero: Lucro e Risco = Nocional
                    lucroMaximo = nocionalTotal;
                    prejuizoMaximo = nocionalTotal.negate();
                }
            }
        }
        // --- FIM DA LÓGICA DE PAYOFF LIMITADO ---
        // -------------------------------------------------------------------------


        return new ResultadoPayoff(
                lucroMaximo.setScale(2, ROUNDING_MODE),
                // Retorna o valor ABSOLUTO do prejuízo máximo
                prejuizoMaximo.negate().abs().setScale(2, ROUNDING_MODE), 
                breakevenPoint.setScale(2, ROUNDING_MODE));
    }
}