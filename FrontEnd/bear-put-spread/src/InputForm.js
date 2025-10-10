// src/InputForm.js
import React, { useState } from 'react';

const InputForm = ({ onCalculate }) => {
  const [formData, setFormData] = useState({
    nome_ativo: '', // <--- NOVO CAMPO
    valor_ativo: '',
    strike_put_comprada: '',
    premio_put_comprada: '',
    strike_put_vendida: '',
    premio_put_vendida: '',
    quantidade: '',
    taxas: ''
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    // O valor do nome_ativo não precisa ser convertido para número
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
      <h2>Dados da Operação</h2>
      {/* NOVO CAMPO DE ENTRADA */}
      <div className="form-group">
        <label>Nome do Ativo (ex: BOVA11, PETR4):</label>
        <input type="text" name="nome_ativo" value={formData.nome_ativo} onChange={handleChange} required />
      </div>
      <div className="form-group">
        <label>Valor do Ativo Atual:</label>
        <input type="number" name="valor_ativo" value={formData.valor_ativo} onChange={handleChange} required />
      </div>
      <div className="form-group">
        <label>Preço de Exercício (Put Comprada):</label>
        <input type="number" name="strike_put_comprada" value={formData.strike_put_comprada} onChange={handleChange} required />
      </div>
      <div className="form-group">
        <label>Valor da Opção (Put Comprada):</label>
        <input type="number" name="premio_put_comprada" value={formData.premio_put_comprada} onChange={handleChange} required />
      </div>
      <div className="form-group">
        <label>Preço de Exercício (Put Vendida):</label>
        <input type="number" name="strike_put_vendida" value={formData.strike_put_vendida} onChange={handleChange} required />
      </div>
      <div className="form-group">
        <label>Valor da Opção (Put Vendida):</label>
        <input type="number" name="premio_put_vendida" value={formData.premio_put_vendida} onChange={handleChange} required />
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