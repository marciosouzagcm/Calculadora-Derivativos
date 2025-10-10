package com.calculadora_derivativos.calculadora_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidade que representa uma opção de negociação, utilizando Lombok para 
 * reduzir boilerplate code (Getters/Setters). Mapeia todas as 10 colunas 
 * cruciais do CSV, incluindo as Gregas.
 */
@Entity
@Table(name = "opcoes_final_para_mysql") // Nome da tabela no MySQL
@Data // Gera Getters, Setters, toString, equals e hashCode
@NoArgsConstructor // Garante o construtor padrão (necessário para JPA)
public class Option {

    // Chave primária (ID)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ticker da opção (Ex: BOVAK137W1). Única no banco.
    @Column(unique = true)
    private String ticker;

    // Código do ativo base (Ex: BOVA11). Mapeado para id_acao.
    @Column(name = "id_acao") 
    private String idAcao;

    // Tipo da opção (CALL ou PUT)
    private String tipo;

    // Preço de exercício
    private Double strike;

    // Prêmio (Preço) da opção. Mapeado para premio_pct.
    @Column(name = "premio_pct")
    private Double premioPct;

    // Volatilidade Implícita. Mapeado para vol_implicita.
    @Column(name = "vol_implicita")
    private Double volImplicita;

    // GREGAS (4 Colunas)

    // Sensibilidade do preço da opção à variação do ativo base
    private Double delta;
    
    // Sensibilidade do Delta à variação do ativo base
    private Double gamma;
    
    // Sensibilidade do preço da opção à passagem do tempo (Time Decay)
    private Double theta;
    
    // Sensibilidade do preço da opção à variação da Volatilidade Implícita
    private Double vega;
}
