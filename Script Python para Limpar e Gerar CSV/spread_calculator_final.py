import pandas as pd
import numpy as np
import os
import math 

# =========================================================================
# 1. CONSTANTES DE OPERAÇÃO (Simulam dados fixos do sistema)
# =========================================================================
QUANTIDADE_CONTRATOS = 100      # Lote padrão (10 lotes de 100)
TAXAS_TOTAIS_OPERACAO = 44.00    # Simula taxas totais em R$

# FATOR CRÍTICO: Definimos como 1.0 porque o strike já está na 
# escala R$ por unidade (Ex: 137.00 no CSV), e não em centavos.
FATOR_STRIKE_UNITARIO = 1.0   # Fator de conversão (137.00 / 1.0 = 137.00 R$)

# =========================================================================
# FUNÇÃO DE FORMATAÇÃO (Correção da Pontuação CRÍTICA para grandes números)
# =========================================================================

def formatar_moeda_br(valor):
    """
    Formata um valor float para o padrão de moeda brasileiro (X.XXX,XX).
    Usa manipulação de string para robustez, evitando problemas de locale/ambiente.
    """
    if valor is None or math.isnan(valor):
        return "N/A"
    
    # 1. Formata o valor para string com 2 casas decimais
    valor_str = f"{valor:.2f}"
    
    # Separa a parte inteira e decimal
    if '.' in valor_str:
        parte_inteira, parte_decimal = valor_str.split('.')
    else:
        parte_inteira = valor_str
        parte_decimal = '00'

    # 2. Insere o separador de milhares (ponto) na parte inteira (ex: 141000 -> 141.000)
    inteira_formatada = ""
    # Itera de trás para frente
    for i, digito in enumerate(reversed(parte_inteira)):
        if i > 0 and i % 3 == 0:
            # Insere o ponto a cada 3 dígitos (exceto no primeiro)
            inteira_formatada += "."
        inteira_formatada += digito
        
    # 3. Reverte a string formatada e adiciona a vírgula decimal
    return "".join(reversed(inteira_formatada)) + "," + parte_decimal


# =========================================================================
# 2. FUNÇÕES AUXILIARES
# =========================================================================

def buscar_metrics_do_csv(df_opcoes, ativo, strike, tipo):
    """
    Busca todas as métricas (VI, Delta, Gamma, Theta, Vega) no DataFrame.
    """
    resultado = df_opcoes[
        (df_opcoes['idAcao'] == ativo) & 
        (df_opcoes['strike'] == strike) &
        (df_opcoes['tipo'] == tipo)
    ]
    
    if not resultado.empty:
        # VI está em decimal, multiplicamos por 100 para %
        vi = resultado['volImplicita'].iloc[0] * 100 
        delta = resultado['delta'].iloc[0]
        gamma = resultado['gamma'].iloc[0]
        theta = resultado['theta'].iloc[0]
        vega = resultado['vega'].iloc[0]
        
        return vi, delta, gamma, theta, vega
    else:
        # Retorna NaN para todas as métricas se não encontrar
        return np.nan, np.nan, np.nan, np.nan, np.nan

# =========================================================================
# 3. FUNÇÕES DE CÁLCULO DE MÉTRICAS (Específicas por tipo de Spread)
# =========================================================================

def calcular_bear_call_spread_metrics(strike_venda, premio_venda, strike_compra, premio_compra, quantidade, taxas):
    """
    Calcula as métricas do Bear Call Spread (Crédito).
    Vende Strike MENOR, Compra Strike MAIOR.
    """
    # 1. Crédito Líquido por Unidade (Receita Inicial)
    credito_liquido_unidade = premio_venda - premio_compra
    
    if credito_liquido_unidade <= 0:
        return -math.inf, 0, 0, 0 

    # 2. Lucro Máximo (Max Profit) - Fluxo de Caixa Inicial (Prêmio Bruto - Taxas)
    lucro_maximo_total = (credito_liquido_unidade * quantidade) - taxas
    
    # 3. Prejuízo Máximo (Max Loss) - Distância dos strikes menos o crédito
    diferenca_strikes_unitario = (strike_compra - strike_venda) / FATOR_STRIKE_UNITARIO 
    prejuizo_maximo_total = (diferenca_strikes_unitario - credito_liquido_unidade) * quantidade + taxas
    
    if lucro_maximo_total <= 0:
         return -math.inf, 0, 0, 0 

    # 4. Ponto de Equilíbrio (Breakeven)
    ponto_equilibrio = strike_venda + (credito_liquido_unidade * FATOR_STRIKE_UNITARIO)
    
    # 5. Relação Risco x Retorno (Lucro / Risco) - Métrica de Otimização
    relacao_risco_retorno = lucro_maximo_total / prejuizo_maximo_total
    
    return relacao_risco_retorno, lucro_maximo_total, prejuizo_maximo_total, ponto_equilibrio


