// src/ResultDisplay.js
import React from 'react';

const ResultDisplay = ({ resultados }) => {
  if (!resultados) {
    return null;
  }

  const {
    nome_ativo,
    prejuizoMaximo,
    lucroMaximo,
    pontoEquilibrio,
    relacaoRiscoRetorno
  } = resultados;

  return (
    <div className="resultados">
      <h2>Análise da Operação (Bear Call Spread)</h2>
      <div className="resultado-item">
        <strong>Prejuízo Máximo:</strong> R$ {prejuizoMaximo.toFixed(2)}
      </div>
      <div className="resultado-item">
        <strong>Lucro Máximo:</strong> R$ {lucroMaximo.toFixed(2)}
      </div>
      <div className="resultado-item">
        <strong>Ponto de Equilíbrio ({nome_ativo}):</strong> R$ {pontoEquilibrio.toFixed(2)}
      </div>
      <div className="resultado-item">
        <strong>Relação Risco x Retorno:</strong> {relacaoRiscoRetorno.toFixed(2)}
      </div>
      <div className="analise-final">
        {relacaoRiscoRetorno >= 1 ? (
          <p className="warning">A relação risco-retorno é desfavorável. O prejuízo máximo é maior ou igual ao lucro máximo.</p>
        ) : (
          <p className="success">A relação risco-retorno é favorável. O prejuízo máximo é menor que o lucro máximo.</p>
        )}
      </div>
    </div>
  );
};

export default ResultDisplay;