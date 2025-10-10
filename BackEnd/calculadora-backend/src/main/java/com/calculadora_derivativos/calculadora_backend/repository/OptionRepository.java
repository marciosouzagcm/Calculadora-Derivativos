package com.calculadora_derivativos.calculadora_backend.repository;

import com.calculadora_derivativos.calculadora_backend.model.Option;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface OptionRepository extends JpaRepository<Option, Long> {

    // Adicionado para buscar uma opção específica por Ativo, Strike e Tipo (ex: "CALL")
    Optional<Option> findByIdAcaoAndStrikeAndTipo(String idAcao, Double strike, String tipo);
    
    // Método anterior (mantido)
    List<Option> findByIdAcao(String idAcao);
}