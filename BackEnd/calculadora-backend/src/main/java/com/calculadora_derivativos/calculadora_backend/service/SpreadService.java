package com.calculadora_derivativos.calculadora_backend.service;

import com.calculadora_derivativos.calculadora_backend.model.Option;
import com.calculadora_derivativos.calculadora_backend.model.Ativo; 
import com.calculadora_derivativos.calculadora_backend.repository.OptionRepository; 
import com.calculadora_derivativos.calculadora_backend.repository.AtivoRepository; 
import com.calculadora_derivativos.calculadora_backend.dto.SpreadRequest; 
import com.calculadora_derivativos.calculadora_backend.dto.SpreadResponse; 
import com.calculadora_derivativos.calculadora_backend.dto.PernaSpread; 
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional; 

@Service
public class SpreadService {

    private final OptionRepository optionRepository;
    private final AtivoRepository ativoRepository;

    // Constante para arredondamento, comum em cálculos financeiros
    private static final int SCALE = 4;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    // --- CLASSES AUXILIARES (Record) ---
    private record PernaCalculada(
        String ticker, String tipoOpcao, BigDecimal strike, int quantidade,
        String acao, BigDecimal preco // Renomeado para 'preco'
    ) {}

    private record ResultadoPayoff(
        BigDecimal lucroMaximo, BigDecimal prejuizoMaximo, BigDecimal breakevenPoint
    ) {}

    // --- CONSTRUTOR ATUALIZADO (Injetando OptionRepository e AtivoRepository) ---
    public SpreadService(OptionRepository optionRepository, AtivoRepository ativoRepository) {
        this.optionRepository = optionRepository;
        this.ativoRepository = ativoRepository;
    }

