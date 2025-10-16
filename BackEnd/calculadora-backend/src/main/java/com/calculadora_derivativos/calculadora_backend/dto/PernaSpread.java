package com.calculadora_derivativos.calculadora_backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor 
@AllArgsConstructor 
public class PernaSpread {
    
    // CORREÇÃO: Nome do campo ajustado para 'ticker' (resolve getTicker())
    private String ticker; 
    
    private int quantidade;     
    private BigDecimal precoUnitario; 
    
    // CORREÇÃO: Campo adicionado (resolve getOperacao())
    // Ex: 'C' (Compra da Perna) ou 'V' (Venda da Perna)
    private String operacao; 
    
    private String tipoOpcao; 
    private BigDecimal strike; 
    private String dataVencimento; 
}