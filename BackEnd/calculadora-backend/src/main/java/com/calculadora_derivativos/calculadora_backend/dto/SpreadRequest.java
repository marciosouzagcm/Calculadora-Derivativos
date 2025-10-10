package com.calculadora_derivativos.calculadora_backend.dto;

import java.util.List;

/**
 * DTO (Data Transfer Object) para encapsular os dados de entrada
 * da requisição de cálculo de Spread, suportando estratégias de múltiplas pernas.
 */
public class SpreadRequest {
    
    // Identificador do Ativo Subjacente (ex: PETR4, VALE3)
    private String ativoSubjacente;
    
    // A lista de opções (Pernas) que compõem a estratégia de spread.
    private List<PernaSpread> pernas;
    
    // Construtor padrão
    public SpreadRequest() {}

    // Construtor com campos
    public SpreadRequest(String ativoSubjacente, List<PernaSpread> pernas) {
        this.ativoSubjacente = ativoSubjacente;
        this.pernas = pernas;
    }
    
    // --- Getters e Setters para o Spring mapear a requisição JSON ---

    public String getAtivoSubjacente() {
        return ativoSubjacente;
    }

    public void setAtivoSubjacente(String ativoSubjacente) {
        this.ativoSubjacente = ativoSubjacente;
    }

    public List<PernaSpread> getPernas() {
        return pernas;
    }

    public void setPernas(List<PernaSpread> pernas) {
        this.pernas = pernas;
    }
}