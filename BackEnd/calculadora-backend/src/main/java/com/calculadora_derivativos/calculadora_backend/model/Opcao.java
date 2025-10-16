package com.calculadora_derivativos.calculadora_backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data; // Importado para simplificar o código

@Data // Adicionando Lombok para gerar Getters/Setters/ToString/Equals/HashCode
@Entity
@Table(name = "opcoes_final_tratado") 
public class Opcao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Identificadores ---
    @Column(name = "ticker") // Se 'codigo' for o ticker da opção, mantenha.
    private String codigo; 
    
    @Column(name = "idAcao") // Correspondente a 'idAcao' do CSV/Python
    private String idAcao; // Ticker do Ativo Base (Ex: PETR4)

    // --- Dados da Opção ---
    @Column(name = "tipo") // Tipo da opção: CALL ou PUT (correspondente ao campo 'tipo' no Python)
    private String tipoOpcao; 

    private BigDecimal preco; // Prêmio em R$ (Corresponde a 'premio' se disponível, ou 'premioPct')
    private BigDecimal strike; // Preço de exercício (Strike)
    private LocalDate vencimento; // Data de vencimento

    // --- Métricas de Cálculo / Gregas (Crucial para replicar a lógica do Python) ---
    // Usamos BigDecimal para precisão em todos os cálculos
    
    private BigDecimal delta;
    private BigDecimal gamma;
    private BigDecimal theta;
    private BigDecimal vega;
    private BigDecimal volImplicita; // Volatilidade Implícita
    
    // Coluna para dias úteis (para T-dias). O Python o recalculava, mas é bom tê-lo
    // como Double/BigDecimal para o cálculo de Black-Scholes (T em anos).
    @Transient // Não persiste no BD se for recalculado
    private Integer diasUteis; 
}
// OBS: Se você usa o Lombok (@Data), remova todos os Getters/Setters manuais.