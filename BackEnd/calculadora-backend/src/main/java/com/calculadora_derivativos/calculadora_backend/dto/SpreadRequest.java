package com.calculadora_derivativos.calculadora_backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data 
@NoArgsConstructor 
@AllArgsConstructor 
public class SpreadRequest {

    private String ativoSubjacente; 
    private List<PernaSpread> pernas; 
    private BigDecimal taxasOperacionais;

    // CORREÇÃO: Campo adicionado para resolver cannot find symbol: getCotacaoAtualAtivo()
    private BigDecimal cotacaoAtualAtivo; 

    private BigDecimal taxaJuros; 
    private BigDecimal precoAtual; // Se cotacaoAtualAtivo e precoAtual forem a mesma coisa, use apenas um.
    private String dataCalculo;    
}