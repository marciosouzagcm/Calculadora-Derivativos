package com.calculadora_derivativos.calculadora_backend.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column; // Novo import necessário
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

    // Mapeado para 'id_acao' no DB
    @Column(name = "id_acao") 
    private String idAcao; 
    
    private String tipo;
    private LocalDate vencimento; 
    
    // --- 🟢 NOVOS CAMPOS ADICIONADOS ---
    
    // Mapeado para 'dias_uteis' no DB
    @Column(name = "dias_uteis")
    private Integer diasUteis;
    
    // Mapeado para 'data_hora' no DB
    @Column(name = "data_hora")
    private LocalDateTime dataHora; // Usamos LocalDateTime para data e hora
    
    // --- 🔴 FIM DOS NOVOS CAMPOS ---
    
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
    // GETTERS E SETTERS
    // -----------------------------------------------------------------------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }
    
    public String getIdAcao() { return idAcao; }
    public void setIdAcao(String idAcao) { this.idAcao = idAcao; } 
    
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    
    public LocalDate getVencimento() { return vencimento; }
    public void setVencimento(LocalDate vencimento) { this.vencimento = vencimento; }
    
    // --- 🟢 GETTER/SETTER para diasUteis ---
    public Integer getDiasUteis() { return diasUteis; }
    public void setDiasUteis(Integer diasUteis) { this.diasUteis = diasUteis; }
    
    // --- 🟢 GETTER/SETTER para dataHora ---
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    
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