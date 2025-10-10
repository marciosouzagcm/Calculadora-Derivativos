package com.calculadora_derivativos.calculadora_backend.model;

import lombok.Data; // Assegure-se de que o Lombok está no seu pom.xml

@Data
public class ManualSpreadInput {
    
    // Dados do Ativo
    private String nomeAtivo;
    private Double valorAtivo;
    
    // Dados da Call Vendida (Strike Menor)
    private Double strikeCallVendida; 
    private Double premioCallVendida; // Prêmio em R$ digitado
    
    // Dados da Call Comprada (Strike Maior)
    private Double strikeCallComprada; 
    private Double premioCallComprada; // Prêmio em R$ digitado
    
    // Dados Operacionais
    private Integer quantidade;
    private Double taxas; // Taxas e emolumentos por contrato
}