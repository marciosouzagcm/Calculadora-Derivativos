package com.calculadora_derivativos.calculadora_backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "opcoes_final_tratado") 
public class Opcao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ticker/Código da opção (e.g., BOVAJ134W4)
    private String codigo; 

    // Tipo da opção: CALL ou PUT
    private String tipoOpcao; 

    // Preço da opção (o prêmio pago/recebido) - corresponde a 'premioPct' no CSV
    private BigDecimal preco; 

    // Preço de exercício (Strike) - Essencial para o cálculo do Spread
    private BigDecimal strike; 

    // Data de vencimento
    private LocalDate vencimento; 

    // Construtor padrão (necessário pelo JPA)
    public Opcao() {
    }

    // --- Métodos Getters ---
    
    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getTipoOpcao() {
        return tipoOpcao;
    }

    public BigDecimal getPreco() {
        return preco;
    }
    
    public BigDecimal getStrike() { // NOVO MÉTODO CORRETO
        return strike;
    }

    public LocalDate getVencimento() {
        return vencimento;
    }
    
    // --- Métodos Setters ---

    public void setId(Long id) {
        this.id = id;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public void setTipoOpcao(String tipoOpcao) {
        this.tipoOpcao = tipoOpcao;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }
    
    public void setStrike(BigDecimal strike) { // NOVO MÉTODO CORRETO
        this.strike = strike;
    }

    public void setVencimento(LocalDate vencimento) {
        this.vencimento = vencimento;
    }
}