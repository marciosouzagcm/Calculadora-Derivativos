package com.calculadora_derivativos.calculadora_backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.calculadora_derivativos.calculadora_backend.dto.OtimizacaoResponse;
import com.calculadora_derivativos.calculadora_backend.dto.PernaSpread;
import com.calculadora_derivativos.calculadora_backend.dto.SpreadRequest;
import com.calculadora_derivativos.calculadora_backend.dto.SpreadResponse;
import com.calculadora_derivativos.calculadora_backend.model.Opcao;
import com.calculadora_derivativos.calculadora_backend.repository.OpcaoRepository;

@Service
public class OtimizacaoSpreadService {

    private final OpcaoRepository opcaoRepository;
    private final CalculadoraSpreadService calculadoraSpreadService;

    // Constantes
    private static final int QUANTIDADE_PADRAO = 100;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    @Autowired
    public OtimizacaoSpreadService(
            OpcaoRepository opcaoRepository,
            @Qualifier("spreadService") // Correção para a injeção de dependência
            CalculadoraSpreadService calculadoraSpreadService) {
        this.opcaoRepository = opcaoRepository;
        this.calculadoraSpreadService = calculadoraSpreadService;
    }

    // ----------------------------------------------------
    // --- MÉTODOS DE OTIMIZAÇÃO DE ENDPOINT (4 TIPOS) ---
    // ----------------------------------------------------

    // 1. SPREAD DE DÉBITO: Call Spread de Alta (Buy Call Baixa, Sell Call Alta)
    public OtimizacaoResponse otimizarCallSpreadDeAlta(
            String idAcao,
            BigDecimal cotacaoAtualAtivo,
            BigDecimal taxasOperacionais) {

        return otimizarSpreadVertical(
                idAcao,
                cotacaoAtualAtivo,
                taxasOperacionais,
                "CALL",
                "CALL SPREAD DE ALTA",
                (op1, op2) -> op1.getStrike().compareTo(op2.getStrike()) < 0);
    }

    // 2. SPREAD DE DÉBITO: Put Spread de Baixa (Buy Put Alta, Sell Put Baixa)
    public OtimizacaoResponse otimizarPutSpreadDeBaixa(
            String idAcao,
            BigDecimal cotacaoAtualAtivo,
            BigDecimal taxasOperacionais) {

        return otimizarSpreadVertical(
                idAcao,
                cotacaoAtualAtivo,
                taxasOperacionais,
                "PUT",
                "PUT SPREAD DE BAIXA",
                (op1, op2) -> op1.getStrike().compareTo(op2.getStrike()) > 0);
    }

    // 3. SPREAD DE CRÉDITO: Call Spread de Baixa (Sell Call Baixa, Buy Call Alta)
    public OtimizacaoResponse otimizarCallSpreadDeBaixa(
            String idAcao,
            BigDecimal cotacaoAtualAtivo,
            BigDecimal taxasOperacionais) {

        return otimizarSpreadVertical(
                idAcao,
                cotacaoAtualAtivo,
                taxasOperacionais,
                "CALL",
                "CALL SPREAD DE BAIXA",
                (op1, op2) -> op1.getStrike().compareTo(op2.getStrike()) > 0);
    }

    // 4. SPREAD DE CRÉDITO: Put Spread de Alta (Sell Put Alta, Buy Put Baixa)
    public OtimizacaoResponse otimizarPutSpreadDeAlta(
            String idAcao,
            BigDecimal cotacaoAtualAtivo,
            BigDecimal taxasOperacionais) {

        return otimizarSpreadVertical(
                idAcao,
                cotacaoAtualAtivo,
                taxasOperacionais,
                "PUT",
                "PUT SPREAD DE ALTA",
                (op1, op2) -> op1.getStrike().compareTo(op2.getStrike()) < 0);
    }

    // ----------------------------------------------------
    // --- NOVO MÉTODO: OTIMIZAÇÃO GERAL (TODOS OS 4) ---
    // ----------------------------------------------------

    /**
     * Otimiza e compara os 4 tipos de spreads verticais (Débito e Crédito)
     * e retorna o que oferece o maior lucro máximo.
     */
    public OtimizacaoResponse otimizarMelhorSpreadGeral(
            String idAcao,
            BigDecimal cotacaoAtualAtivo,
            BigDecimal taxasOperacionais) {

        // 1. Executa a otimização para todos os 4 cenários
        OtimizacaoResponse callAlta = otimizarCallSpreadDeAlta(idAcao, cotacaoAtualAtivo, taxasOperacionais);
        OtimizacaoResponse putBaixa = otimizarPutSpreadDeBaixa(idAcao, cotacaoAtualAtivo, taxasOperacionais);
        OtimizacaoResponse callBaixa = otimizarCallSpreadDeBaixa(idAcao, cotacaoAtualAtivo, taxasOperacionais);
        OtimizacaoResponse putAlta = otimizarPutSpreadDeAlta(idAcao, cotacaoAtualAtivo, taxasOperacionais);

        List<OtimizacaoResponse> todosResultados = List.of(callAlta, putBaixa, callBaixa, putAlta);

        // 2. Filtra apenas os resultados que não indicam erro/vazio e compara
        OtimizacaoResponse melhorResultadoGeral = todosResultados.stream()
                // Usando .melhorEstrategia() em vez de .melhorSpread()
                .filter(res -> res.melhorEstrategia() != null)
                // Usando .resultadoOtimizacao() em vez de .lucroMaximo()
                .filter(res -> res.resultadoOtimizacao().compareTo(BigDecimal.ZERO) > 0)
                // Usando .resultadoOtimizacao()
                .max(Comparator.comparing(OtimizacaoResponse::resultadoOtimizacao))
                .orElse(null);

        // 3. Retorna o resultado
        if (melhorResultadoGeral != null) {
            // Cria uma resposta consolidada
            return new OtimizacaoResponse(
                    idAcao,
                    // Usando .tipoOtimizacao() em vez de .nomeEstrategia()
                    "MELHOR SPREAD GERAL (" + melhorResultadoGeral.tipoOtimizacao() + ")",
                    melhorResultadoGeral.resultadoOtimizacao(), // Usando .resultadoOtimizacao()
                    melhorResultadoGeral.melhorEstrategia(), // Usando .melhorEstrategia()
                    melhorResultadoGeral.estrategiasAvaliadas() // Usando .estrategiasAvaliadas()
            );
        } else {
            // Retorna uma resposta indicando que nenhuma estratégia lucrativa foi
            // encontrada
            return new OtimizacaoResponse(
                    idAcao,
                    "MELHOR SPREAD GERAL",
                    BigDecimal.ZERO,
                    new SpreadResponse("Nenhuma estratégia lucrativa foi encontrada em nenhum dos 4 cenários.",
                            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of(), BigDecimal.ZERO),
                    List.of());
        }
    }

