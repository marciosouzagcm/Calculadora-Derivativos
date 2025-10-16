package com.calculadora_derivativos.calculadora_backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class SpreadResponse {
    
    private String mensagem;
    private BigDecimal resultadoBruto;
    private BigDecimal resultadoLiquido;
    private BigDecimal probabilidadeSucesso; 

    // CORREÇÃO: Construtor de 4 argumentos NECESSÁRIO para o SpreadService
    public SpreadResponse(String mensagem, BigDecimal resultadoBruto, BigDecimal resultadoLiquido, BigDecimal probabilidadeSucesso) {
        this.mensagem = mensagem;
        this.resultadoBruto = resultadoBruto;
        this.resultadoLiquido = resultadoLiquido;
        this.probabilidadeSucesso = probabilidadeSucesso;
    }
}