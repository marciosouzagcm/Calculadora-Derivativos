import pandas as pd
import os
import re
import numpy as np

# =========================================================================
# 1. Configurações - CÓDIGO FINAL E DEFINITIVO (V28)
# =========================================================================

# Usamos o arquivo original (o único que funciona)
nome_arquivo_excel = r"C:\Users\DELL\Downloads\Opções BOVA11 - CALLs e PUTs - lista, pesquisa e cotações (1).xlsx"
nome_arquivo_saida = 'opcoes_final_tratado.csv'
linhas_a_pular = 2 

# Mapeamento do V27, mas adicionando o Vega (índice 17) para pegar o Delta real
# A ordem é: Ticker, Tipo, Strike, Prêmio, VI (Lixo), Delta (Lixo), Gamma (Gamma), Theta (Delta), Vega (Theta)
# Vamos tentar: 0, 3, 6, 9, 12(VI), 13(Delta), 14(Gamma), 15(Theta), 17(Vega)
indices_colunas_v28 = [
    0,  # Ticker
    3,  # Tipo
    6,  # Strike
    9,  # Prêmio
    12, # <--- VI (Lixo - Volume Financeiro)
    13, # <--- Delta Real (Estava no índice 15 no V27) -> Tentar 13 para a VI real
    14, # <--- Gamma Real
    15, # <--- Theta Real
    17  # <--- Vega Real
]

# Fatores de Correção
FATOR_CORRECAO_ESCALA_PRECO = 100.0    
FATOR_CORRECAO_ESCALA_GREGA = 10000.0  
# Não faremos correção na VI, pois o valor lido é Volume Financeiro
# Vamos manter o divisor de 100.0, só para ter um valor de referência
FATOR_CORRECAO_ESCALA_VI = 100.0 

# =========================================================================

try:
    match = re.search(r'Opções\s+(\w+)', os.path.basename(nome_arquivo_excel))
    ativo_base = match.group(1) if match else 'BOVA11'
except Exception:
    ativo_base = 'BOVA11'

print(f"Iniciando limpeza e formatação FINAL (Rearranjo V28). Ativo Base: {ativo_base}")
if not os.path.exists(nome_arquivo_excel):
    print(f"ERRO: Arquivo não encontrado no caminho: {nome_arquivo_excel}")
else:
    try:
        # 2. Leitura dos Dados
        df = pd.read_excel(
            nome_arquivo_excel,
            header=None,
            skiprows=linhas_a_pular, 
            usecols=indices_colunas_v28,
            engine='openpyxl',
            dtype={idx: str for idx in indices_colunas_v28} 
        )
        
        # 3. Mapeamento, Renomeação e Limpeza
        # Renomeamos as colunas com base no V27. 
        # A ordem do DataFrame LIDO é: Col0, Col1, Col2, Col3, Col4, Col5, Col6, Col7, Col8
        df.columns = ['ticker', 'tipo', 'strike', 'premioPct', 'vol_lixo', 'delta_lixo', 'gamma_real', 'theta_real', 'vega_real']

        # Criamos o DataFrame final com as colunas na ordem correta, RENOMEANDO os campos.
        df_final = pd.DataFrame({
            'idAcao': ativo_base,
            'ticker': df['ticker'],
            'tipo': df['tipo'],
            'strike': df['strike'],
            'premioPct': df['premioPct'],
            'volImplicita': df['vol_lixo'],        # Lendo o Volume Financeiro
            'delta': df['theta_real'],           # Delta Real
            'gamma': df['gamma_real'],           # Gamma Real
            'theta': df['delta_lixo'],           # Theta Real (Estava no índice 13)
            'vega': df['vega_real']              # Vega Real
        })

        colunas_numericas_limpar = ['strike', 'premioPct', 'volImplicita', 'delta', 'gamma', 'theta', 'vega']

        for col in colunas_numericas_limpar:
            df_final[col] = df_final[col].astype(str).str.replace('.', '', regex=False).str.replace(',', '.', regex=False)
            df_final[col] = pd.to_numeric(df_final[col], errors='coerce')
        
        # --- CORREÇÃO DE ESCALA ---
        df_final['strike'] = (df_final['strike'] / FATOR_CORRECAO_ESCALA_PRECO).round(4)
        df_final['premioPct'] = (df_final['premioPct'] / FATOR_CORRECAO_ESCALA_PRECO).round(4)
        
        # A volImplicita ainda será o LIXO (Volume Financeiro), mas com o divisor que a escala precisa.
        df_final['volImplicita'] = np.abs(df_final['volImplicita'] / FATOR_CORRECAO_ESCALA_VI).round(4) 
        
        # Gregas: Divididas por 10000.0 (aplicado às colunas renomeadas)
        df_final['delta'] = (df_final['delta'] / FATOR_CORRECAO_ESCALA_GREGA).round(4) 
        df_final['gamma'] = (df_final['gamma'] / FATOR_CORRECAO_ESCALA_GREGA).round(4) 
        df_final['theta'] = (df_final['theta'] / FATOR_CORRECAO_ESCALA_GREGA).round(4) 
        df_final['vega'] = (df_final['vega'] / FATOR_CORRECAO_ESCALA_GREGA).round(4)
        
        # 4. Substitui quaisquer valores NaN por 0.0
        df_final = df_final.fillna(0.0) 

        # --- FILTRO DE QUALIDADE FINAL ---
        df_final = df_final[ 
            (df_final['ticker'].astype(str).str.len() > 5) & 
            (df_final['strike'] > 0.0) &
            (df_final['premioPct'] > 0.0) &
            (df_final['delta'] != 0.0)
        ].copy()
        
        # 5. Salvar no CSV
        df_final.to_csv(nome_arquivo_saida, index=False)
        
        print("===================================================================")
        print(f"SUCESSO! CSV limpo e formatado gerado: {nome_arquivo_saida}")
        print(f"Linhas válidas processadas: {len(df_final)}")
        print(f"Primeiras linhas do DataFrame FINAL: \n{df_final.head()}")
        print("===================================================================")

    except Exception as e:
        print(f"Erro ao ler ou processar o arquivo Excel: {e}")