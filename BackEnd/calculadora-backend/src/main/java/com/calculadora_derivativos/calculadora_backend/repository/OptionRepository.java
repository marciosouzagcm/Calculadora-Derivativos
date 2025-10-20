package com.calculadora_derivativos.calculadora_backend.repository;

import com.calculadora_derivativos.calculadora_backend.model.Option;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Importa a anotação de Query
import org.springframework.data.repository.query.Param; // Importa a anotação de Parâmetro
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OptionRepository extends JpaRepository<Option, Long> {
    
    /**
     * Busca uma Option pelo ticker.
     */
    Optional<Option> findByTicker(String ticker); 
    
    /**
     * Busca todas as opções de um ativo (idAcao) com um tipo específico (CALL/PUT).
     * Usado como ponto de partida para a otimização.
     */
    List<Option> findByIdAcaoAndTipo(String idAcao, String tipo);

    /**
     * Busca todas as opções de um ativo (idAcao) com um vencimento e tipo específicos.
     * Essencial para garantir que ambas as pernas do spread estejam no mesmo vencimento.
     */
    List<Option> findByIdAcaoAndVencimentoAndTipo(String idAcao, LocalDate vencimento, String tipo);
    
    /**
     * Busca todos os vencimentos distintos disponíveis para um ativo.
     * Útil para iterar sobre todos os grupos de vencimento possíveis.
     * * AJUSTE FEITO AQUI: Usando @Query para garantir que apenas o campo 'vencimento' seja selecionado como LocalDate.
     */
    @Query("SELECT DISTINCT o.vencimento FROM Option o WHERE o.idAcao = :idAcao ORDER BY o.vencimento ASC")
    List<LocalDate> findDistinctVencimentoByIdAcaoOrderByVencimentoAsc(@Param("idAcao") String idAcao);
}