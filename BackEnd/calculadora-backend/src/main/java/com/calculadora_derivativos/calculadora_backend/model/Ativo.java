package com.calculadora_derivativos.calculadora_backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import lombok.Data; // Importante manter o @Data

@Data // ANOTAÇÃO DO LOMBOK QUE GERA O getPrecoAtual()
@Entity
public class Ativo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codigo; // Ex: BOVA11, PETR4

    private BigDecimal precoAtual; // NÃO PRECISA DE GETTER/SETTER MANUAL
    
    // NENHUM OUTRO MÉTODO GETTER OU SETTER DEVE ESTAR AQUI!
}