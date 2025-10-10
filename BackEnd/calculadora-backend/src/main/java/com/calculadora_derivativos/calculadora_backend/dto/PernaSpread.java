package com.calculadora_derivativos.calculadora_backend.dto;

/**
 * DTO (Data Transfer Object) para representar uma perna individual
 * dentro de uma estratégia de spread (Ex: uma opção comprada e/ou vendida).
 * Usado como um item na lista de 'pernas' do SpreadRequest.
 */
public class PernaSpread {
    
    // Código da opção (ex: PETRJ28) - usado para buscar dados no BD. 
    private String ticker; 
    
    // Quantidade de contratos envolvidos na perna (ex: 100).
    private int quantidade; 
    
    // Ação da perna: "COMPRA" ou "VENDA".
    // CORRIGIDO: O nome do campo foi alterado de 'acao' para 'operacao' para coincidir com o JSON de entrada.
    private String operacao; 

    /**
     * Construtor padrão.
     * Necessário para a desserialização de JSON pelo Spring/Jackson.
     */
    public PernaSpread() {
    }

    /**
     * Construtor completo.
     * Útil para testes e criação programática.
     */
    public PernaSpread(String ticker, int quantidade, String operacao) {
        this.ticker = ticker;
        this.quantidade = quantidade;
        this.operacao = operacao; // CORRIGIDO
    }

    // --- Getters e Setters ---

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    // CORRIGIDO: Antigo getAcao() renomeado para getOperacao()
    public String getOperacao() {
        return operacao;
    }

    // CORRIGIDO: Antigo setAcao() renomeado para setOperacao()
    public void setOperacao(String operacao) {
        this.operacao = operacao;
    }
}