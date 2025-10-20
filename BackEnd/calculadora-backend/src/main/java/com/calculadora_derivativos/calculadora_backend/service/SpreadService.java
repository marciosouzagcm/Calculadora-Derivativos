package com.calculadora_derivativos.calculadora_backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.calculadora_derivativos.calculadora_backend.dto.PernaSpread;
import com.calculadora_derivativos.calculadora_backend.dto.SpreadRequest;
import com.calculadora_derivativos.calculadora_backend.dto.SpreadResponse;
import com.calculadora_derivativos.calculadora_backend.model.Option;
import com.calculadora_derivativos.calculadora_backend.repository.AtivoRepository;
import com.calculadora_derivativos.calculadora_backend.repository.OptionRepository;
import static com.calculadora_derivativos.calculadora_backend.service.SpreadFinanceiroUtils.QUANTIDADE_CONTRATOS;
import static com.calculadora_derivativos.calculadora_backend.service.SpreadFinanceiroUtils.TAXAS_TOTAIS_OPERACAO;
import static com.calculadora_derivativos.calculadora_backend.service.SpreadFinanceiroUtils.arredondar;
import static com.calculadora_derivativos.calculadora_backend.service.SpreadFinanceiroUtils.arredondarParaMoeda;

@Service
public class SpreadService implements CalculadoraSpreadService {

    private final OptionRepository optionRepository;
    private final AtivoRepository ativoRepository;

    private static final int SCALE = 4;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    // --- CLASSES AUXILIARES (Record) ---
    private record PernaCalculada(
             String ticker, String tipoOpcao, BigDecimal strike, int quantidade,
             String acao, BigDecimal preco) {
    }

    // Resultado do Payoff por Simulação. Adiciona o Prêmio Líquido Unitário (para o
    // cálculo das métricas finais)
    private record ResultadoPayoff(
             BigDecimal lucroMaximoBruto, BigDecimal prejuizoMaximoBruto,
             BigDecimal breakevenPoint, BigDecimal premioLiquidoUnitario,
             BigDecimal ganhoMaximoStrikeUnitario, BigDecimal riscoMaximoTeoricoUnitario) {
    }

    public SpreadService(OptionRepository optionRepository, AtivoRepository ativoRepository) {
        this.optionRepository = optionRepository;
        this.ativoRepository = ativoRepository;
    }

    // --- MÉTODO AUXILIAR PARA RESPOSTAS DE ERRO (Refatoração) ---
    private SpreadResponse createErrorResponse(String mensagem, String nomeEstrategia) {
        return SpreadResponse.builder()
                 .mensagem(mensagem)
                 .nomeEstrategia(nomeEstrategia)
                 .lucroMaximo(BigDecimal.ZERO)
                 .prejuizoMaximo(BigDecimal.ZERO)
                 .breakevenPoint(BigDecimal.ZERO)
                 .pernasExecutadas(List.of())
                 .custoLiquido(BigDecimal.ZERO)
                 .premioLiquidoUnitario(BigDecimal.ZERO)
                 .ganhoMaximoStrikeUnitario(BigDecimal.ZERO)
                 .riscoMaximoTeoricoUnitario(BigDecimal.ZERO)
                 .lucroMaximoLiquidoTotal(BigDecimal.ZERO)
                 .riscoMaximoLiquidoTotal(BigDecimal.ZERO)
                 .relacaoRiscoRetornoLiquida(BigDecimal.ZERO)
                 .build();
    }

