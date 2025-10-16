package com.calculadora_derivativos.calculadora_backend.repository;

import com.calculadora_derivativos.calculadora_backend.model.Option;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional; // É fundamental ter este import

public interface OptionRepository extends JpaRepository<Option, Long> {
    
    /**
     * Busca uma Option pelo ticker.
     * Retorna Optional<Option> para garantir que o serviço trate o caso de ausência de resultado.
     * Isso resolve o erro de incompatibilidade de tipos na linha 80 do SpreadService.java.
     */
    Optional<Option> findByTicker(String ticker); 
}