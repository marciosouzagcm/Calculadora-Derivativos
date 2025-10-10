// src/App.js
import React, { useState } from 'react';
import InputForm from './InputForm';
import ResultDisplay from './ResultDisplay';
import './App.css';

const App = () => {
  const [resultados, setResultados] = useState(null);

  const handleCalculate = (data) => {
    const {
      nome_ativo, // <--- NOVO CAMPO
      valor_ativo, // Nome alterado para ser mais genérico
      strike_put_comprada,
      premio_put_comprada,
      strike_put_vendida,
      premio_put_vendida,
      quantidade,
      taxas
    } = data;

    const custoLiquidoUnidade = premio_put_comprada - premio_put_vendida;
    const custoTotal = (custoLiquidoUnidade * quantidade) + taxas;
    const prejuizoMaximo = custoTotal;

    const lucroBrutoMaximoUnidade = strike_put_comprada - strike_put_vendida;
    const lucroLiquidoMaximoTotal = (lucroBrutoMaximoUnidade - custoLiquidoUnidade) * quantidade - taxas;
    const lucroMaximo = lucroLiquidoMaximoTotal;

    const pontoEquilibrio = strike_put_comprada - custoLiquidoUnidade;
    
    const relacaoRiscoRetorno = prejuizoMaximo !== 0 ? lucroMaximo / prejuizoMaximo : Infinity;

    setResultados({
      nome_ativo, // <--- PASSA O NOME DO ATIVO PARA O COMPONENTE DE EXIBIÇÃO
      prejuizoMaximo,
      lucroMaximo,
      pontoEquilibrio,
      relacaoRiscoRetorno
    });
  };

  return (
    <div className="container">
      <h1>Análise de Spread de Baixa</h1>
      <InputForm onCalculate={handleCalculate} />
      <ResultDisplay resultados={resultados} />
    </div>
  );
};

export default App;