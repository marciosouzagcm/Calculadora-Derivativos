import pandas as pd
import numpy as np

# =========================================================================
# 1. DADOS DE ENTRADA DO USUÁRIO (Simulando o Front-end)
# =========================================================================
INPUT_DATA_USUARIO = {
    'nome_ativo': 'BOVA11',
    'valor_ativo': 120.50,          # Cotação atual (R$ 120,50)
    
    # O USUÁRIO FORNECE OS DADOS REAIS DA OPERAÇÃO (Prêmio em R$)
    'strike_call_vendida': 118.00,  # Strike MENOR
    'premio_call_vendida': 3.50,    # Prêmio em R$ real da negociação
    
    'strike_call_comprada': 123.00, # Strike MAIOR
    'premio_call_comprada': 1.00,   # Prêmio em R$ real da negociação
    
    'quantidade': 1000,
    'taxas': 0.05                   # Taxas por contrato (R$ 0,05)
}

# =========================================================================

def buscar_dados_adicionais_db(df_opcoes, ativo, strike):
    """
    Simula a busca de dados na tabela 'opcoes' usando o Strike e Ativo.
    Retorna a Volatilidade Implícita (VI).
    """
    # Filtra o DataFrame (simulando a query SQL no banco)
    resultado = df_opcoes[
        (df_opcoes['idAcao'] == ativo) & 
        (df_opcoes['strike'] == strike)
    ]
    
    if not resultado.empty:
        # Retorna o primeiro valor de VI encontrado para esse Strike
        return resultado['volImplicita'].iloc[0]
    else:
        return np.nan # Retorna NaN se não encontrar (simulando NULL)

def calcular_bear_call_spread(dados_input, df_opcoes_db):
    """
    Calcula as métricas do Bear Call Spread, abstraindo a VI do 'banco'.
    """
    
    # 1. Extração dos dados de entrada
    strike_venda = dados_input['strike_call_vendida']
    strike_compra = dados_input['strike_call_comprada']
    premio_venda = dados_input['premio_call_vendida']
    premio_compra = dados_input['premio_call_comprada']
    quantidade = dados_input['quantidade']
    taxas = dados_input['taxas']
    nome_ativo = dados_input['nome_ativo']
    
    # Validação Básica
    if strike_venda >= strike_compra:
        raise ValueError("O Strike Vendida deve ser MENOR que o Strike Comprada.")
        
    # 2. ABSTRAÇÃO DE DADOS (Buscando Volatilidade do CSV/DB)
    # Na vida real, o seu OptionRepository.findByStrike() faria isso.
    vi_vendida = buscar_dados_adicionais_db(df_opcoes_db, nome_ativo, strike_venda)
    vi_comprada = buscar_dados_adicionais_db(df_opcoes_db, nome_ativo, strike_compra)
    
    # 3. CÁLCULOS PRINCIPAIS
    
    # O crédito é calculado com os prêmios em R$ que o usuário forneceu
    credito_bruto_por_contrato = premio_venda - premio_compra
    
    taxa_total_por_contrato = taxas * 2 
    credito_liquido_por_contrato = credito_bruto_por_contrato - taxa_total_por_contrato
    
    distancia_strikes = strike_compra - strike_venda
    
    prejuizo_maximo_por_contrato = distancia_strikes - credito_liquido_por_contrato
    ganho_maximo_por_contrato = credito_liquido_por_contrato
    
    ponto_equilibrio = strike_venda + credito_liquido_por_contrato
    
    # 4. RESULTADO
    
    resultado = {
        "ativo_base": nome_ativo,
        "credito_liquido": credito_liquido_por_contrato,
        "ganho_maximo_total": ganho_maximo_por_contrato * quantidade,
        "prejuizo_maximo_total": prejuizo_maximo_por_contrato * quantidade,
        "ponto_equilibrio": ponto_equilibrio,
        # Dados Abstraídos da Tabela:
        "vi_call_vendida": vi_vendida,
        "vi_call_comprada": vi_comprada,
        "status_vi_abstrata": "OK" if not pd.isna(vi_vendida) and not pd.isna(vi_comprada) else "Dados de VI incompletos no DB"
    }
    
    return resultado

# =========================================================================
# EXECUÇÃO DA SIMULAÇÃO
# =========================================================================

# Lê o CSV gerado, simulando a conexão com o banco de dados
try:
    df_opcoes = pd.read_csv('opcoes_final_para_mysql.csv')
    
    # Garante que o strike seja numérico para a busca
    df_opcoes['strike'] = pd.to_numeric(df_opcoes['strike'], errors='coerce')

    resultado_final = calcular_bear_call_spread(INPUT_DATA_USUARIO, df_opcoes)

    print("=" * 60)
    print("RELATÓRIO DE CÁLCULO BEAR CALL SPREAD (TESTE DE ABSTRAÇÃO)")
    print("=" * 60)
    print(f"Ativo: {resultado_final['ativo_base']} | Cotação: R$ {INPUT_DATA_USUARIO['valor_ativo']:.2f}")
    print("-" * 60)
    print(f"Venda: Strike R$ {INPUT_DATA_USUARIO['strike_call_vendida']:.2f} | Prêmio R$ {INPUT_DATA_USUARIO['premio_call_vendida']:.2f}")
    print(f"Compra: Strike R$ {INPUT_DATA_USUARIO['strike_call_comprada']:.2f} | Prêmio R$ {INPUT_DATA_USUARIO['premio_call_comprada']:.2f}")
    print("-" * 60)
    print(f"CRÉDITO LÍQUIDO por contrato (Ganho Máx.): R$ {resultado_final['credito_liquido']:.2f}")
    print(f"PONTO DE EQUILÍBRIO (Breakeven):         R$ {resultado_final['ponto_equilibrio']:.2f}")
    print(f"PREJUÍZO MÁXIMO TOTAL ({INPUT_DATA_USUARIO['quantidade']} unid.):    R$ {resultado_final['prejuizo_maximo_total']:.2f}")
    print(f"GANHO MÁXIMO TOTAL ({INPUT_DATA_USUARIO['quantidade']} unid.):      R$ {resultado_final['ganho_maximo_total']:.2f}")
    print("-" * 60)
    print(f"VI (Vendida): {resultado_final['vi_call_vendida']:.2f} | VI (Comprada): {resultado_final['vi_call_comprada']:.2f}")
    print(f"Status da Abstração: {resultado_final['status_vi_abstrata']}")
    print("=" * 60)


except FileNotFoundError:
    print(f"ERRO: Arquivo 'opcoes_final_para_mysql.csv' não encontrado. Garanta que ele foi gerado.")
except Exception as e:
    print(f"Ocorreu um erro no cálculo do spread: {e}")