    // ----------------------------------------------------
    // --- MÉTODO GENÉRICO DE OTIMIZAÇÃO (CORE) ---
    // ----------------------------------------------------

    private OtimizacaoResponse otimizarSpreadVertical(
            String idAcao,
            BigDecimal cotacaoAtualAtivo,
            BigDecimal taxasOperacionais,
            String tipoOpcao,
            String nomeEstrategia,
            BiPredicate<Opcao, Opcao> isBuyA_SellB) {

        // 1. Busca e filtra opções
        List<Opcao> opcoesDisponiveis = opcaoRepository.findByTickerCamelCaseOrTickerSnakeCase(idAcao, idAcao).stream()
                .filter(op -> tipoOpcao.equalsIgnoreCase(op.getTipo()))
                .toList();

        if (opcoesDisponiveis.size() < 2) {
            return new OtimizacaoResponse(
                    idAcao,
                    nomeEstrategia,
                    BigDecimal.ZERO,
                    null,
                    List.of(new SpreadResponse(
                            "Nenhuma " + tipoOpcao + " disponível para otimização com o ativo " + idAcao + ".",
                            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of(), BigDecimal.ZERO)));
        }

        List<SpreadResponse> resultados = new ArrayList<>();

        // 2. Agrupa por vencimento para formar spreads da mesma série
        opcoesDisponiveis.stream()
                .collect(Collectors.groupingBy(Opcao::getVencimento))
                .forEach((vencimento, opcoesDaSerie) -> {

                    opcoesDaSerie.sort(Comparator.comparing(Opcao::getStrike));

                    // 3. Combina pares de opções para formar spreads
                    for (int i = 0; i < opcoesDaSerie.size(); i++) {
                        for (int j = i + 1; j < opcoesDaSerie.size(); j++) {
                            Opcao op1 = opcoesDaSerie.get(i);
                            Opcao op2 = opcoesDaSerie.get(j);

                            if (op1.getStrike().compareTo(op2.getStrike()) == 0)
                                continue;

                            Opcao pernaComprada;
                            Opcao pernaVendida;

                            // Determina a perna comprada e vendida usando o BiPredicate
                            if (isBuyA_SellB.test(op1, op2)) {
                                pernaComprada = op1;
                                pernaVendida = op2;
                            } else if (isBuyA_SellB.test(op2, op1)) {
                                pernaComprada = op2;
                                pernaVendida = op1;
                            } else {
                                continue;
                            }

                            // 4. Criação das Pernas do Spread (Lógica inalterada)
                            PernaSpread pernaBuy = new PernaSpread(pernaComprada.getTickerReal(), QUANTIDADE_PADRAO,
                                    "COMPRA");
                            PernaSpread pernaSell = new PernaSpread(pernaVendida.getTickerReal(), QUANTIDADE_PADRAO,
                                    "VENDA");

                            List<PernaSpread> pernas = List.of(pernaBuy, pernaSell);

                            SpreadRequest spreadRequest = new SpreadRequest(
                                    idAcao, cotacaoAtualAtivo, taxasOperacionais, pernas);

                            // 5. Calcula o spread e adiciona aos resultados
                            SpreadResponse resultadoSpread = calculadoraSpreadService.calcularSpread(spreadRequest);

                            if (resultadoSpread != null) {
                                resultados.add(resultadoSpread);
                            }
                        }
                    }
                });

        // 6. Encontra a melhor estratégia (maior Lucro Máximo)
        SpreadResponse melhorResultado = resultados.stream()
                .max(Comparator.comparing(SpreadResponse::lucroMaximo))
                .orElse(null);

        // 7. Prepara a resposta de otimização
        if (melhorResultado != null) {
            return new OtimizacaoResponse(
                    idAcao,
                    nomeEstrategia,
                    melhorResultado.lucroMaximo().setScale(2, ROUNDING_MODE), // Usamos lucroMaximo como
                                                                              // resultadoOtimizacao
                    melhorResultado,
                    resultados.stream()
                            .sorted(Comparator.comparing(SpreadResponse::lucroMaximo).reversed())
                            .limit(5)
                            .toList());
        }

        // Caso não encontre nenhum resultado lucrativo
        return new OtimizacaoResponse(
                idAcao,
                nomeEstrategia,
                BigDecimal.ZERO,
                new SpreadResponse(
                        "Nenhuma estratégia de " + nomeEstrategia + " lucrativa encontrada. Tentativas: "
                                + resultados.size(),
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of(), BigDecimal.ZERO),
                resultados.stream().limit(5).toList());
    }
}