def calcular_bear_put_spread_metrics(strike_compra, premio_compra, strike_venda, premio_venda, quantidade, taxas):
    """
    Calcula as métricas do Bear Put Spread (Débito).
    Compra Strike MAIOR, Vende Strike MENOR.
    """
    # 1. Custo Líquido por Unidade (Débito Inicial)
    custo_liquido_unidade = premio_compra - premio_venda
    
    if custo_liquido_unidade <= 0:
        return -math.inf, 0, 0, 0 

    # 2. Prejuízo Máximo (Max Loss) - Fluxo de Caixa Inicial (Custo Bruto + Taxas)
    prejuizo_maximo_total = (custo_liquido_unidade * quantidade) + taxas
    
    # 3. Lucro Líquido Máximo Total
    lucro_bruto_maximo_unidade = (strike_compra - strike_venda) / FATOR_STRIKE_UNITARIO 
    lucro_maximo_total = (lucro_bruto_maximo_unidade - custo_liquido_unidade) * quantidade - taxas
    
    if lucro_maximo_total <= 0:
         return -math.inf, 0, 0, 0 

    # 4. Ponto de Equilíbrio (Breakeven)
    ponto_equilibrio = strike_compra - (custo_liquido_unidade * FATOR_STRIKE_UNITARIO)
    
    # 5. Relação Risco x Retorno (Lucro / Risco) - Métrica de Otimização
    relacao_risco_retorno = lucro_maximo_total / prejuizo_maximo_total
    
    return relacao_risco_retorno, lucro_maximo_total, prejuizo_maximo_total, ponto_equilibrio


# =========================================================================
# 4. FUNÇÕES DE OTIMIZAÇÃO (DB Scan)
# =========================================================================

def encontrar_melhor_bear_call_spread(df_opcoes, ativo_base, cotação_atual, quantidade, taxas):
    """
    Analisa o CSV e encontra o Bear Call Spread com a melhor relação Risco x Retorno.
    """
    calls_validas = df_opcoes[
        (df_opcoes['idAcao'] == ativo_base) & 
        (df_opcoes['tipo'] == 'CALL') &
        (df_opcoes['premioPct'] > 0.0)
    ].copy()
    
    if calls_validas.empty:
        return None, "Nenhuma opção CALL válida encontrada para o ativo."

    calls_validas.sort_values(by='strike', inplace=True)
    
    melhor_relacao = -math.inf
    melhor_spread = None
    
    for i in range(len(calls_validas)):
        call_venda = calls_validas.iloc[i] # Opção para ser vendida (Strike MENOR)
        
        for j in range(len(calls_validas)):
            if i == j: 
                continue
            
            call_compra = calls_validas.iloc[j] # Opção para ser comprada (Strike MAIOR)

            # Regra Fundamental: Vende Strike MENOR, Compra Strike MAIOR
            if call_venda['strike'] < call_compra['strike']:
                
                # O prêmio já está na escala R$ por unidade
                premio_venda_R = call_venda['premioPct']
                premio_compra_R = call_compra['premioPct'] 
                
                relacao, lucro_max, prejuizo_max, ponto_equilibrio = calcular_bear_call_spread_metrics(
                    call_venda['strike'], premio_venda_R, 
                    call_compra['strike'], premio_compra_R, 
                    quantidade, taxas
                )
                
                if relacao > melhor_relacao:
                    melhor_relacao = relacao
                    
                    # Extração de todas as métricas, incluindo Gregas
                    vi_venda, delta_v, gamma_v, theta_v, vega_v = buscar_metrics_do_csv(df_opcoes, ativo_base, call_venda['strike'], 'CALL')
                    vi_compra, delta_c, gamma_c, theta_c, vega_c = buscar_metrics_do_csv(df_opcoes, ativo_base, call_compra['strike'], 'CALL')
                    
                    melhor_spread = {
                        "tipo_operacao": "Bear Call Spread (Crédito)",
                        "ativo_base": ativo_base,
                        "cotação_atual": cotação_atual,
                        "strike_venda": call_venda['strike'],
                        "premio_venda": premio_venda_R,
                        "ticker_venda": call_venda['ticker'],
                        "strike_compra": call_compra['strike'],
                        "premio_compra": premio_compra_R,
                        "ticker_compra": call_compra['ticker'],
                        "premio_liquido_unidade": premio_venda_R - premio_compra_R,
                        "lucro_maximo_total": lucro_max, # É o Crédito Líquido Total (Prêmio Bruto - Taxas)
                        "prejuizo_maximo_total": prejuizo_max,
                        "ponto_equilibrio": ponto_equilibrio,
                        "relacao_risco_retorno": relacao,
                        "vi_venda": vi_venda,
                        "vi_compra": vi_compra,
                        
                        # Armazenamento das Gregas
                        "delta_venda": delta_v, "gamma_venda": gamma_v, "theta_venda": theta_v, "vega_venda": vega_v,
                        "delta_compra": delta_c, "gamma_compra": gamma_c, "theta_compra": theta_c, "vega_compra": vega_c,
                        
                        # Gregas Líquidas (Net Greeks)
                        "net_delta": delta_v - delta_c,    # Short Call Delta - Long Call Delta
                        "net_gamma": gamma_v - gamma_c,    # Short Gamma - Long Gamma
                        "net_theta": theta_v - theta_c,    # Short Theta - Long Theta
                        "net_vega": vega_v - vega_c,      # Short Vega - Long Vega
                    }
                    
    if melhor_spread:
        return melhor_spread, "OK"
    else:
        return None, "Nenhuma combinação de Bear Call Spread válida e lucrativa foi encontrada no mercado simulado (CSV)."