    // --- MÉTODO PRINCIPAL DA API ---
    public SpreadResponse calcularSpread(SpreadRequest request) {

        String ativoSubjacente = request.getAtivoSubjacente();
        List<PernaSpread> pernas = request.getPernas();

        // Validação do Request
        if (request.getCotacaoAtualAtivo() == null || request.getTaxasOperacionais() == null) {
            return new SpreadResponse("ERRO: O SpreadRequest deve incluir a cotação atual do ativo e as taxas operacionais.",
                                     BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        if (pernas == null || pernas.isEmpty()) {
            return new SpreadResponse("ERRO: A lista de pernas (opções) está vazia.",
                                     BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        // 1. OBTENDO DADOS
        // O preço da Request (cotacaoAtualAtivo) é o que será usado no cálculo do payoff.
        BigDecimal precoAtualSubjacente = request.getCotacaoAtualAtivo().setScale(SCALE, ROUNDING_MODE);

        // --- INÍCIO DO CÁLCULO DE CUSTO LÍQUIDO ---
        List<PernaCalculada> pernasParaCalculo = new ArrayList<>();

        // O custo líquido (fluxo de caixa inicial) deve ser inicializado como ZERO
        BigDecimal custoLiquidoTotal = BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE); // AJUSTE DE ESCALA
        BigDecimal taxaUnitario = request.getTaxasOperacionais().setScale(SCALE, ROUNDING_MODE);

        for (PernaSpread perna : pernas) {
            String ticker = perna.getTicker();

            // Busca a opção (RETORNA OPTIONAL)
            Optional<Option> optionOptional = optionRepository.findByTicker(ticker);

            if (optionOptional.isEmpty()) {
                return new SpreadResponse(
                    "ERRO: Ticker de opção não encontrado no banco de dados: " + ticker,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
                );
            }

            Option dadosOpcao = optionOptional.get();

            // ** LINHA CORRIGIDA: Usando getPreco() em vez de getPremioPct() **
            // O campo 'preco' substituiu 'premioPct' na Entidade Option
            BigDecimal precoPremio = dadosOpcao.getPreco().setScale(SCALE, ROUNDING_MODE); 
            // ** FIM DA CORREÇÃO **
            
            BigDecimal quantidade = new BigDecimal(perna.getQuantidade());

            // Cálculo do Custo/Crédito Bruto (Prêmio * Quantidade)
            BigDecimal valorPernaBruto = precoPremio.multiply(quantidade).setScale(SCALE, ROUNDING_MODE); // AJUSTE DE ESCALA

            // Cálculo do Custo de Taxas (Taxa Unitário * Quantidade)
            // Taxas sempre são um custo (Subtrai o valor final do spread)
            BigDecimal taxasTotalPerna = taxaUnitario.multiply(quantidade).abs().setScale(SCALE, ROUNDING_MODE); // AJUSTE DE ESCALA

            // LÓGICA DO FLUXO DE CAIXA INICIAL (Custo Líquido Total)
            if ("COMPRA".equalsIgnoreCase(perna.getOperacao())) {
                // COMPRA (PAGAMENTO): CUSTO (sai do caixa)
                // Subtrai o prêmio e subtrai a taxa (ambos são saída de caixa)
                custoLiquidoTotal = custoLiquidoTotal
                    .subtract(valorPernaBruto)
                    .subtract(taxasTotalPerna);
            } else if ("VENDA".equalsIgnoreCase(perna.getOperacao())) {
                // VENDA (RECEBIMENTO): CRÉDITO (entra no caixa)
                // Adiciona o prêmio e subtrai a taxa (entrada de caixa - saída de caixa)
                custoLiquidoTotal = custoLiquidoTotal
                    .add(valorPernaBruto)
                    .subtract(taxasTotalPerna);
            } else {
                return new SpreadResponse("ERRO: Ação inválida para o ticker " + ticker + ". Use 'COMPRA' ou 'VENDA'.",
                                         BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            }

            // A perna calculada registra a ação para o cálculo de Payoff
            pernasParaCalculo.add(new PernaCalculada(
                ticker,
                dadosOpcao.getTipo(), // Tipo: CALL ou PUT
                dadosOpcao.getStrike().setScale(SCALE, ROUNDING_MODE),
                perna.getQuantidade(),
                perna.getOperacao(), // Ação: COMPRA ou VENDA
                precoPremio // O prêmio da perna é o 'preco'
            ));
        }

        // 2. CHAMADA DA LÓGICA DE PAYOFF
        ResultadoPayoff resultado = this.calcularOtimizacao(pernasParaCalculo, custoLiquidoTotal, precoAtualSubjacente);

        // 3. Retorno Final
        return new SpreadResponse(
            "Sucesso! Spread de " + ativoSubjacente + " calculado. Fluxo Inicial Líquido: " + custoLiquidoTotal.setScale(2, ROUNDING_MODE),
            resultado.lucroMaximo(),
            resultado.prejuizoMaximo(),
            resultado.breakevenPoint()
        );
    }

    // --- FUNÇÃO RESPONSÁVEL PELA LÓGICA DE PAYOFF E OTIMIZAÇÃO ---
    private ResultadoPayoff calcularOtimizacao(List<PernaCalculada> pernas, BigDecimal custoLiquido, BigDecimal precoAtualSubjacente) {

        // O lucro inicial é o NEGATIVO do fluxo de caixa inicial (Custos = lucro positivo; Créditos = lucro negativo)
        // Isso garante que o resultado do Payoff seja o LUCRO FINAL (Payoff - Custo Inicial)

        // AJUSTE DE ESCALA NA INICIALIZAÇÃO
        BigDecimal lucroMaximo = new BigDecimal("-999999999.00").setScale(SCALE, ROUNDING_MODE);
        BigDecimal prejuizoMaximo = new BigDecimal("999999999.00").setScale(SCALE, ROUNDING_MODE);
        BigDecimal breakevenPoint = BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE);
        BigDecimal menorDiferencaBreakeven = new BigDecimal("999999999.00").setScale(SCALE, ROUNDING_MODE);

        // 1. Definir o Intervalo de Simulação (+/- 20% do preço atual)
        BigDecimal rangeSimulacao = precoAtualSubjacente.multiply(new BigDecimal("0.2")).setScale(SCALE, ROUNDING_MODE);

        BigDecimal precoMinimo = precoAtualSubjacente.subtract(rangeSimulacao).max(BigDecimal.ZERO).setScale(SCALE, ROUNDING_MODE);
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
                    // AJUSTE DE ESCALA (A garantia de escala é feita no setScale final)
                    payoffPerna = precoSimulado.subtract(perna.strike()).max(BigDecimal.ZERO);
                } else { // PUT
                    // Put: max(K - S, 0)
                    // AJUSTE DE ESCALA (A garantia de escala é feita no setScale final)
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

            // ATUALIZAR MÁXIMOS, MÍNIMOS E BREAKEVEN
            if (lucroTotalNoPonto.compareTo(lucroMaximo) > 0) {
                lucroMaximo = lucroTotalNoPonto;
            }
            if (lucroTotalNoPonto.compareTo(prejuizoMaximo) < 0) {
                prejuizoMaximo = lucroTotalNoPonto;
            }

            // Busca o Breakeven Point (o ponto onde o lucro está mais próximo de zero)
            BigDecimal diferencaZero = lucroTotalNoPonto.abs();
            // Comparação com a menor diferença já encontrada (para achar o ponto mais próximo de 0)
            if (diferencaZero.compareTo(menorDiferencaBreakeven) < 0) {
                menorDiferencaBreakeven = diferencaZero;
                breakevenPoint = precoSimulado;
            }

            // Move para o próximo ponto simulado
            precoSimulado = precoSimulado.add(passoSimulacao).setScale(SCALE, ROUNDING_MODE);
        }

        return new ResultadoPayoff(
            lucroMaximo.setScale(2, ROUNDING_MODE),
            // Retorna o valor ABSOLUTO do prejuízo máximo (que será um valor negativo no cálculo)
            prejuizoMaximo.negate().abs().setScale(2, ROUNDING_MODE), // Usa negate() ou abs() para garantir que o retorno seja o valor positivo do prejuízo.
            breakevenPoint.setScale(2, ROUNDING_MODE)
        );
    }
}