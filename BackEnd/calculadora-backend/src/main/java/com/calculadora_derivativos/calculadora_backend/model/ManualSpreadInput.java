package com.calculadora_derivativos.calculadora_backend.model;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ManualSpreadInput {
    
    // Dados do Ativo
    private String nomeAtivo;
    private BigDecimal valorAtivo; // Alterado para BigDecimal
    
    // Dados da Call Vendida (Strike Menor)
    private BigDecimal strikeCallVendida; // Alterado para BigDecimal
    private BigDecimal premioCallVendida; // Alterado para BigDecimal
    
    // Dados da Call Comprada (Strike Maior)
    private BigDecimal strikeCallComprada; // Alterado para BigDecimal
    private BigDecimal premioCallComprada; // Alterado para BigDecimal
    
    // Dados Operacionais
    private Integer quantidade;
    private BigDecimal taxas; // Alterado para BigDecimal
}