def encontrar_melhor_bear_put_spread(df_opcoes, ativo_base, cotação_atual, quantidade, taxas):
    """
    Analisa o CSV e encontra o Bear Put Spread com a melhor relação Risco x Retorno.
    """
    puts_validas = df_opcoes[
        (df_opcoes['idAcao'] == ativo_base) & 
        (df_opcoes['tipo'] == 'PUT') &
        (df_opcoes['premioPct'] > 0.0)
    ].copy()
    
    if puts_validas.empty:
        return None, "Nenhuma opção PUT válida encontrada para o ativo."

    puts_validas.sort_values(by='strike', inplace=True)
    
    melhor_relacao = -math.inf
    melhor_spread = None
    
    for i in range(len(puts_validas)):
        put_compra = puts_validas.iloc[i] # Opção para ser comprada (Strike MAIOR)
        
        for j in range(len(puts_validas)):
            if i == j: 
                continue
            
            put_venda = puts_validas.iloc[j] # Opção para ser vendida (Strike MENOR)

            # Regra Fundamental: Compra Strike MAIOR, Vende Strike MENOR
            if put_compra['strike'] > put_venda['strike']:
                
                # O prêmio já está na escala R$ por unidade
                premio_compra_R = put_compra['premioPct']
                premio_venda_R = put_venda['premioPct']
                
                relacao, lucro_max, prejuizo_max, ponto_equilibrio = calcular_bear_put_spread_metrics(
                    put_compra['strike'], premio_compra_R, 
                    put_venda['strike'], premio_venda_R, 
                    quantidade, taxas
                )
                
                if relacao > melhor_relacao:
                    melhor_relacao = relacao
                    
                    # Extração de todas as métricas, incluindo Gregas
                    vi_compra, delta_c, gamma_c, theta_c, vega_c = buscar_metrics_do_csv(df_opcoes, ativo_base, put_compra['strike'], 'PUT')
                    vi_venda, delta_v, gamma_v, theta_v, vega_v = buscar_metrics_do_csv(df_opcoes, ativo_base, put_venda['strike'], 'PUT')
                    
                    melhor_spread = {
                        "tipo_operacao": "Bear Put Spread (Débito)",
                        "ativo_base": ativo_base,
                        "cotação_atual": cotação_atual,
                        "strike_compra": put_compra['strike'],
                        "premio_compra": premio_compra_R,
                        "ticker_compra": put_compra['ticker'],
                        "strike_venda": put_venda['strike'],
                        "premio_venda": premio_venda_R,
                        "ticker_venda": put_venda['ticker'],
                        "custo_liquido_unidade": premio_compra_R - premio_venda_R,
                        "lucro_maximo_total": lucro_max,
                        "prejuizo_maximo_total": prejuizo_max, # É o Débito Líquido Total (Custo Bruto + Taxas)
                        "ponto_equilibrio": ponto_equilibrio,
                        "relacao_risco_retorno": relacao,
                        "vi_compra": vi_compra,
                        "vi_venda": vi_venda,

                        # Armazenamento das Gregas
                        "delta_compra": delta_c, "gamma_compra": gamma_c, "theta_compra": theta_c, "vega_compra": vega_c,
                        "delta_venda": delta_v, "gamma_venda": gamma_v, "theta_venda": theta_v, "vega_venda": vega_v,
                        
                        # Gregas Líquidas (Net Greeks)
                        "net_delta": delta_c - delta_v,    # Long Put Delta - Short Put Delta
                        "net_gamma": gamma_c - gamma_v,    # Long Gamma - Short Gamma
                        "net_theta": theta_c - theta_v,    # Long Theta - Short Theta
                        "net_vega": vega_c - vega_v,      # Long Vega - Short Vega
                    }
                    
    if melhor_spread:
        return melhor_spread, "OK"
    else:
        return None, "Nenhuma combinação de Bear Put Spread válida e lucrativa foi encontrada no mercado simulado (CSV)."


