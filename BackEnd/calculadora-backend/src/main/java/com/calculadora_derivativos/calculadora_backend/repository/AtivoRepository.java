package com.calculadora_derivativos.calculadora_backend.repository;

import com.calculadora_derivativos.calculadora_backend.model.Ativo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório JPA para a entidade Ativo.
 * Responsável por buscar dados de ativos subjacentes (ex: PETR4).
 */
@Repository
public interface AtivoRepository extends JpaRepository<Ativo, Long> {

    /**
     * Busca um Ativo subjacente por seu código (Ex: PETR4).
     * @param codigo O código do ativo (ex: PETR4).
     * @return Um Optional contendo o Ativo, se encontrado.
     */
    Optional<Ativo> findByCodigo(String codigo);
}