    // --- MÉTODO PRINCIPAL DA API (CÁLCULO MANUAL) ---
    @Override
    public SpreadResponse calcularSpread(SpreadRequest request) {

        String ativoSubjacente = request.ativoSubjacente();
        List<PernaSpread> pernas = request.pernas();

        // Tratamento de erro 1: Valores nulos essenciais
        if (request.cotacaoAtualAtivo() == null || request.taxasOperacionais() == null) {
            return createErrorResponse(
                         "ERRO: O SpreadRequest deve incluir a cotação atual do ativo e as taxas operacionais.",
                         "Erro");
        }

        // Tratamento de erro 2: Lista de pernas vazia
        if (pernas == null || pernas.isEmpty()) {
            return createErrorResponse(
                         "ERRO: A lista de pernas (opções) está vazia.",
                         "Erro");
        }

        // 1. OBTENDO DADOS E CALCULANDO CUSTO LÍQUIDO TOTAL
        BigDecimal precoAtualSubjacente = request.cotacaoAtualAtivo().setScale(SCALE, ROUNDING_MODE);

        List<PernaCalculada> pernasParaCalculo = new ArrayList<>();
        BigDecimal custoLiquidoTotal = BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE);
        BigDecimal premioLiquidoTotalBruto = BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE);
        // Taxa é por contrato/unidade
        BigDecimal taxaUnitario = request.taxasOperacionais().setScale(SCALE, ROUNDING_MODE);

        for (PernaSpread perna : pernas) {
            String ticker = perna.ticker();
            Optional<Option> optionOptional = optionRepository.findByTicker(ticker);

            // Tratamento de erro 3: Ticker não encontrado
            if (optionOptional.isEmpty()) {
                return createErrorResponse(
                         "ERRO: Ticker de opção não encontrado no banco de dados: " + ticker,
                         "Erro");
            }

            Option dadosOpcao = optionOptional.get();

            BigDecimal precoPremio = dadosOpcao.getPreco().setScale(SCALE, ROUNDING_MODE);
            BigDecimal quantidade = BigDecimal.valueOf(perna.quantidade());
            BigDecimal valorPernaBruto = precoPremio.multiply(quantidade).setScale(SCALE, ROUNDING_MODE);
            // Taxas são sempre um custo, por isso subtraímos do fluxo de caixa
            BigDecimal taxasTotalPerna = taxaUnitario.multiply(quantidade).abs().setScale(SCALE, ROUNDING_MODE);

            if ("COMPRA".equalsIgnoreCase(perna.operacao())) {
                // Saída de caixa (débito) + Custo da taxa
                custoLiquidoTotal = custoLiquidoTotal.subtract(valorPernaBruto).subtract(taxasTotalPerna);
                // Prêmio Bruto (Usado para métricas unitárias)
                premioLiquidoTotalBruto = premioLiquidoTotalBruto.subtract(valorPernaBruto);
            } else if ("VENDA".equalsIgnoreCase(perna.operacao())) {
                // Entrada de caixa (crédito) - Custo da taxa
                custoLiquidoTotal = custoLiquidoTotal.add(valorPernaBruto).subtract(taxasTotalPerna);
                // Prêmio Bruto (Usado para métricas unitárias)
                premioLiquidoTotalBruto = premioLiquidoTotalBruto.add(valorPernaBruto);
            } else {
                // Tratamento de erro 4: Operação inválida
                return createErrorResponse(
                         "ERRO: Ação inválida para o ticker " + ticker + ". Use 'COMPRA' ou 'VENDA'.",
                         "Erro");
            }

            // Adiciona a perna calculada para o Payoff
            pernasParaCalculo.add(new PernaCalculada(
                         ticker, dadosOpcao.getTipo(), dadosOpcao.getStrike().setScale(SCALE, ROUNDING_MODE),
                         perna.quantidade(), perna.operacao(), precoPremio));
        }

        // 2. CHAMADA DA LÓGICA DE PAYOFF BRUTO
        ResultadoPayoff resultadoBruto = this.calcularOtimizacao(pernasParaCalculo, custoLiquidoTotal,
                         precoAtualSubjacente, premioLiquidoTotalBruto);

        // 3. CALCULAR AS MÉTRICAS LÍQUIDAS TOTAIS

        BigDecimal lucroMaximoLiquidoTotal = calcularLucroMaximoLiquidoTotal(
                         resultadoBruto.lucroMaximoBruto(), resultadoBruto.premioLiquidoUnitario(),
                         resultadoBruto.ganhoMaximoStrikeUnitario());

        BigDecimal riscoMaximoLiquidoTotal = calcularRiscoMaximoLiquidoTotal(
                         resultadoBruto.prejuizoMaximoBruto().abs(),
                         resultadoBruto.riscoMaximoTeoricoUnitario());

        BigDecimal relacaoRiscoRetornoLiquida = calcularRelacaoRiscoRetornoLiquida(
                         lucroMaximoLiquidoTotal, riscoMaximoLiquidoTotal);

        // 4. Retorno Final (Ajustado ao novo Record SpreadResponse usando Builder)
        return SpreadResponse.builder()
                 .mensagem("Sucesso! Spread de " + ativoSubjacente + " calculado. Fluxo Inicial Líquido: R$"
                             + custoLiquidoTotal.setScale(2, ROUNDING_MODE))
                 .nomeEstrategia("Spread Manual")
                 .lucroMaximo(resultadoBruto.lucroMaximoBruto().setScale(2, ROUNDING_MODE))
                 .prejuizoMaximo(resultadoBruto.prejuizoMaximoBruto().abs().setScale(2, ROUNDING_MODE))
                 .breakevenPoint(resultadoBruto.breakevenPoint().setScale(2, ROUNDING_MODE))
                 .pernasExecutadas(pernas)
                 .custoLiquido(custoLiquidoTotal.setScale(2, ROUNDING_MODE))
                 // Campos UNITÁRIOS (com SCALE=4)
                 .premioLiquidoUnitario(resultadoBruto.premioLiquidoUnitario().setScale(SCALE, ROUNDING_MODE))
                 .ganhoMaximoStrikeUnitario(resultadoBruto.ganhoMaximoStrikeUnitario().setScale(SCALE, ROUNDING_MODE))
                 .riscoMaximoTeoricoUnitario(resultadoBruto.riscoMaximoTeoricoUnitario().setScale(SCALE, ROUNDING_MODE))
                 // Campos LÍQUIDOS TOTAIS (com SCALE=2 para exibição monetária)
                 .lucroMaximoLiquidoTotal(lucroMaximoLiquidoTotal.setScale(2, ROUNDING_MODE))
                 .riscoMaximoLiquidoTotal(riscoMaximoLiquidoTotal.setScale(2, ROUNDING_MODE))
                 .relacaoRiscoRetornoLiquida(relacaoRiscoRetornoLiquida.setScale(2, ROUNDING_MODE))
                 .build();
    }

    // --- FUNÇÃO AUXILIAR PARA PAYOFF UNITÁRIO (Refatoração) ---
    private BigDecimal calcularPayoffUnitario(String tipoOpcao, BigDecimal strike, BigDecimal precoSimulado) {
        if ("CALL".equalsIgnoreCase(tipoOpcao)) {
            // Payoff Call = Max(0, Preço - Strike)
            return precoSimulado.subtract(strike).max(BigDecimal.ZERO);
        } else if ("PUT".equalsIgnoreCase(tipoOpcao)) {
            // Payoff Put = Max(0, Strike - Preço)
            return strike.subtract(precoSimulado).max(BigDecimal.ZERO);
        }
        return BigDecimal.ZERO;
    }

    // --- FUNÇÃO RESPONSÁVEL PELA LÓGICA DE PAYOFF E OTIMIZAÇÃO (BRUTA) ---
    private ResultadoPayoff calcularOtimizacao(List<PernaCalculada> pernas, BigDecimal custoLiquido,
             BigDecimal precoAtualSubjacente, BigDecimal premioLiquidoTotalBruto) {

        // Inicializa com valores extremos para otimização
        BigDecimal lucroMaximo = new BigDecimal("-999999999.00").setScale(SCALE, ROUNDING_MODE);
        BigDecimal prejuizoMaximo = new BigDecimal("999999999.00").setScale(SCALE, ROUNDING_MODE);

        // Inicializa o Breakeven com o valor do Payoff da simulação
        BigDecimal breakevenPointSimulacao = BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE);
        BigDecimal menorDiferencaBreakeven = new BigDecimal("999999999.00").setScale(SCALE, ROUNDING_MODE);

        // Define o range de simulação (+/- 20% do preço atual)
        BigDecimal rangeSimulacao = precoAtualSubjacente.multiply(new BigDecimal("0.2")).setScale(SCALE, ROUNDING_MODE);

        BigDecimal precoMinimo = precoAtualSubjacente.subtract(rangeSimulacao).max(BigDecimal.ZERO).setScale(SCALE,
                 ROUNDING_MODE);
        BigDecimal precoMaximo = precoAtualSubjacente.add(rangeSimulacao).setScale(SCALE, ROUNDING_MODE);

        BigDecimal precoSimulado = precoMinimo;
        BigDecimal passoSimulacao = new BigDecimal("0.01").setScale(SCALE, ROUNDING_MODE);

        // Loop de simulação do Payoff
        while (precoSimulado.compareTo(precoMaximo) <= 0) {
            BigDecimal lucroTotalNoPonto = custoLiquido.negate();

            for (PernaCalculada perna : pernas) {

                // Refatoração: Uso do método auxiliar
                BigDecimal payoffPerna = calcularPayoffUnitario(perna.tipoOpcao(), perna.strike(), precoSimulado);

                BigDecimal quantidade = BigDecimal.valueOf(perna.quantidade());
                payoffPerna = payoffPerna.multiply(quantidade);

                // Inverte o sinal se for Venda (o payoff representa um PREJUÍZO para quem
                // vende)
                if ("VENDA".equalsIgnoreCase(perna.acao())) {
                    payoffPerna = payoffPerna.negate();
                }

                lucroTotalNoPonto = lucroTotalNoPonto.add(payoffPerna);
            }
            lucroTotalNoPonto = lucroTotalNoPonto.setScale(SCALE, ROUNDING_MODE);

            // Otimização de Lucro/Prejuízo Máximo
            if (lucroTotalNoPonto.compareTo(lucroMaximo) > 0) {
                lucroMaximo = lucroTotalNoPonto;
            }
            if (lucroTotalNoPonto.compareTo(prejuizoMaximo) < 0) {
                prejuizoMaximo = lucroTotalNoPonto;
            }

            // Otimização do Breakeven (ponto onde o lucro está mais próximo de zero)
            BigDecimal diferencaZero = lucroTotalNoPonto.abs();
            if (diferencaZero.compareTo(menorDiferencaBreakeven) < 0) {
                menorDiferencaBreakeven = diferencaZero;
                // Guarda o Breakeven encontrado pela simulação como fallback
                breakevenPointSimulacao = precoSimulado;
            }

            precoSimulado = precoSimulado.add(passoSimulacao).setScale(SCALE, ROUNDING_MODE);
        }

        // --- CÁLCULO DAS MÉTRICAS UNITÁRIAS (Para Spreads Verticais de 2 pernas) ---

        BigDecimal premioLiquidoUnitario = BigDecimal.ZERO;
        BigDecimal ganhoMaximoStrikeUnitario = BigDecimal.ZERO;
        BigDecimal riscoMaximoTeoricoUnitario = BigDecimal.ZERO;
        BigDecimal breakevenPointCorrigido = breakevenPointSimulacao; // Default é o da simulação

        // Verifica se é um Spread Vertical simples (2 pernas, mesma quantidade, mesmo
        // tipo)
        if (pernas.size() == 2 && pernas.get(0).quantidade() == pernas.get(1).quantidade()
                 && pernas.get(0).tipoOpcao().equals(pernas.get(1).tipoOpcao())) {

            PernaCalculada perna1 = pernas.get(0);
            PernaCalculada perna2 = pernas.get(1);
            int quantidade = perna1.quantidade();

            BigDecimal strike1 = perna1.strike();
            BigDecimal strike2 = perna2.strike();

            BigDecimal k_baixo = strike1.min(strike2);
            BigDecimal k_alto = strike1.max(strike2);

            // Prêmio Unitário Bruto (Prêmio Líquido Total Bruto / Quantidade)
            premioLiquidoUnitario = premioLiquidoTotalBruto.divide(BigDecimal.valueOf(quantidade), SCALE,
                     ROUNDING_MODE);
            // Diferença Unitária dos Strikes
            ganhoMaximoStrikeUnitario = k_alto.subtract(k_baixo);

            // Cálculo do Risco Máximo Teórico Unitário
            if (premioLiquidoUnitario.compareTo(BigDecimal.ZERO) < 0) {
                // Spread de Débito (Custo). Risco Máximo Teórico = Custo Unitário
                riscoMaximoTeoricoUnitario = premioLiquidoUnitario.abs();

                // CORREÇÃO DO BREAKEVEN PARA SPREAD DE DÉBITO (Call/Put Comprado K Menor)
                Optional<PernaCalculada> pernaCompra = pernas.stream()
                         .filter(p -> "COMPRA".equalsIgnoreCase(p.acao()))
                         .min(Comparator.comparing(PernaCalculada::strike)); 

                if (pernaCompra.isPresent()) {
                    BigDecimal custoUnitarioLiquido = custoLiquido.abs().divide(BigDecimal.valueOf(quantidade), SCALE,
                             ROUNDING_MODE);
                    breakevenPointCorrigido = pernaCompra.get().strike().add(custoUnitarioLiquido);
                }

            } else {
                // Spread de Crédito (Receita). Risco Máximo Teórico = Diferença Strikes -
                // Prêmio Unitário
                riscoMaximoTeoricoUnitario = ganhoMaximoStrikeUnitario.subtract(premioLiquidoUnitario);

                // CORREÇÃO DO BREAKEVEN PARA SPREAD DE CRÉDITO
                Optional<PernaCalculada> pernaVenda = pernas.stream()
                         .filter(p -> "VENDA".equalsIgnoreCase(p.acao()))
                         .min(Comparator.comparing(PernaCalculada::strike)); 

                if (pernaVenda.isPresent()) {
                    BigDecimal receitaUnitarioLiquido = custoLiquido.abs().divide(BigDecimal.valueOf(quantidade), SCALE,
                             ROUNDING_MODE);
                    // BULL PUT (PUT): Breakeven = Strike de Venda - Prêmio Unitário
                    if ("PUT".equalsIgnoreCase(pernaVenda.get().tipoOpcao())) {
                        breakevenPointCorrigido = pernaVenda.get().strike().subtract(receitaUnitarioLiquido);
                    } 
                    // BEAR CALL (CALL): Breakeven = Strike de Venda + Prêmio Unitário
                    else { 
                        breakevenPointCorrigido = pernaVenda.get().strike().add(receitaUnitarioLiquido);
                    }
                }
            }

            // CORREÇÃO DE PAYOFF LIMITADO (FINAL)
            BigDecimal diferencaStrikesTotal = ganhoMaximoStrikeUnitario
                     .multiply(BigDecimal.valueOf(pernas.get(0).quantidade()));

            if (custoLiquido.compareTo(BigDecimal.ZERO) < 0) {
                // Débito (Custo Líquido é negativo)
                lucroMaximo = diferencaStrikesTotal.subtract(custoLiquido.abs());
                prejuizoMaximo = custoLiquido; // Já é negativo (o custo)
            } else {
                // Crédito (Custo Líquido é positivo, ou seja, Receita)
                lucroMaximo = custoLiquido; // É o crédito recebido
                prejuizoMaximo = diferencaStrikesTotal.subtract(custoLiquido).negate(); // Negativo
            }
        }

        return new ResultadoPayoff(
                 lucroMaximo.setScale(SCALE, ROUNDING_MODE),
                 prejuizoMaximo.setScale(SCALE, ROUNDING_MODE),
                 breakevenPointCorrigido.setScale(SCALE, ROUNDING_MODE),
                 premioLiquidoUnitario,
                 ganhoMaximoStrikeUnitario,
                 riscoMaximoTeoricoUnitario);
    }

    // --- FUNÇÕES DE CÁLCULO LÍQUIDO TOTAIS ---

    private BigDecimal calcularLucroMaximoLiquidoTotal(BigDecimal lucroMaximoBruto, BigDecimal premioLiquidoUnitario,
             BigDecimal ganhoMaximoStrikeUnitario) {
        // Refatoração: Usar BigDecimal.valueOf() para int
        if (premioLiquidoUnitario.compareTo(BigDecimal.ZERO) >= 0) {
            // CRÉDITO: Prêmio Total Bruto (unitário * contratos) - Taxas TOTAIS
            BigDecimal premioTotalBruto = premioLiquidoUnitario.multiply(BigDecimal.valueOf(QUANTIDADE_CONTRATOS));
            return arredondarParaMoeda(premioTotalBruto.subtract(TAXAS_TOTAIS_OPERACAO));
        } else {
            // DÉBITO: (Ganho Strike Total) + (Custo Total (negativo)) - Taxas TOTAIS
            BigDecimal ganhoMaxStrikeTotal = ganhoMaximoStrikeUnitario
                     .multiply(BigDecimal.valueOf(QUANTIDADE_CONTRATOS));
            BigDecimal premioTotalBruto = premioLiquidoUnitario.multiply(BigDecimal.valueOf(QUANTIDADE_CONTRATOS));

            return arredondarParaMoeda(ganhoMaxStrikeTotal.add(premioTotalBruto).subtract(TAXAS_TOTAIS_OPERACAO));
        }
    }

    private BigDecimal calcularRiscoMaximoLiquidoTotal(BigDecimal riscoBrutoTotal,
             BigDecimal riscoMaximoTeoricoUnitario) {

        // Risco Total = (Risco Teórico Unitário * Qtd) + Taxas TOTAIS
        // Refatoração: Usar BigDecimal.valueOf() para int
        BigDecimal riscoBrutoTeoricoTotal = riscoMaximoTeoricoUnitario
                 .multiply(BigDecimal.valueOf(QUANTIDADE_CONTRATOS));

        return arredondarParaMoeda(riscoBrutoTeoricoTotal.add(TAXAS_TOTAIS_OPERACAO));
    }

    private BigDecimal calcularRelacaoRiscoRetornoLiquida(BigDecimal lucroMaximoLiquidoTotal,
             BigDecimal riscoMaximoLiquidoTotal) {
        if (riscoMaximoLiquidoTotal.compareTo(BigDecimal.ZERO) > 0) {
            return arredondar(lucroMaximoLiquidoTotal.divide(
                     riscoMaximoLiquidoTotal,
                     6,
                     SpreadFinanceiroUtils.MODO_ARREDONDAMENTO));
        } else {
            return BigDecimal.ZERO;
        }
    }

    // =============================================================================
    // --- MÉTODOS DE OTIMIZAÇÃO (Busca no DB - IMPLEMENTAÇÃO REAL) ---
    // =============================================================================

    /**
     * Otimiza o Bull Call Spread (Compra CALL K baixo, Venda CALL K alto - Débito).
     * Busca em todos os vencimentos e encontra o par com a melhor Relação Risco/Retorno Líquida.
     */
    public SpreadResponse otimizarBullCallSpread(String ativoSubjacente, BigDecimal cotacaoAtualAtivo,
            BigDecimal taxasOperacionais) {
        
        List<LocalDate> vencimentos = optionRepository.findDistinctVencimentoByIdAcaoOrderByVencimentoAsc(ativoSubjacente);
        
        if (vencimentos.isEmpty()) {
            return createErrorResponse("ERRO: Nenhuma data de vencimento encontrada para o ativo " + ativoSubjacente, "Bull Call Spread");
        }

        SpreadResponse melhorSpread = createErrorResponse("Nenhuma combinação de Bull Call Spread válida encontrada.", "Bull Call Spread");
        BigDecimal melhorRiscoRetorno = BigDecimal.ZERO;

        // Itera sobre CADA vencimento
        for (LocalDate vencimento : vencimentos) {
            // Busca todas as CALLs para este vencimento
            List<Option> callsNoVencimento = optionRepository.findByIdAcaoAndVencimentoAndTipo(ativoSubjacente, vencimento, "CALL");
            
            if (callsNoVencimento.size() < 2) continue;

            // Loop Exaustivo (Brute-Force) para encontrar o melhor par
            for (Option compraPerna : callsNoVencimento) {
                for (Option vendaPerna : callsNoVencimento) {

                    // Critério Bull Call: Compra K Baixo < Venda K Alto
                    if (compraPerna.getStrike().compareTo(vendaPerna.getStrike()) < 0) {
                        
                        List<PernaSpread> pernas = List.of(
                            new PernaSpread(compraPerna.getTicker(), QUANTIDADE_CONTRATOS, "COMPRA"),
                            new PernaSpread(vendaPerna.getTicker(), QUANTIDADE_CONTRATOS, "VENDA")
                        );

                        SpreadRequest request = new SpreadRequest(
                            ativoSubjacente, cotacaoAtualAtivo, taxasOperacionais, pernas);

                        SpreadResponse candidato = calcularSpread(request);

                        if (!"Erro".equals(candidato.getNomeEstrategia()) && candidato.getRelacaoRiscoRetornoLiquida().compareTo(melhorRiscoRetorno) > 0) {
                            melhorRiscoRetorno = candidato.getRelacaoRiscoRetornoLiquida();
                            melhorSpread = candidato.toBuilder().vencimento(vencimento).build(); // Guarda o vencimento
                        }
                    }
                }
            }
        }

        // Retorno Final
        if (melhorRiscoRetorno.compareTo(BigDecimal.ZERO) > 0) {
            String novaMensagem = String.format(
                "SUCESSO: Melhor Bull Call Spread encontrado (Vencimento: %s). Relação R/R: %s.",
                melhorSpread.getVencimento().toString(), 
                melhorSpread.getRelacaoRiscoRetornoLiquida().setScale(2, ROUNDING_MODE));

            return melhorSpread.toBuilder().mensagem(novaMensagem).build();
        } else {
            return melhorSpread; 
        }
    }

    /**
     * * Otimiza o Bear Put Spread (Compra Put K alto, Venda Put K baixo - Débito).
     * Objetivo: Maximizar a Relação Risco/Retorno Líquida.
     */
    public SpreadResponse otimizarBearPutSpread(String ativoSubjacente, BigDecimal cotacaoAtualAtivo,
            BigDecimal taxasOperacionais) {
        
        List<LocalDate> vencimentos = optionRepository.findDistinctVencimentoByIdAcaoOrderByVencimentoAsc(ativoSubjacente);
        if (vencimentos.isEmpty()) {
            return createErrorResponse("ERRO: Nenhuma data de vencimento encontrada para o ativo " + ativoSubjacente, "Bear Put Spread");
        }

        SpreadResponse melhorSpread = createErrorResponse("Nenhuma combinação de Bear Put Spread válida encontrada.", "Bear Put Spread");
        BigDecimal melhorRiscoRetorno = BigDecimal.ZERO;

        for (LocalDate vencimento : vencimentos) {
            List<Option> putsNoVencimento = optionRepository.findByIdAcaoAndVencimentoAndTipo(ativoSubjacente, vencimento, "PUT");
            
            if (putsNoVencimento.size() < 2) continue;

            for (Option compraPerna : putsNoVencimento) { // Compra é no K Alto
                for (Option vendaPerna : putsNoVencimento) { // Venda é no K Baixo

                    // Critério Bear Put: Compra K Alto > Venda K Baixo
                    if (compraPerna.getStrike().compareTo(vendaPerna.getStrike()) > 0) {
                        
                        List<PernaSpread> pernas = List.of(
                            new PernaSpread(compraPerna.getTicker(), QUANTIDADE_CONTRATOS, "COMPRA"),
                            new PernaSpread(vendaPerna.getTicker(), QUANTIDADE_CONTRATOS, "VENDA")
                        );

                        SpreadRequest request = new SpreadRequest(ativoSubjacente, cotacaoAtualAtivo, taxasOperacionais, pernas);
                        SpreadResponse candidato = calcularSpread(request);

                        if (!"Erro".equals(candidato.getNomeEstrategia()) && candidato.getRelacaoRiscoRetornoLiquida().compareTo(melhorRiscoRetorno) > 0) {
                            melhorRiscoRetorno = candidato.getRelacaoRiscoRetornoLiquida();
                            melhorSpread = candidato.toBuilder().vencimento(vencimento).build();
                        }
                    }
                }
            }
        }
        
        if (melhorRiscoRetorno.compareTo(BigDecimal.ZERO) > 0) {
            String novaMensagem = String.format(
                "SUCESSO: Melhor Bear Put Spread encontrado (Vencimento: %s). Relação R/R: %s.",
                melhorSpread.getVencimento().toString(), 
                melhorSpread.getRelacaoRiscoRetornoLiquida().setScale(2, ROUNDING_MODE));

            return melhorSpread.toBuilder().mensagem(novaMensagem).build();
        } else {
            return melhorSpread;
        }
    }
    
    /**
     * * Otimiza o Bull Put Spread (Venda PUT K alto, Compra PUT K baixo - Crédito).
     * Objetivo: Maximizar a Relação Risco/Retorno Líquida.
     */
    public SpreadResponse otimizarBullPutSpread(String ativoSubjacente, BigDecimal cotacaoAtualAtivo,
            BigDecimal taxasOperacionais) {
        
        List<LocalDate> vencimentos = optionRepository.findDistinctVencimentoByIdAcaoOrderByVencimentoAsc(ativoSubjacente);
        if (vencimentos.isEmpty()) {
            return createErrorResponse("ERRO: Nenhuma data de vencimento encontrada para o ativo " + ativoSubjacente, "Bull Put Spread");
        }

        SpreadResponse melhorSpread = createErrorResponse("Nenhuma combinação de Bull Put Spread válida encontrada.", "Bull Put Spread");
        BigDecimal melhorRiscoRetorno = BigDecimal.ZERO;

        for (LocalDate vencimento : vencimentos) {
            List<Option> putsNoVencimento = optionRepository.findByIdAcaoAndVencimentoAndTipo(ativoSubjacente, vencimento, "PUT");
            
            if (putsNoVencimento.size() < 2) continue;

            for (Option vendaPerna : putsNoVencimento) { // Venda é no K Alto
                for (Option compraPerna : putsNoVencimento) { // Compra é no K Baixo

                    // Critério Bull Put: Venda K Alto > Compra K Baixo
                    if (vendaPerna.getStrike().compareTo(compraPerna.getStrike()) > 0) {
                        
                        List<PernaSpread> pernas = List.of(
                            new PernaSpread(vendaPerna.getTicker(), QUANTIDADE_CONTRATOS, "VENDA"),
                            new PernaSpread(compraPerna.getTicker(), QUANTIDADE_CONTRATOS, "COMPRA")
                        );

                        SpreadRequest request = new SpreadRequest(ativoSubjacente, cotacaoAtualAtivo, taxasOperacionais, pernas);
                        SpreadResponse candidato = calcularSpread(request);

                        if (!"Erro".equals(candidato.getNomeEstrategia()) && candidato.getRelacaoRiscoRetornoLiquida().compareTo(melhorRiscoRetorno) > 0) {
                            melhorRiscoRetorno = candidato.getRelacaoRiscoRetornoLiquida();
                            melhorSpread = candidato.toBuilder().vencimento(vencimento).build();
                        }
                    }
                }
            }
        }
        
        if (melhorRiscoRetorno.compareTo(BigDecimal.ZERO) > 0) {
            String novaMensagem = String.format(
                "SUCESSO: Melhor Bull Put Spread encontrado (Vencimento: %s). Relação R/R: %s.",
                melhorSpread.getVencimento().toString(), 
                melhorSpread.getRelacaoRiscoRetornoLiquida().setScale(2, ROUNDING_MODE));

            return melhorSpread.toBuilder().mensagem(novaMensagem).build();
        } else {
            return melhorSpread;
        }
    }

    /**
     * * Otimiza o Bear Call Spread (Venda CALL K baixo, Compra CALL K alto - Crédito).
     * Objetivo: Maximizar a Relação Risco/Retorno Líquida.
     */
    public SpreadResponse otimizarBearCallSpread(String ativoSubjacente, BigDecimal cotacaoAtualAtivo,
            BigDecimal taxasOperacionais) {
        
        List<LocalDate> vencimentos = optionRepository.findDistinctVencimentoByIdAcaoOrderByVencimentoAsc(ativoSubjacente);
        if (vencimentos.isEmpty()) {
            return createErrorResponse("ERRO: Nenhuma data de vencimento encontrada para o ativo " + ativoSubjacente, "Bear Call Spread");
        }

        SpreadResponse melhorSpread = createErrorResponse("Nenhuma combinação de Bear Call Spread válida encontrada.", "Bear Call Spread");
        BigDecimal melhorRiscoRetorno = BigDecimal.ZERO;

        for (LocalDate vencimento : vencimentos) {
            List<Option> callsNoVencimento = optionRepository.findByIdAcaoAndVencimentoAndTipo(ativoSubjacente, vencimento, "CALL");
            
            if (callsNoVencimento.size() < 2) continue;

            for (Option vendaPerna : callsNoVencimento) { // Venda é no K Baixo
                for (Option compraPerna : callsNoVencimento) { // Compra é no K Alto

                    // Critério Bear Call: Venda K Baixo < Compra K Alto
                    if (vendaPerna.getStrike().compareTo(compraPerna.getStrike()) < 0) {
                        
                        List<PernaSpread> pernas = List.of(
                            new PernaSpread(vendaPerna.getTicker(), QUANTIDADE_CONTRATOS, "VENDA"),
                            new PernaSpread(compraPerna.getTicker(), QUANTIDADE_CONTRATOS, "COMPRA")
                        );

                        SpreadRequest request = new SpreadRequest(ativoSubjacente, cotacaoAtualAtivo, taxasOperacionais, pernas);
                        SpreadResponse candidato = calcularSpread(request);

                        if (!"Erro".equals(candidato.getNomeEstrategia()) && candidato.getRelacaoRiscoRetornoLiquida().compareTo(melhorRiscoRetorno) > 0) {
                            melhorRiscoRetorno = candidato.getRelacaoRiscoRetornoLiquida();
                            melhorSpread = candidato.toBuilder().vencimento(vencimento).build();
                        }
                    }
                }
            }
        }
        
        if (melhorRiscoRetorno.compareTo(BigDecimal.ZERO) > 0) {
            String novaMensagem = String.format(
                "SUCESSO: Melhor Bear Call Spread encontrado (Vencimento: %s). Relação R/R: %s.",
                melhorSpread.getVencimento().toString(), 
                melhorSpread.getRelacaoRiscoRetornoLiquida().setScale(2, ROUNDING_MODE));

            return melhorSpread.toBuilder().mensagem(novaMensagem).build();
        } else {
            return melhorSpread;
        }
    }

    // --- MÉTODO DE OTIMIZAÇÃO UNIFICADA (BUSCA AUTOMÁTICA) ---

    /**
     * Otimiza e compara as 4 principais estratégias direcionais (2 de Alta, 2 de
     * Baixa)
     * e retorna a que oferecer o maior Lucro Máximo Líquido Total.
     */
    public SpreadResponse otimizarMelhorEstrategia(String ativoSubjacente, BigDecimal cotacaoAtualAtivo,
             BigDecimal taxasOperacionais) {

        // 1. Otimizar as 4 estratégias individuais
        SpreadResponse bullCall = otimizarBullCallSpread(ativoSubjacente, cotacaoAtualAtivo, taxasOperacionais);
        SpreadResponse bearPut = otimizarBearPutSpread(ativoSubjacente, cotacaoAtualAtivo, taxasOperacionais);
        SpreadResponse bullPut = otimizarBullPutSpread(ativoSubjacente, cotacaoAtualAtivo, taxasOperacionais);
        SpreadResponse bearCall = otimizarBearCallSpread(ativoSubjacente, cotacaoAtualAtivo, taxasOperacionais);

        // 2. Colocar todas em uma lista para comparação
        List<SpreadResponse> resultados = List.of(bullCall, bearPut, bullPut, bearCall);

        // 3. Encontrar a melhor estratégia: a com maior Lucro Máximo Líquido Total
        Optional<SpreadResponse> melhorResultado = resultados.stream()
                 .filter(r -> !"Erro".equals(r.getNomeEstrategia())) // Filtra os erros de busca/estrutura
                 .max(Comparator.comparing(SpreadResponse::getLucroMaximoLiquidoTotal));

        // 4. Retornar o melhor resultado
        if (melhorResultado.isPresent()) {
            SpreadResponse melhor = melhorResultado.get();

            String novaMensagem = String.format(
                 "SUCESSO: A melhor de 4 Estratégias Encontrada é o %s (Vencimento: %s). Lucro Máximo Líquido: R$%s.",
                 melhor.getNomeEstrategia(), melhor.getVencimento().toString(), melhor.getLucroMaximoLiquidoTotal().setScale(2, ROUNDING_MODE));

            return melhor.toBuilder().mensagem(novaMensagem).build();
        } else {
            // Tratamento de erro 5: Nenhuma estratégia otimizada
            return createErrorResponse(
                 "ERRO: Nenhuma estratégia de spread vertical válida pôde ser otimizada para " + ativoSubjacente,
                 "Erro");
        }
    }
}