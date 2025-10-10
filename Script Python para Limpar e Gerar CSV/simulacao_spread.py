import pandas as pd

# =========================================================================
# 1. PARÂMETROS DE ENTRADA DO USUÁRIO (Simulando o Front-end)
# =========================================================================

COTACAO_ATUAL = 142 # Valor do BOVA11 no mercado (Exemplo)
TAXA = 44,00      # 10% (0.10) para o Black & Scholes, se necessário depois
QUANTIDADE = 100     # Lote de contratos

# =========================================================================

nome_arquivo_csv = 'opcoes_final_para_mysql.csv'

try:
    # 2. LER OS DADOS DO ARQUIVO CSV JÁ LIMPO
    df = pd.read_csv(nome_arquivo_csv)
    
    print(f"Lendo {len(df)} linhas de dados limpos do CSV...")

    # 3. TRATAMENTO ESSENCIAL: CALCULAR O PRÊMIO MONETÁRIO
    # O CSV tem premioPct (Ex: 0.05 para 5%). O prêmio monetário é: Prêmio% * Cotação Atual
    
    # 3.1. Convertendo o Strike e Volatilidade para números (se não estiverem)
    df['strike'] = pd.to_numeric(df['strike'], errors='coerce')
    df['premioPct'] = pd.to_numeric(df['premioPct'], errors='coerce')
    df['volImplicita'] = pd.to_numeric(df['volImplicita'], errors='coerce')
    
    # 3.2. Calcula o Prêmio em R$ (valor monetário da opção)
    df['premio_monetario'] = df['premioPct'] * COTACAO_ATUAL
    
    print(f"Prêmio Monetário calculado com base na cotação R$ {COTACAO_ATUAL:.2f}")

    # 4. LÓGICA DE SIMULAÇÃO DE SPREAD (Exemplo)
    
    # Agrupa por ativo base (BOVA11)
    opcoes_bova = df[df['idAcao'] == 'BOVA11']
    
    # Simulação: Encontrar a CALL mais barata e a CALL mais cara
    
    opcoes_call = opcoes_bova[opcoes_bova['tipo'] == 'CALL'].copy()
    
    if opcoes_call.empty:
        print("\nNão há CALLs válidas para análise.")
    else:
        # Exemplo de Spread de Alta (Bull Call Spread): Compra strike baixo, Vende strike alto
        
        # 4.1. Seleciona a Call com o menor Strike e Prêmio (SIMULAÇÃO DE COMPRA)
        call_compra = opcoes_call.sort_values(by=['strike', 'premio_monetario']).iloc[0]
        
        # 4.2. Seleciona a Call com o Strike um pouco maior (SIMULAÇÃO DE VENDA)
        strike_venda_alvo = call_compra['strike'] + 5 # + R$ 5,00 no strike
        
        # Tenta encontrar a opção de venda mais próxima
        call_venda = opcoes_call[opcoes_call['strike'] > strike_venda_alvo].sort_values(by='strike').iloc[0]
        
        # 5. CÁLCULO DO SPREAD FINAL
        
        # Débito/Crédito Inicial = Prêmio Comprada - Prêmio Vendida
        # Se for um spread de débito (compra mais cara que venda), o resultado é positivo.
        spread_liquido = call_compra['premio_monetario'] - call_venda['premio_monetario']
        
        # Ganho Máximo = (Strike Venda - Strike Compra) - Spread Líquido
        ganho_maximo = (call_venda['strike'] - call_compra['strike']) - spread_liquido
        
        print("\n--- Resultado da Simulação de Bull Call Spread ---")
        print(f"1. Compra (Strike Baixo): {call_compra['ticker']} | Strike R$ {call_compra['strike']:.2f} | Prêmio R$ {call_compra['premio_monetario']:.2f}")
        print(f"2. Venda (Strike Alto):   {call_venda['ticker']} | Strike R$ {call_venda['strike']:.2f} | Prêmio R$ {call_venda['premio_monetario']:.2f}")
        print("-" * 40)
        print(f"Spread Líquido (Débito/Crédito) por Contrato: R$ {spread_liquido:.2f}")
        print(f"Custo/Retorno Total da Operação ({QUANTIDADE} contratos): R$ {spread_liquido * QUANTIDADE:.2f}")
        print(f"Ganho Máximo Teórico por Contrato: R$ {ganho_maximo:.2f}")
        print("-------------------------------------------------")

except FileNotFoundError:
    print(f"ERRO: Arquivo {nome_arquivo_csv} não encontrado. Execute o script de limpeza primeiro.")
except Exception as e:
    print(f"Erro na simulação: {e}")