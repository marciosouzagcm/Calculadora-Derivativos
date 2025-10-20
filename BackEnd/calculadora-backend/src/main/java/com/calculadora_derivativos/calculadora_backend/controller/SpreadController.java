package com.calculadora_derivativos.calculadora_backend.controller;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.calculadora_derivativos.calculadora_backend.dto.OtimizacaoResponse;
import com.calculadora_derivativos.calculadora_backend.service.OtimizacaoSpreadService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;

@RestController
@RequestMapping("/api/spreads/otimizar")
public class SpreadController {

    private final OtimizacaoSpreadService otimizacaoSpreadService;

    @Autowired
    public SpreadController(OtimizacaoSpreadService otimizacaoSpreadService) {
        this.otimizacaoSpreadService = otimizacaoSpreadService;
    }

    // CORREÇÃO ESSENCIAL: Permite que o Spring lide com vírgulas (',') em números
    // decimais.
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(BigDecimal.class, new CustomBigDecimalEditor());
    }

    // Classe auxiliar para conversão de BigDecimal
    private static class CustomBigDecimalEditor extends java.beans.PropertyEditorSupport {
        @Override
        public void setAsText(String text) throws IllegalArgumentException {
            if (text == null || text.trim().isEmpty()) {
                setValue(null);
                return;
            }
            try {
                // Tenta formatar com ponto (padrão inglês/Spring)
                setValue(new BigDecimal(text.trim()));
            } catch (NumberFormatException e) {
                // Se falhar, tenta formatar com vírgula (padrão brasileiro)
                try {
                    NumberFormat format = NumberFormat.getInstance(new Locale("pt", "BR"));
                    Number number = format.parse(text.trim());
                    // Conversão mais segura para BigDecimal
                    setValue(new BigDecimal(number.toString()));
                } catch (ParseException ex) {
                    throw new IllegalArgumentException(
                            "Não foi possível converter o valor '" + text + "' para um número decimal.", ex);
                }
            }
        }
    }

    // ------------------------------------
    // --- ENDPOINTS DE OTIMIZAÇÃO (4 TIPOS) ---
    // ------------------------------------

    // 1. Call Spread de Alta (Débito)
    @GetMapping("/call-alta")
    public ResponseEntity<OtimizacaoResponse> otimizarCallSpreadDeAlta(
            @RequestParam String idAcao,
            @RequestParam @Valid @DecimalMin(value = "0.01", message = "A cotação deve ser maior que zero.") BigDecimal cotacaoAtualAtivo,
            @RequestParam @Valid @DecimalMin(value = "0.00", message = "As taxas devem ser não-negativas.") BigDecimal taxasOperacionais) {

        return ResponseEntity
                .ok(otimizacaoSpreadService.otimizarCallSpreadDeAlta(idAcao, cotacaoAtualAtivo, taxasOperacionais));
    }

    // 2. Put Spread de Baixa (Débito)
    @GetMapping("/put-baixa")
    public ResponseEntity<OtimizacaoResponse> otimizarPutSpreadDeBaixa(
            @RequestParam String idAcao,
            @RequestParam @Valid @DecimalMin(value = "0.01", message = "A cotação deve ser maior que zero.") BigDecimal cotacaoAtualAtivo,
            @RequestParam @Valid @DecimalMin(value = "0.00", message = "As taxas devem ser não-negativas.") BigDecimal taxasOperacionais) {

        return ResponseEntity
                .ok(otimizacaoSpreadService.otimizarPutSpreadDeBaixa(idAcao, cotacaoAtualAtivo, taxasOperacionais));
    }

    // 3. Call Spread de Baixa (Crédito)
    @GetMapping("/call-baixa")
    public ResponseEntity<OtimizacaoResponse> otimizarCallSpreadDeBaixa(
            @RequestParam String idAcao,
            @RequestParam @Valid @DecimalMin(value = "0.01", message = "A cotação deve ser maior que zero.") BigDecimal cotacaoAtualAtivo,
            @RequestParam @Valid @DecimalMin(value = "0.00", message = "As taxas devem ser não-negativas.") BigDecimal taxasOperacionais) {

        return ResponseEntity
                .ok(otimizacaoSpreadService.otimizarCallSpreadDeBaixa(idAcao, cotacaoAtualAtivo, taxasOperacionais));
    }

    // 4. Put Spread de Alta (Crédito)
    @GetMapping("/put-alta")
    public ResponseEntity<OtimizacaoResponse> otimizarPutSpreadDeAlta(
            @RequestParam String idAcao,
            @RequestParam @Valid @DecimalMin(value = "0.01", message = "A cotação deve ser maior que zero.") BigDecimal cotacaoAtualAtivo,
            @RequestParam @Valid @DecimalMin(value = "0.00", message = "As taxas devem ser não-negativas.") BigDecimal taxasOperacionais) {

        return ResponseEntity
                .ok(otimizacaoSpreadService.otimizarPutSpreadDeAlta(idAcao, cotacaoAtualAtivo, taxasOperacionais));
    }

    // 5. OTIMIZAÇÃO GERAL (Compara os 4 e retorna o melhor)
    @GetMapping("/geral")
    public ResponseEntity<OtimizacaoResponse> otimizarMelhorSpreadGeral(
            @RequestParam String idAcao,
            @RequestParam @Valid @DecimalMin(value = "0.01", message = "A cotação deve ser maior que zero.") BigDecimal cotacaoAtualAtivo,
            @RequestParam @Valid @DecimalMin(value = "0.00", message = "As taxas devem ser não-negativas.") BigDecimal taxasOperacionais) {

        return ResponseEntity
                .ok(otimizacaoSpreadService.otimizarMelhorSpreadGeral(idAcao, cotacaoAtualAtivo, taxasOperacionais));
    }
}