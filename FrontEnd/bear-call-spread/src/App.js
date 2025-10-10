// src/App.js
import React, { useState } from 'react';
import InputForm from './InputForm';
import ResultDisplay from './ResultDisplay';
import './App.css';

const App = () => {
  const [resultados, setResultados] = useState(null);

  const handleCalculate = (data) => {
    const {
      nome_ativo,
      strike_call_comprada,
      premio_call_comprada,
      strike_call_vendida,
      premio_call_vendida,
      quantidade,
      taxas
    } = data;

    // Cálculo do Crédito Líquido (Receita Inicial)
    const creditoLiquidoUnidade = premio_call_vendida - premio_call_comprada;
    const creditoTotalBruto = creditoLiquidoUnidade * quantidade;
    const lucroMaximo = creditoTotalBruto - taxas;

    // Cálculo do Prejuízo Máximo
    const diferencaStrikes = strike_call_comprada - strike_call_vendida;
    const prejuizoMaximo = (diferencaStrikes - creditoLiquidoUnidade) * quantidade + taxas;

    // Cálculo do Ponto de Equilíbrio
    const pontoEquilibrio = strike_call_vendida + creditoLiquidoUnidade;
    
    // Cálculo da Relação Risco x Retorno
    // Note: Em alguns sistemas, a relação é calculada como Risco / Retorno.
    // Usaremos essa abordagem para ser mais fiel à análise financeira.
    const relacaoRiscoRetorno = prejuizoMaximo / lucroMaximo;

    setResultados({
      nome_ativo,
      prejuizoMaximo,
      lucroMaximo,
      pontoEquilibrio,
      relacaoRiscoRetorno
    });
  };

  return (
    <div className="container">
      <h1>Análise de Bear Call Spread</h1>
      <InputForm onCalculate={handleCalculate} />
      <ResultDisplay resultados={resultados} />
    </div>
  );
};

export default App;