package com.calculadora_derivativos.calculadora_backend.model;

import lombok.Data;

@Data
public class SpreadResult {
    
    // Métricas principais do cálculo
    private String operacaoTipo = "Bear Call Spread";
    private String status = "Sucesso";
    
    private Double creditoLiquidoPorContrato;
    private Double ganhoMaximoTotal;
    private Double prejuizoMaximoTotal;
    private Double pontoEquilibrio;
    
    // Informações abstraídas do DB
    private Double viCallVendida;
    private Double viCallComprada;
    
    // Construtores
    public SpreadResult(String status) {
        this.status = status;
    }
    
    public SpreadResult() {
        // Construtor padrão
    }
}