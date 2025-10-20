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
import com.calculadora_derivativos.calculadora_backend.model.Option; // Usando 'Option' conforme seu último ManualSpreadService
import com.calculadora_derivativos.calculadora_backend.repository.AtivoRepository;
import com.calculadora_derivativos.calculadora_backend.repository.OptionRepository;

@Service
// OBLIGATÓRIO: Implementar a interface
public class ManualSpreadService implements CalculadoraSpreadService {

    private final OptionRepository optionRepository;
    private final AtivoRepository ativoRepository;

    private static final int SCALE = 4;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    public ManualSpreadService(OptionRepository optionRepository, AtivoRepository ativoRepository) {
        this.optionRepository = optionRepository;
        this.ativoRepository = ativoRepository;
    }

    // OBLIGATÓRIO: Usar o nome do método da interface
    @Override
    public SpreadResponse calcularSpread(SpreadRequest request) {

        List<PernaSpread> pernas = request.pernas();
        String ativoSubjacente = request.ativoSubjacente();

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

        BigDecimal precoAtualSubjacente = request.cotacaoAtualAtivo().setScale(SCALE, ROUNDING_MODE);
        BigDecimal taxaUnitario = request.taxasOperacionais().setScale(SCALE, ROUNDING_MODE);

        List<PernaSpread> pernasParaRetorno = new ArrayList<>();
        BigDecimal custoLiquidoTotal = BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE);

        for (PernaSpread perna : pernas) {
            String ticker = perna.ticker();

            Optional<Option> opt = optionRepository.findByTicker(ticker);
            if (opt.isEmpty()) {
                return new SpreadResponse(
                        "ERRO: Ticker de opção não encontrado no banco de dados: " + ticker,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        List.of(), BigDecimal.ZERO);
            }

            Option dados = opt.get();
            BigDecimal precoPremio = dados.getPreco().setScale(SCALE, ROUNDING_MODE);
            BigDecimal quantidade = new BigDecimal(perna.quantidade());

            BigDecimal valorPernaBruto = precoPremio.multiply(quantidade).setScale(SCALE, ROUNDING_MODE);
            BigDecimal taxasTotalPerna = taxaUnitario.multiply(quantidade).abs().setScale(SCALE, ROUNDING_MODE);

            if ("COMPRA".equalsIgnoreCase(perna.operacao())) {
                custoLiquidoTotal = custoLiquidoTotal.subtract(valorPernaBruto).subtract(taxasTotalPerna);
            } else if ("VENDA".equalsIgnoreCase(perna.operacao())) {
                custoLiquidoTotal = custoLiquidoTotal.add(valorPernaBruto).subtract(taxasTotalPerna);
            } else {
                return new SpreadResponse(
                        "ERRO: Ação inválida para o ticker " + ticker + ". Use 'COMPRA' ou 'VENDA'.",
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        List.of(), BigDecimal.ZERO);
            }

            pernasParaRetorno.add(perna);
        }

        // Simulação e Payoff
        BigDecimal lucroMaximo = new BigDecimal("-999999999.00").setScale(SCALE, ROUNDING_MODE);
        BigDecimal prejuizoMaximo = new BigDecimal("999999999.00").setScale(SCALE, ROUNDING_MODE);
        BigDecimal breakevenPoint = BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE);

        BigDecimal passo = new BigDecimal("0.01").setScale(SCALE, ROUNDING_MODE);
        BigDecimal range = precoAtualSubjacente.multiply(new BigDecimal("0.2")).setScale(SCALE, ROUNDING_MODE);
        BigDecimal pMin = precoAtualSubjacente.subtract(range).max(BigDecimal.ZERO).setScale(SCALE, ROUNDING_MODE);
        BigDecimal pMax = precoAtualSubjacente.add(range).setScale(SCALE, ROUNDING_MODE);

        BigDecimal p = pMin;
        BigDecimal menorDif = new BigDecimal("999999999.00").setScale(SCALE, ROUNDING_MODE);

        while (p.compareTo(pMax) <= 0) {
            BigDecimal lucro = BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE);

            for (PernaSpread perna : pernas) {
                Optional<Option> opt2 = optionRepository.findByTicker(perna.ticker());
                if (opt2.isEmpty())
                    continue;
                Option d = opt2.get();
                BigDecimal payoff;
                if ("CALL".equalsIgnoreCase(d.getTipo())) {
                    payoff = p.subtract(d.getStrike()).max(BigDecimal.ZERO);
                } else {
                    payoff = d.getStrike().subtract(p).max(BigDecimal.ZERO);
                }
                payoff = payoff.multiply(new BigDecimal(perna.quantidade()));
                if ("VENDA".equalsIgnoreCase(perna.operacao()))
                    payoff = payoff.negate();

                lucro = lucro.add(payoff);
            }

            // Payoff Líquido = Payoff Bruto + Fluxo de Caixa Inicial (Custo Liquido Total)
            lucro = lucro.add(custoLiquidoTotal);

            lucro = lucro.setScale(SCALE, ROUNDING_MODE);
            if (lucro.compareTo(lucroMaximo) > 0)
                lucroMaximo = lucro;
            if (lucro.compareTo(prejuizoMaximo) < 0)
                prejuizoMaximo = lucro;

            BigDecimal dif = lucro.abs();
            if (dif.compareTo(menorDif) < 0) {
                menorDif = dif;
                breakevenPoint = p;
            }

            p = p.add(passo).setScale(SCALE, ROUNDING_MODE);
        }

        return new SpreadResponse(
                "Sucesso! Spread manual de " + ativoSubjacente + " calculado. Fluxo Inicial Líquido: "
                        + custoLiquidoTotal.setScale(2, ROUNDING_MODE),
                lucroMaximo.setScale(2, ROUNDING_MODE),
                prejuizoMaximo.negate().abs().setScale(2, ROUNDING_MODE),
                breakevenPoint.setScale(2, ROUNDING_MODE),
                pernasParaRetorno,
                custoLiquidoTotal.setScale(2, ROUNDING_MODE));
    }
}