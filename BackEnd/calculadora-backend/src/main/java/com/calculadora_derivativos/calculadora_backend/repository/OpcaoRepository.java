package com.calculadora_derivativos.calculadora_backend.repository;

import com.calculadora_derivativos.calculadora_backend.model.Opcao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional; // Importe o Optional

/**
 * Repositório JPA para a entidade Opcao.
 * Necessário para a injeção de dependência no SpreadService.
 */
@Repository
public interface OpcaoRepository extends JpaRepository<Opcao, Long> {

    // Método original de busca de exemplo (mantido)
    List<Opcao> findByCodigoStartingWith(String codigoInicial);

    /**
     * NOVO MÉTODO: Busca uma única Opcao por seu código (ticker) exato.
     * Usa Optional para garantir tratamento seguro de casos onde a opção não é encontrada.
     * @param codigo O código exato da opção (ex: PETRJ28).
     * @return Um Optional contendo a Opcao, se encontrada.
     */
    Optional<Opcao> findByCodigo(String codigo);
}