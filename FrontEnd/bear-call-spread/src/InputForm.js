// src/InputForm.js
import React, { useState } from 'react';

const InputForm = ({ onCalculate }) => {
  const [formData, setFormData] = useState({
    nome_ativo: '',
    valor_ativo: '',
    strike_call_vendida: '',
    premio_call_vendida: '',
    strike_call_comprada: '',
    premio_call_comprada: '',
    quantidade: '',
    taxas: ''
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    const parsedValue = name === 'nome_ativo' ? value : parseFloat(value) || '';
    setFormData(prevState => ({
      ...prevState,
      [name]: parsedValue
    }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    onCalculate(formData);
  };

  return (
    <form onSubmit={handleSubmit}>
      <h2>Dados da Operação (Bear Call Spread)</h2>
      <div className="form-group">
        <label>Nome do Ativo:</label>
        <input type="text" name="nome_ativo" value={formData.nome_ativo} onChange={handleChange} required />
      </div>
      <div className="form-group">
        <label>Valor do Ativo Atual:</label>
        <input type="number" name="valor_ativo" value={formData.valor_ativo} onChange={handleChange} required />
      </div>
      <div className="form-group">
        <label>Preço de Exercício (Call Vendida - Menor Strike):</label>
        <input type="number" name="strike_call_vendida" value={formData.strike_call_vendida} onChange={handleChange} required />
      </div>
      <div className="form-group">
        <label>Valor da Opção (Call Vendida):</label>
        <input type="number" name="premio_call_vendida" value={formData.premio_call_vendida} onChange={handleChange} required />
      </div>
      <div className="form-group">
        <label>Preço de Exercício (Call Comprada - Maior Strike):</label>
        <input type="number" name="strike_call_comprada" value={formData.strike_call_comprada} onChange={handleChange} required />
      </div>
      <div className="form-group">
        <label>Valor da Opção (Call Comprada):</label>
        <input type="number" name="premio_call_comprada" value={formData.premio_call_comprada} onChange={handleChange} required />
      </div>
      <div className="form-group">
        <label>Quantidade de Unidades:</label>
        <input type="number" name="quantidade" value={formData.quantidade} onChange={handleChange} required />
      </div>
      <div className="form-group">
        <label>Taxas e Emolumentos:</label>
        <input type="number" name="taxas" value={formData.taxas} onChange={handleChange} required />
      </div>
      <button type="submit">Calcular</button>
    </form>
  );
};

export default InputForm;