# =========================================================================
# 5. EXECUÇÃO DA SIMULAÇÃO
# =========================================================================
def executar_simulacao():
    
    try:
        if not os.path.exists('opcoes_excel_tratado.csv'):
            # Se o arquivo não existir, criamos um DataFrame simulado com seus dados
            print("AVISO: Arquivo 'opcoes_final_tratado.csv' não encontrado. Usando dados SIMULADOS fornecidos pelo usuário.")
            data = {
                'idAcao': ['BOVA11', 'BOVA11', 'BOVA11', 'BOVA11', 'BOVA11', 'BOVA11', 'BOVA11'],
                'ticker': ['BOVAW124W1', 'BOVAK137W1', 'BOVAK149W1', 'BOVAW147W1', 'BOVAW141W1', 'BOVAK144W1', 'BOVAK143W1'],
                'tipo': ['PUT', 'CALL', 'CALL', 'PUT', 'PUT', 'CALL', 'CALL'],
                'strike': [124.0, 137.0, 149.0, 147.0, 141.0, 144.0, 143.0],
                'premioPct': [0.06, 5.81, 0.46, 5.38, 1.57, 1.67, 2.13],
                'volImplicita': [0.2177, 0.1156, 0.1363, 0.1581, 0.1346, 0.1272, 0.1314],
                'delta': [-1.72, 87.15, 15.25, -71.93, -37.76, 42.29, 48.7],
                'gamma': [0.45, 4.19, 3.99, 4.91, 6.5, 7.09, 6.99],
                'theta': [-0.69, -9.14, -4.15, 1.52, -1.37, -7.95, -8.69],
                'vega': [185.04, 910.15, 1022.46, 1463.47, 1650.01, 1700.66, 1730.96]
            }
            df_opcoes = pd.DataFrame(data)
        else:
            df_opcoes = pd.read_csv('opcoes_final_para_mysql.csv')
        
        df_opcoes['strike'] = pd.to_numeric(df_opcoes['strike'], errors='coerce')

        # === Solicita Ticker e Preço ao Usuário ===
        print("\n--- OTIMIZADOR DE SPREADS DE BAIXA ---")
        
        ativo_base = input("Digite o Ticker do ativo (Ex: BOVA11): ").strip().upper() or 'BOVA11'
        
        cotação_atual = 0.0
        while True:
            try:
                # Permite vírgula ou ponto como separador decimal para a cotação
                preco_input = input("Digite o Valor Atual do ativo (Ex: 120,50): ").strip().replace(',', '.')
                cotação_atual = float(preco_input) if preco_input else 120.50
                break
            except ValueError:
                print("Entrada inválida. Por favor, digite um número (Ex: 120.50).")
        
        # === Solicita Dias para Vencimento ===
        dias_vencimento = 0
        while True:
            try:
                dias_input = input("Digite o número de dias até o vencimento das opções (Ex: 23): ").strip()
                # Usamos 23 como um valor razoável se o usuário deixar vazio
                dias_vencimento = int(dias_input) if dias_input else 23 
                break
            except ValueError:
                print("Entrada inválida. Por favor, digite um número inteiro.")
        
        # === Seleção da Operação ===
        print("\n--- Seleção da Otimização ---")
        while True:
            TIPO_OPERACAO = input("Qual Spread deseja otimizar? (1: BEAR CALL - Crédito, 2: BEAR PUT - Débito): ").strip()
            if TIPO_OPERACAO in ['1', '2']:
                break
            print("Opção inválida. Digite 1 para BEAR CALL ou 2 para BEAR PUT.")
        
        print("---------------------------------\n")

        # 1. Executa a Otimização
        if TIPO_OPERACAO == '1':
            melhor_resultado, status = encontrar_melhor_bear_call_spread(
                df_opcoes, ativo_base, cotação_atual, 
                QUANTIDADE_CONTRATOS, TAXAS_TOTAIS_OPERACAO
            )
            natureza = "CRÉDITO (Embolsando Prêmios)"
        else: # TIPO_OPERACAO == '2'
            melhor_resultado, status = encontrar_melhor_bear_put_spread(
                df_opcoes, ativo_base, cotação_atual, 
                QUANTIDADE_CONTRATOS, TAXAS_TOTAIS_OPERACAO
            )
            natureza = "DÉBITO (Desembolsando Custo)"

        # 2. Apresentação do Resultado
        print("=" * 60)
        print(f"RELATÓRIO DE OTIMIZAÇÃO: {melhor_resultado['tipo_operacao'].upper() if melhor_resultado else 'N/A'}")
        print("=" * 60)
        
        if melhor_resultado:
            res = melhor_resultado
            
            # --- CÁLCULO DE VALORES DE EXIBIÇÃO ---
            strike_venda_unit = res['strike_venda'] / FATOR_STRIKE_UNITARIO
            strike_compra_unit = res['strike_compra'] / FATOR_STRIKE_UNITARIO
            
            # Cálculo Nocional: Strike * Quantidade de Contratos
            valor_nocional_venda = strike_venda_unit * QUANTIDADE_CONTRATOS
            valor_nocional_compra = strike_compra_unit * QUANTIDADE_CONTRATOS
            ponto_equilibrio_unit = res['ponto_equilibrio'] / FATOR_STRIKE_UNITARIO

            print(f"STATUS: {status}")
            print(f"Ativo: {res['ativo_base']} | Cotação Atual: R$ {formatar_moeda_br(res['cotação_atual'])}")
            print(f"Vencimento: {dias_vencimento} dias")
            print(f"Lotes de {QUANTIDADE_CONTRATOS} Contratos | Taxas Totais: R$ {formatar_moeda_br(TAXAS_TOTAIS_OPERACAO)}")
            print(f"Natureza da Operação: {natureza}")
            print("-" * 60)
            
            # --- Exibição das Pernas ---
            if TIPO_OPERACAO == '1':
                # Bear Call Spread (Crédito)
                print(f"VENDA (Strike Menor): {res['ticker_venda']} (Strike/Unidade: R$ {formatar_moeda_br(strike_venda_unit)} | Prêmio: R$ {formatar_moeda_br(res['premio_venda'])} | Valor Nocional Total: R$ {formatar_moeda_br(valor_nocional_venda)})")
                print(f"COMPRA (Strike Maior): {res['ticker_compra']} (Strike/Unidade: R$ {formatar_moeda_br(strike_compra_unit)} | Prêmio: R$ {formatar_moeda_br(res['premio_compra'])} | Valor Nocional Total: R$ {formatar_moeda_br(valor_nocional_compra)})")
                print("-" * 60)
                
                premio_bruto_unitario = res['premio_liquido_unidade']
                fluxo_bruto_total = premio_bruto_unitario * QUANTIDADE_CONTRATOS
                
                print(f"PRÊMIO LÍQUIDO por unidade: R$ {formatar_moeda_br(premio_bruto_unitario)}")
                print(f"FLUXO DE CAIXA INICIAL (CRÉDITO BRUTO TOTAL): R$ {formatar_moeda_br(fluxo_bruto_total)}")
                print(f"TAXAS: R$ {formatar_moeda_br(TAXAS_TOTAIS_OPERACAO)}")
                
                print("-" * 60)
                print(f"PONTO DE EQUILÍBRIO (Breakeven): R$ {formatar_moeda_br(ponto_equilibrio_unit)} (Por Unidade)")
                
                # LUCRO MÁXIMO (Fluxo de Caixa Líquido)
                print(f"LUCRO MÁXIMO TOTAL (CRÉDITO LÍQUIDO APÓS TAXAS): R$ {formatar_moeda_br(res['lucro_maximo_total'])}")
                print(f"PREJUÍZO MÁXIMO TOTAL: R$ {formatar_moeda_br(res['prejuizo_maximo_total'])}")

            else:
                # Bear Put Spread (Débito)
                print(f"COMPRA (Strike Maior): {res['ticker_compra']} (Strike/Unidade: R$ {formatar_moeda_br(strike_compra_unit)} | Prêmio: R$ {formatar_moeda_br(res['premio_compra'])} | Valor Nocional Total: R$ {formatar_moeda_br(valor_nocional_compra)})")
                print(f"VENDA (Strike Menor): {res['ticker_venda']} (Strike/Unidade: R$ {formatar_moeda_br(strike_venda_unit)} | Prêmio: R$ {formatar_moeda_br(res['premio_venda'])} | Valor Nocional Total: R$ {formatar_moeda_br(valor_nocional_venda)})")
                print("-" * 60)

                custo_bruto_unitario = res['custo_liquido_unidade']
                fluxo_bruto_total = custo_bruto_unitario * QUANTIDADE_CONTRATOS

                print(f"CUSTO LÍQUIDO por unidade: R$ {formatar_moeda_br(custo_bruto_unitario)}")
                print(f"FLUXO DE CAIXA INICIAL (CUSTO BRUTO TOTAL): R$ {formatar_moeda_br(fluxo_bruto_total)}")
                print(f"TAXAS: R$ {formatar_moeda_br(TAXAS_TOTAIS_OPERACAO)}")

                print("-" * 60)
                print(f"PONTO DE EQUILÍBRIO (Breakeven): R$ {formatar_moeda_br(ponto_equilibrio_unit)} (Por Unidade)")

                print(f"LUCRO MÁXIMO TOTAL: R$ {formatar_moeda_br(res['lucro_maximo_total'])}")
                # PREJUÍZO MÁXIMO (Fluxo de Caixa Líquido)
                print(f"PREJUÍZO MÁXIMO TOTAL (DÉBITO LÍQUIDO APÓS TAXAS): R$ {formatar_moeda_br(res['prejuizo_maximo_total'])}")


            print(f"RELAÇÃO RISCO/RETORNO (Lucro/Risco): {res['relacao_risco_retorno']:.2f}")
            print("-" * 60)
            
            # --- ANÁLISE DE GREGAS (UNITÁRIA) ---
            
            if TIPO_OPERACAO == '1': # BEAR CALL
                ticker_v = res['ticker_venda']
                ticker_c = res['ticker_compra']
            else: # BEAR PUT
                ticker_v = res['ticker_venda']
                ticker_c = res['ticker_compra']

            print("--- ANÁLISE DE GREGAS (UNITÁRIA) ---")
            print(f"| {ticker_v} (Venda): Delta={res['delta_venda']:.2f}, Gamma={res['gamma_venda']:.2f}, Theta={res['theta_venda']:.2f}, Vega={res['vega_venda']:.2f}")
            print(f"| {ticker_c} (Compra): Delta={res['delta_compra']:.2f}, Gamma={res['gamma_compra']:.2f}, Theta={res['theta_compra']:.2f}, Vega={res['vega_compra']:.2f}")
            
            print("\n--- POSIÇÃO LÍQUIDA DO SPREAD ---")
            print(f"| NET DELTA: {res['net_delta']:.2f}")
            print(f"| NET GAMMA: {res['net_gamma']:.2f}")
            print(f"| NET THETA: {res['net_theta']:.2f} (Ganho/Perda Diária Teórica)")
            print(f"| NET VEGA: {res['net_vega']:.2f}")

            print("-" * 60)
            print(f"VI VENDIDA (DB): {res['vi_venda']:.2f}% | VI COMPRADA (DB): {res['vi_compra']:.2f}%")
        else:
            print(f"STATUS: {status}")
        
        print("=" * 60)


    except FileNotFoundError:
        print(f"ERRO: Arquivo 'opcoes_final_para_mysql.csv' não encontrado e a simulação de dados falhou. Garanta que o arquivo CSV esteja presente.")
    except Exception as e:
        print(f"Ocorreu um erro na otimização do spread: {e}")

if __name__ == "__main__":
    executar_simulacao()
