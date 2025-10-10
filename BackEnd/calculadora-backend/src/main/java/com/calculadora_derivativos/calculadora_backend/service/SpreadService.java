package com.calculadora_derivativos.calculadora_backend.service;

import com.calculadora_derivativos.calculadora_backend.model.Opcao;
import com.calculadora_derivativos.calculadora_backend.model.Ativo; 
import com.calculadora_derivativos.calculadora_backend.repository.OpcaoRepository; 
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

    private final OpcaoRepository opcaoRepository;
    private final AtivoRepository ativoRepository; 

    // Constante para arredondamento, comum em cálculos financeiros
    private static final int SCALE = 4;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    // --- CLASSES AUXILIARES (Record) ---
    private record PernaCalculada(
        String ticker, String tipoOpcao, BigDecimal strike, int quantidade, 
        String acao, BigDecimal premio
    ) {}

    private record ResultadoPayoff(
        BigDecimal lucroMaximo, BigDecimal prejuizoMaximo, BigDecimal breakevenPoint
    ) {}

    // --- CONSTRUTOR ATUALIZADO (Injetando ambos os Repositórios) ---
    public SpreadService(OpcaoRepository opcaoRepository, AtivoRepository ativoRepository) {
        this.opcaoRepository = opcaoRepository; 
        this.ativoRepository = ativoRepository;
    }

    // --- MÉTODO PRINCIPAL DA API ---
    public SpreadResponse calcularSpread(SpreadRequest request) { 
        
        String ativoSubjacente = request.getAtivoSubjacente(); 
        List<PernaSpread> pernas = request.getPernas();
        
        if (pernas == null || pernas.isEmpty()) {
            return new SpreadResponse("ERRO: A lista de pernas (opções) está vazia.", 
                                     BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        // --- BUSCA DO PREÇO ATUAL DO ATIVO SUBJACENTE ---
        Optional<Ativo> ativoOptional = ativoRepository.findByCodigo(ativoSubjacente);
        
        if (ativoOptional.isEmpty()) {
            return new SpreadResponse("ERRO: Preço atual do ativo subjacente (" + ativoSubjacente + ") não encontrado.", 
                                     BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        
        // Conversão segura do preço (assumindo que getPrecoAtual retorna um BigDecimal)
        BigDecimal precoAtualSubjacente = ativoOptional.get().getPrecoAtual().setScale(SCALE, ROUNDING_MODE);

        // --- INÍCIO DO CÁLCULO DE CUSTO LÍQUIDO ---
        List<PernaCalculada> pernasParaCalculo = new ArrayList<>();
        // O custo líquido deve ser inicializado como ZERO
        BigDecimal custoLiquidoTotal = BigDecimal.ZERO; 
        
        for (PernaSpread perna : pernas) {
            String ticker = perna.getTicker();
            Optional<Opcao> opcaoOptional = opcaoRepository.findByCodigo(ticker);
            
            if (opcaoOptional.isEmpty()) {
                return new SpreadResponse(
                    "ERRO: Ticker de opção não encontrado no banco de dados: " + ticker, 
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
                );
            }
            
            Opcao dadosOpcao = opcaoOptional.get();
            // O prêmio da opção deve ser o preco na Entity
            BigDecimal precoPremio = dadosOpcao.getPreco().setScale(SCALE, ROUNDING_MODE); 
            
            // Cálculo do Custo Líquido
            BigDecimal valorPerna = precoPremio.multiply(new BigDecimal(perna.getQuantidade()));
            
            // CORREÇÃO CRÍTICA: Trocando getAcao() por getOperacao()
            if ("COMPRA".equalsIgnoreCase(perna.getOperacao())) { 
                // COMPRA (PAGAMENTO) reduz o custo líquido total
                custoLiquidoTotal = custoLiquidoTotal.subtract(valorPerna);
            } else if ("VENDA".equalsIgnoreCase(perna.getOperacao())) { // <--- CORREÇÃO AQUI
                // VENDA (RECEBIMENTO) aumenta o custo líquido total
                custoLiquidoTotal = custoLiquidoTotal.add(valorPerna);
            } else {
                return new SpreadResponse("ERRO: Ação inválida para o ticker " + ticker + ". Use 'COMPRA' ou 'VENDA'.", 
                                         BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            }

            // A perna calculada registra a ação para o cálculo de Payoff
            pernasParaCalculo.add(new PernaCalculada(
                ticker, dadosOpcao.getTipoOpcao(), 
                dadosOpcao.getStrike().setScale(SCALE, ROUNDING_MODE), // Usando o Strike correto
                perna.getQuantidade(), perna.getOperacao(), precoPremio // Usando getOperacao() para a record
            ));
        }
        
        // 2. CHAMADA DA LÓGICA DE PAYOFF (passando o preço atual)
        ResultadoPayoff resultado = this.calcularOtimizacao(pernasParaCalculo, custoLiquidoTotal, precoAtualSubjacente);
        
        // 3. Retorno Final
        return new SpreadResponse(
            "Sucesso! Spread de " + ativoSubjacente + " calculado. Custo Líquido: " + custoLiquidoTotal.setScale(2, ROUNDING_MODE), 
            resultado.lucroMaximo(), 
            resultado.prejuizoMaximo(),
            resultado.breakevenPoint() 
        );
    }
    
    // --- FUNÇÃO RESPONSÁVEL PELA LÓGICA DE PAYOFF E OTIMIZAÇÃO ---
    private ResultadoPayoff calcularOtimizacao(List<PernaCalculada> pernas, BigDecimal custoLiquido, BigDecimal precoAtualSubjacente) {
        
        BigDecimal lucroMaximo = new BigDecimal("-999999999.00");
        BigDecimal prejuizoMaximo = new BigDecimal("999999999.00");
        BigDecimal breakevenPoint = BigDecimal.ZERO;
        BigDecimal menorDiferencaBreakeven = new BigDecimal("999999999.00");
        
        // 1. Definir o Intervalo de Simulação
        BigDecimal rangeSimulacao = precoAtualSubjacente.multiply(new BigDecimal("0.2")); // +/- 20%
        
        BigDecimal precoMinimo = precoAtualSubjacente.subtract(rangeSimulacao).max(BigDecimal.ZERO); 
        BigDecimal precoMaximo = precoAtualSubjacente.add(rangeSimulacao);
        
        // 2. Iterar sobre os preços simulados
        BigDecimal precoSimulado = precoMinimo;
        BigDecimal passoSimulacao = new BigDecimal("0.01"); // Passo de 1 centavo
        
        while (precoSimulado.compareTo(precoMaximo) <= 0) {
            
            // O lucro inicial é o negativo do custo líquido (custo que se paga para montar)
            BigDecimal lucroTotalNoPonto = custoLiquido.negate(); 
            
            // CÁLCULO DO PAYOFF DE CADA PERNA
            for (PernaCalculada perna : pernas) {
                
                BigDecimal payoffPerna;
                
                // Diferença entre o preço simulado e o strike (S - K)
                BigDecimal diferencaStrike = precoSimulado.subtract(perna.strike()).setScale(SCALE, ROUNDING_MODE);
                
                if ("CALL".equalsIgnoreCase(perna.tipoOpcao())) {
                    // Payoff da CALL: max(S - K, 0)
                    payoffPerna = diferencaStrike.max(BigDecimal.ZERO);
                } else { // PUT
                    // Payoff da PUT: max(K - S, 0)
                    payoffPerna = perna.strike().subtract(precoSimulado).max(BigDecimal.ZERO);
                }
                
                // Multiplica o Payoff pelo número de contratos
                payoffPerna = payoffPerna.multiply(new BigDecimal(perna.quantidade()));
                
                // Se a perna for de VENDA, o Payoff é negativo (você paga)
                if ("VENDA".equalsIgnoreCase(perna.acao())) {
                    payoffPerna = payoffPerna.negate();
                }

                lucroTotalNoPonto = lucroTotalNoPonto.add(payoffPerna);
            }
            
            // ATUALIZAR MÁXIMOS, MÍNIMOS E BREAKEVEN
            if (lucroTotalNoPonto.compareTo(lucroMaximo) > 0) {
                lucroMaximo = lucroTotalNoPonto;
            }
            if (lucroTotalNoPonto.compareTo(prejuizoMaximo) < 0) {
                prejuizoMaximo = lucroTotalNoPonto;
            }
            
            // Busca o Breakeven Point (o ponto onde o lucro está mais próximo de zero)
            BigDecimal diferencaZero = lucroTotalNoPonto.abs();
            if (diferencaZero.compareTo(menorDiferencaBreakeven) < 0) {
                menorDiferencaBreakeven = diferencaZero;
                breakevenPoint = precoSimulado;
            }

            // Move para o próximo ponto simulado
            precoSimulado = precoSimulado.add(passoSimulacao).setScale(SCALE, ROUNDING_MODE);
        }
        
        return new ResultadoPayoff(
            lucroMaximo.setScale(2, ROUNDING_MODE),
            prejuizoMaximo.setScale(2, ROUNDING_MODE),
            breakevenPoint.setScale(2, ROUNDING_MODE) 
        );
    }
}