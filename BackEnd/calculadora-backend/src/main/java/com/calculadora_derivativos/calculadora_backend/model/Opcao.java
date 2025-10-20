package com.calculadora_derivativos.calculadora_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "opcoes_final_tratado") 
public class Opcao {

    @Id
    private Long id; 

    // Mapeia a coluna 'idAcao'
    @Column(name = "idAcao") 
    private String tickerCamelCase; 

    // Mapeia a coluna 'id_acao'
    @Column(name = "id_acao") 
    private String tickerSnakeCase; 

    private String ticker; // Supondo que você ainda tem um campo 'ticker'

    private String tipo; // "CALL" ou "PUT"
    private BigDecimal strike;
    private LocalDate vencimento;
    private BigDecimal preco; 

    // Construtor padrão (obrigatório para JPA)
    public Opcao() {}

    // ------------------------------------
    // GETTERS
    // ------------------------------------
    
    public Long getId() { return id; }
    public String getTickerCamelCase() { return tickerCamelCase; }
    public String getTickerSnakeCase() { return tickerSnakeCase; }
    public String getTicker() { return ticker; }
    public String getTipo() { return tipo; }
    public BigDecimal getStrike() { return strike; }
    public LocalDate getVencimento() { return vencimento; }
    public BigDecimal getPreco() { return preco; }
    
    /**
     * Método utilitário para retornar o ticker real (o que não for nulo/vazio)
     * Usado na lógica de serviço, mas não pelo JPA para busca.
     */
    public String getTickerReal() {
        if (tickerCamelCase != null && !tickerCamelCase.isEmpty()) {
            return tickerCamelCase;
        }
        return tickerSnakeCase;
    }

    // ------------------------------------
    // SETTERS (INCLUÍDOS AGORA COMPLETAMENTE)
    // ------------------------------------

    public void setId(Long id) { this.id = id; }
    public void setTickerCamelCase(String tickerCamelCase) { this.tickerCamelCase = tickerCamelCase; }
    public void setTickerSnakeCase(String tickerSnakeCase) { this.tickerSnakeCase = tickerSnakeCase; }
    public void setTicker(String ticker) { this.ticker = ticker; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public void setStrike(BigDecimal strike) { this.strike = strike; }
    public void setVencimento(LocalDate vencimento) { this.vencimento = vencimento; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }

    // ------------------------------------
    // MÉTODOS DE CONVENIÊNCIA (Opcionais, mas boas práticas)
    // ------------------------------------
    
    @Override
    public String toString() {
        return "Opcao{" +
                "id=" + id +
                ", tickerReal='" + getTickerReal() + '\'' +
                ", tipo='" + tipo + '\'' +
                ", strike=" + strike +
                ", vencimento=" + vencimento +
                ", preco=" + preco +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Opcao opcao = (Opcao) o;
        return Objects.equals(id, opcao.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}