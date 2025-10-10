package com.calculadora_derivativos.calculadora_backend.dto;

import java.math.BigDecimal;

/**
 * DTO (Data Transfer Object) para encapsular o resultado do cálculo de spread.
 */
public class SpreadResponse {
    
    private String statusMessage;
    private BigDecimal lucroMaximo;
    private BigDecimal prejuizoMaximo;
    private BigDecimal breakevenPoint; // <--- NOVO CAMPO

    // Construtor padrão (necessário para serialização do JSON pelo Spring/Jackson)
    public SpreadResponse() {
    }

    /**
     * Construtor completo para retornar os resultados da otimização.
     */
    public SpreadResponse(String statusMessage, BigDecimal lucroMaximo, 
                          BigDecimal prejuizoMaximo, BigDecimal breakevenPoint) { // <--- CONSTRUTOR ATUALIZADO
        this.statusMessage = statusMessage;
        this.lucroMaximo = lucroMaximo;
        this.prejuizoMaximo = prejuizoMaximo;
        this.breakevenPoint = breakevenPoint;
    }

    // --- Getters e Setters ---
    
    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public BigDecimal getLucroMaximo() {
        return lucroMaximo;
    }

    public void setLucroMaximo(BigDecimal lucroMaximo) {
        this.lucroMaximo = lucroMaximo;
    }

    public BigDecimal getPrejuizoMaximo() {
        return prejuizoMaximo;
    }

    public void setPrejuizoMaximo(BigDecimal prejuizoMaximo) {
        this.prejuizoMaximo = prejuizoMaximo;
    }
    
    public BigDecimal getBreakevenPoint() { // <--- NOVO GETTER
        return breakevenPoint;
    }

    public void setBreakevenPoint(BigDecimal breakevenPoint) { // <--- NOVO SETTER
        this.breakevenPoint = breakevenPoint;
    }
}