package com.calculadora_derivativos.calculadora_backend.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.AllArgsConstructor;
import lombok.extern.jackson.Jacksonized;

/**
 * DTO de requisição para cálculo de uma estratégia de spread manual.
 */
@Value
@Builder
@AllArgsConstructor
@Jacksonized
public class ManualSpreadRequest {

    // Ativo objeto da opção (Ex: "PETR4")
    String ativoSubjacente;

    // Lista de pernas que compõem o spread
    List<PernaSpread> pernas;
}
