package com.calculadora_derivativos.calculadora_backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "opcoes_final_tratado") 
public class Option {

    public Option() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Coluna 'ticker'
    @Column(unique = true)
    private String ticker;

    // Mapeado para 'id_acao' no DB (como esperado pelo DataLoader)
    @Column(name = "id_acao") 
    private String idAcao; 
    
    private String tipo;
    private LocalDate vencimento; 
    private BigDecimal strike; 
    private BigDecimal preco; 

    // Mapeado para 'vol_implicita' no DB
    @Column(name = "vol_implicita")
    private BigDecimal volImplicita; 

    private BigDecimal delta; 
    private BigDecimal gamma; 
    private BigDecimal theta; 
    private BigDecimal vega; 

    // -----------------------------------------------------------------------
    // GETTERS E SETTERS (Corrigidos)
    // -----------------------------------------------------------------------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }
    
    public String getIdAcao() { return idAcao; }
    // CORRIGIDO: idAcao (apenas um 'a')
    public void setIdAcao(String idAcao) { this.idAcao = idAcao; } 
    
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    
    public LocalDate getVencimento() { return vencimento; }
    public void setVencimento(LocalDate vencimento) { this.vencimento = vencimento; }
    
    public BigDecimal getStrike() { return strike; }
    public void setStrike(BigDecimal strike) { this.strike = strike; }
    
    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }
    
    public BigDecimal getVolImplicita() { return volImplicita; }
    public void setVolImplicita(BigDecimal volImplicita) { this.volImplicita = volImplicita; }
    
    public BigDecimal getDelta() { return delta; }
    public void setDelta(BigDecimal delta) { this.delta = delta; }
    
    public BigDecimal getGamma() { return gamma; }
    public void setGamma(BigDecimal gamma) { this.gamma = gamma; }
    
    public BigDecimal getTheta() { return theta; }
    public void setTheta(BigDecimal theta) { this.theta = theta; }
    
    public BigDecimal getVega() { return vega; }
    public void setVega(BigDecimal vega) { this.vega = vega; }
}