package com.calculadora_derivativos.calculadora_backend.controller;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin; // Import para o CORS
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.calculadora_derivativos.calculadora_backend.dto.SpreadRequest;
import com.calculadora_derivativos.calculadora_backend.dto.SpreadResponse;
import com.calculadora_derivativos.calculadora_backend.service.SpreadService;

/**
 * Controller responsável por receber requisições de cálculo e otimização de
 * spreads.
 * Adicionada anotação @CrossOrigin para permitir requisições de frontend.
 */
@RestController
@RequestMapping("/spread")
@CrossOrigin(origins = "*") // Permite requisições de qualquer origem (ideal para desenvolvimento/teste)
public class SpreadController {

    private final SpreadService spreadService;

    @Autowired
    public SpreadController(SpreadService spreadService) {
        this.spreadService = spreadService;
    }

    // --- 1. CÁLCULO MANUAL (POST) ---

    /**
     * Calcula o resultado de um spread com parâmetros fornecidos manualmente.
     * * @param request Dados de entrada para o cálculo do spread.
     * 
     * @return O resultado do cálculo do spread.
     */
    @PostMapping("/manual/calcular")
    public SpreadResponse calcularManual(@RequestBody SpreadRequest request) {
        return spreadService.calcularSpread(request);
    }

    // --- 2. OTIMIZAÇÃO INDIVIDUAL: Bull Call Spread (Call-Alta) ---

    /**
     * Otimiza a melhor combinação de Bull Call Spread para uma determinada ação.
     * * @param idAcao            O ID da ação (ex: PETR4).
     * 
     * @param cotacaoAtualAtivo Preço atual do ativo.
     * @param taxasOperacionais Taxas.
     * @return O SpreadResponse da estratégia otimizada.
     */
    @GetMapping("/otimizar/call-alta/{idAcao}")
    public SpreadResponse otimizarBullCallSpread(
            @PathVariable String idAcao,
            @RequestParam BigDecimal cotacaoAtualAtivo,
            @RequestParam BigDecimal taxasOperacionais) {
        return spreadService.otimizarBullCallSpread(idAcao, cotacaoAtualAtivo, taxasOperacionais);
    }

    // --- 3. OTIMIZAÇÃO INDIVIDUAL: Bear Put Spread (Put-Baixa) ---

    /**
     * Otimiza a melhor combinação de Bear Put Spread para uma determinada ação.
     * * @param idAcao            O ID da ação (ex: PETR4).
     * 
     * @param cotacaoAtualAtivo Preço atual do ativo.
     * @param taxasOperacionais Taxas.
     * @return O SpreadResponse da estratégia otimizada.
     */
    @GetMapping("/otimizar/put-baixa/{idAcao}")
    public SpreadResponse otimizarBearPutSpread(
            @PathVariable String idAcao,
            @RequestParam BigDecimal cotacaoAtualAtivo,
            @RequestParam BigDecimal taxasOperacionais) {
        return spreadService.otimizarBearPutSpread(idAcao, cotacaoAtualAtivo, taxasOperacionais);
    }

    // --- 4. NOVO ENDPOINT INDIVIDUAL: Bull Put Spread (Put-Alta) ---

    /**
     * Otimiza a melhor combinação de Bull Put Spread para uma determinada ação.
     * * @param idAcao            O ID da ação (ex: PETR4).
     * 
     * @param cotacaoAtualAtivo Preço atual do ativo.
     * @param taxasOperacionais Taxas.
     * @return O SpreadResponse da estratégia otimizada.
     */
    @GetMapping("/otimizar/put-alta/{idAcao}")
    public SpreadResponse otimizarBullPutSpread(
            @PathVariable String idAcao,
            @RequestParam BigDecimal cotacaoAtualAtivo,
            @RequestParam BigDecimal taxasOperacionais) {
        // CORREÇÃO: Chamando o método otimizarBullPutSpread, e não
        // otimizarBearCallSpread
        return spreadService.otimizarBullPutSpread(idAcao, cotacaoAtualAtivo, taxasOperacionais);
    }

    // --- 5. NOVO ENDPOINT INDIVIDUAL: Bear Call Spread (Call-Baixa) ---

    /**
     * Otimiza a melhor combinação de Bear Call Spread para uma determinada ação.
     * * @param idAcao            O ID da ação (ex: PETR4).
     * 
     * @param cotacaoAtualAtivo Preço atual do ativo.
     * @param taxasOperacionais Taxas.
     * @return O SpreadResponse da estratégia otimizada.
     */
    @GetMapping("/otimizar/call-baixa/{idAcao}")
    public SpreadResponse otimizarBearCallSpread(
            @PathVariable String idAcao,
            @RequestParam BigDecimal cotacaoAtualAtivo,
            @RequestParam BigDecimal taxasOperacionais) {
        return spreadService.otimizarBearCallSpread(idAcao, cotacaoAtualAtivo, taxasOperacionais);
    }

    // --- 6. NOVO ENDPOINT UNIFICADO: OTIMIZAÇÃO DA MELHOR ESTRATÉGIA ---

    /**
     * Compara as 4 estratégias direcionais (Bull Call, Bear Put, Bull Put, Bear
     * Call)
     * e retorna a que oferece o melhor resultado (maior Lucro Máximo).
     * * @param idAcao            O ID da ação (ex: PETR4).
     * 
     * @param cotacaoAtualAtivo Preço atual do ativo.
     * @param taxasOperacionais Taxas.
     * @return O SpreadResponse da melhor estratégia encontrada.
     */
    @GetMapping("/otimizar/melhor/{idAcao}")
    public SpreadResponse otimizarMelhorEstrategia(
            @PathVariable String idAcao,
            @RequestParam BigDecimal cotacaoAtualAtivo,
            @RequestParam BigDecimal taxasOperacionais) {
        return spreadService.otimizarMelhorEstrategia(idAcao, cotacaoAtualAtivo, taxasOperacionais);
    }
}