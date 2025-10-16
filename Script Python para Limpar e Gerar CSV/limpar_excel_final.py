import pandas as pd
import os
import re
import numpy as np

# =========================================================================
# 1. Configurações - CÓDIGO FINAL E DEFINITIVO (V31 - COM VENCIMENTO E DIAS ÚTEIS)
# =========================================================================

# Ajuste o caminho do arquivo para o local correto na sua máquina
nome_arquivo_excel = r"C:\Users\DELL\Downloads\Opções PETR4 - CALLs e PUTs - lista, pesquisa e cotações.xlsx"
nome_arquivo_saida = 'opcoes_final_tratado.csv'

# --- MAPEAMENTOS DE VERSÃO E CORREÇÃO (V31) ---
# Dicionário onde a CHAVE é o NOME DA COLUNA FINAL (padronizado), 
# e o VALOR é uma lista de nomes possíveis no Excel.
COLUNAS_MAP = {
    'ticker': ['Ticker'], 
    # NOVAS COLUNAS INTEGRADA
    'vencimento': ['Vencimento'],
    'diasUteis': ['Dias úteis'],
    # FIM NOVAS COLUNAS
    'tipo': ['Tipo', 'TipoF.M.'],
    'strike': ['Strike'], 
    'premioPct': ['Prêmio', 'Último', 'Último '], # Adicionado 'Último ' para maior robustez
    'volImplicita': ['Vol. Implícita (%)', 'Vol. Impl.', 'Vol. Implícita'], 
    'delta': ['Delta'],
    'gamma': ['Gamma'],
    'theta': ['Theta ($)', 'Theta (%)', 'Theta'], # Prioriza Theta ($)
    'vega': ['Vega'],
    'dataHora': ['Data/Hora'], # Mantido para referência
}

# Colunas que serão processadas e salvas no CSV FINAL (Nomes padronizados)
COLUNAS_FINAIS = [
    'idAcao', 'ticker', 'vencimento', 'diasUteis', 'tipo', 
    'strike', 'premioPct', 'volImplicita', 'delta', 'gamma', 
    'theta', 'vega', 'dataHora'
]

# Fatores de Correção
FATOR_CORRECAO_ESCALA_PRECO = 100.0
FATOR_CORRECAO_ESCALA_GREGA = 10000.0
FATOR_CORRECAO_ESCALA_VI = 100.0

# =========================================================================

try:
    # Tenta extrair o ticker base do nome do arquivo
    match = re.search(r'Opções\s+(\w+)', os.path.basename(nome_arquivo_excel))
    ativo_base = match.group(1).upper() if match else 'BOVA11'
except Exception:
    ativo_base = 'BOVA11'

print(f"Iniciando limpeza e formatação FINAL (V31). Ativo Base: {ativo_base}")

if not os.path.exists(nome_arquivo_excel):
    print(f"ERRO: Arquivo não encontrado no caminho: {nome_arquivo_excel}")
