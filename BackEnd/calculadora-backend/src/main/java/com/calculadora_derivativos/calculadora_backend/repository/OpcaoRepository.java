package com.calculadora_derivativos.calculadora_backend.repository;

import com.calculadora_derivativos.calculadora_backend.model.Opcao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OpcaoRepository extends JpaRepository<Opcao, Long> {

    /**
     * Busca opções onde o ticker está presente na coluna 'idAcao' OU na coluna 'id_acao'.
     * * @param tickerCamelCase O ticker (ex: BOVA11) para a coluna idAcao.
     * @param tickerSnakeCase O ticker (ex: BOVA11) para a coluna id_acao.
     * @return Lista de Opcoes que correspondem ao ticker em uma das duas colunas.
     */
    List<Opcao> findByTickerCamelCaseOrTickerSnakeCase(String tickerCamelCase, String tickerSnakeCase);
    
    // O antigo findByIdAcao não é mais necessário e pode ser removido ou comentado.
    // List<Opcao> findByIdAcao(String idAcao); 
}