package com.calculadora_derivativos.calculadora_backend.controller;

import com.calculadora_derivativos.calculadora_backend.dto.SpreadRequest;
import com.calculadora_derivativos.calculadora_backend.dto.SpreadResponse;
import com.calculadora_derivativos.calculadora_backend.service.SpreadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para endpoints de cálculo de spreads.
 * Expõe a API para o frontend ou Postman.
 */
@RestController
@RequestMapping("/api/spreads")
public class SpreadController {

    private final SpreadService spreadService;

    @Autowired
    public SpreadController(SpreadService spreadService) {
        this.spreadService = spreadService;
    }

    /**
     * Endpoint para calcular e otimizar um spread de baixa (Bear Call ou Bear Put).
     * @param request DTO com os parâmetros da operação.
     * @return SpreadResponse com o relatório de otimização.
     */
    @PostMapping("/calcular")
    public SpreadResponse calcularSpread(@RequestBody SpreadRequest request) {
        // Delega a lógica de negócio para o Service.
        return spreadService.calcularSpread(request);
    }
}