else:
    try:
        # 2. DETECÇÃO E LEITURA DE DADOS
        
        # Leitura de um pequeno trecho para detectar o cabeçalho (SkipRows=1 pula a primeira linha, lendo o cabeçalho na linha 2)
        df_header_detect = pd.read_excel(
            nome_arquivo_excel,
            header=None,
            skiprows=1,
            nrows=1,
            engine='openpyxl'
        )
        
        header_original = df_header_detect.iloc[0].astype(str).str.strip().tolist()
        
        # Encontra o mapeamento de índices do Excel para os nomes de colunas finais
        colunas_encontradas = {}
        for nome_final, nomes_possiveis in COLUNAS_MAP.items():
            encontrado = False
            for nome_excel in nomes_possiveis:
                # Usa 'in' para busca parcial (mais robusto)
                for idx, col_nome_original in enumerate(header_original):
                    if isinstance(col_nome_original, str) and nome_excel.lower() in col_nome_original.lower():
                        colunas_encontradas[nome_final] = idx
                        encontrado = True
                        break
                if encontrado:
                    break
            
            if not encontrado:
                 print(f"AVISO: Coluna '{nome_final}' não encontrada em nenhuma das versões de nome.")

        indices_para_leitura = list(colunas_encontradas.values())
        
        # Verifica se colunas críticas estão presentes (Ticker, Strike, Prêmio)
        colunas_criticas = ['ticker', 'strike', 'premioPct']
        if not all(c in colunas_encontradas for c in colunas_criticas):
             raise ValueError(f"As colunas essenciais ({', '.join(colunas_criticas)}) não foram encontradas no Excel. Revise o nome do cabeçalho.")
        
        # 2b. Leitura Final dos Dados
        df_dados = pd.read_excel(
            nome_arquivo_excel,
            header=None,
            skiprows=2, # Pula o cabeçalho mesclado e a linha de headers
            usecols=indices_para_leitura,
            engine='openpyxl',
            # Lê tudo como string para evitar conversões automáticas e perda de precisão/formato
            dtype={idx: str for idx in indices_para_leitura}
        )
        
        # 3. Mapeamento, Renomeação e Limpeza
        # Garante a ordem correta das colunas lidas
        colunas_lidas_ordem = [k for k, v in sorted(colunas_encontradas.items(), key=lambda item: item[1])]
        df_dados.columns = colunas_lidas_ordem

        # --- Limpeza de Strings e Conversão para Numérico ---
        # Adicionado 'diasUteis' para limpeza numérica
        colunas_numericas_limpar = ['strike', 'premioPct', 'volImplicita', 'delta', 'gamma', 'theta', 'vega', 'diasUteis']

        for col in colunas_numericas_limpar:
             if col in df_dados.columns:
                # Remove espaços, remove ponto de milhar e transforma vírgula em ponto decimal
                df_dados[col] = df_dados[col].astype(str).str.strip().str.replace('.', '', regex=False).str.replace(',', '.', regex=False)
                df_dados[col] = pd.to_numeric(df_dados[col], errors='coerce')
        
        # --- CORREÇÃO DE ESCALA E CRIAÇÃO DO DF FINAL ---
        df_final = df_dados.copy()
        df_final.insert(0, 'idAcao', ativo_base)

        # Aplica correção de escala
        df_final['strike'] = (df_final['strike'] / FATOR_CORRECAO_ESCALA_PRECO).round(4)
        df_final['premioPct'] = (df_final['premioPct'] / FATOR_CORRECAO_ESCALA_PRECO).round(4)
        
        # Correção das Gregas (Exceto volImplicita)
        for col in ['volImplicita', 'delta', 'gamma', 'theta', 'vega']:
            if col in df_final.columns:
                fator = FATOR_CORRECAO_ESCALA_GREGA if col in ['delta', 'gamma', 'theta', 'vega'] else FATOR_CORRECAO_ESCALA_VI
                
                # Theta é negativo (perda de valor com o tempo), o abs() é removido para theta se for a única grega negativa na planilha
                # Usamos abs() para as outras gregas, que normalmente são positivas na planilha.
                if col == 'theta':
                    # Se Theta estiver em R$ (Theta $), geralmente é um valor negativo. Ajustamos a escala.
                    df_final[col] = (df_final[col] / fator).round(4)
                else:
                    df_final[col] = (df_final[col].abs() / fator).round(4)

        # --- Formatação Final das Colunas não Numéricas ---
        if 'vencimento' in df_final.columns:
            # Converte para data e formata para 'YYYY-MM-DD', forçando a leitura como dia/mês/ano (dayfirst=True)
            df_final['vencimento'] = pd.to_datetime(df_final['vencimento'], errors='coerce', dayfirst=True).dt.strftime('%Y-%m-%d')
            
        if 'tipo' in df_final.columns:
            # Padroniza 'Tipo' para CALL/PUT
            df_final['tipo'] = df_final['tipo'].astype(str).str.upper().str.strip().str[0]
            # Mapeamento para CALL/PUT, incluindo as letras E e A do seu exemplo
            df_final['tipo'] = df_final['tipo'].replace({'C': 'CALL', 'P': 'PUT', 'E': 'CALL', 'A': 'PUT'}) 
        
        # 4. Substitui quaisquer valores NaN (resultantes de 'coerce') por 0.0
        df_final = df_final.fillna(0.0) 

        # --- FILTRO DE QUALIDADE FINAL ---
        # ADIÇÃO DO FILTRO PARA DELTA E VEGA > 0.0
        df_final = df_final[
            (df_final['ticker'].astype(str).str.len() > 5) &
            (df_final['strike'] > 0.0) &
            (df_final['premioPct'] > 0.0) &
            (df_final['delta'].abs() > 0.0) &  # Garante que haja sensibilidade ao preço
            (df_final['vega'].abs() > 0.0)     # Garante que haja sensibilidade à volatilidade
        ].copy()
        
        # 5. Salvar no CSV
        # Garante que só as colunas presentes e na ordem final sejam salvas
        colunas_presentes_finais = [c for c in COLUNAS_FINAIS if c in df_final.columns]
        df_final.to_csv(nome_arquivo_saida, index=False, columns=colunas_presentes_finais)
        
        df_print = df_final.reindex(columns=colunas_presentes_finais)

        print("===================================================================")
        print(f"SUCESSO! CSV limpo e formatado gerado: {nome_arquivo_saida}")
        print(f"Linhas válidas processadas: {len(df_final)}")
        print(f"Colunas encontradas e utilizadas: {list(df_print.columns)}")
        print(f"Primeiras linhas do DataFrame FINAL: \n{df_print.head()}")
        print("===================================================================")

    except Exception as e:
        print(f"Erro ao ler ou processar o arquivo Excel: {e}")