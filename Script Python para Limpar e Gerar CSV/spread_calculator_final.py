import pandas as pd
import numpy as np
from datetime import datetime
from math import log, sqrt, exp, pi, isnan, inf

# =========================================================================
# 1. CONSTANTES E CONFIGURAÇÃO
# =========================================================================

# Nome do arquivo CSV gerado pelo script de limpeza
NOME_ARQUIVO_CSV = 'opcoes_final_tratado.csv'

# Parâmetros de Mercado (Ajuste conforme necessário para o cálculo Black-Scholes)
TAXA_JUROS_ANUAL = 0.10 

# Parâmetros de CÁLCULO TOTAL
QUANTIDADE_CONTRATOS = 100 # Lote padrão (100 contratos)
TAXAS_TOTAIS_OPERACAO = 44.00 # Simula taxas totais em R$

# Parâmetro de QUALIDADE OBRIGATÓRIO (NOVO)
MIN_RISCO_RETORNO_LIQUIDO = 1.0 # Exige que o lucro máximo seja pelo menos igual ao prejuízo máximo.

# =========================================================================
# 2. FUNÇÕES DE SUPORTE
# =========================================================================

def formatar_moeda_br(valor):
    """
    Formata um valor float para o padrão de moeda brasileiro (X.XXX,XX).
    """
    if valor is None or isnan(valor) or valor in (inf, -inf):
        return "N/A"
    
    # Se o valor for muito próximo de zero, tratamos como zero para evitar -R$ 0,00
    if abs(valor) < 0.005:
        return "R$ 0,00"
        
    # 1. Formata o valor para string com 2 casas decimais
    valor_str = f"{abs(valor):.2f}"
    
    # Separa a parte inteira e decimal
    if '.' in valor_str:
        parte_inteira, parte_decimal = valor_str.split('.')
    else:
        # Garante a formatação correta caso o valor seja um inteiro (e.g., '10' se torna '10,00')
        parte_inteira = valor_str.split('.')[0]
        parte_decimal = '00'

    # 2. Insere o separador de milhares (ponto)
    inteira_formatada = ""
    for i, digito in enumerate(reversed(parte_inteira)):
        if i > 0 and i % 3 == 0:
            inteira_formatada += "."
        inteira_formatada += digito
        
    # 3. Adiciona o sinal e a vírgula decimal
    resultado = "".join(reversed(inteira_formatada)) + "," + parte_decimal
    
    if valor < 0:
        return f"- R$ {resultado}"
    return f"R$ {resultado}"

def buscar_metrics_do_csv(df_csv: pd.DataFrame, ticker_alvo: str, cotação_atual: float) -> dict:
    """Busca métricas do ativo base e usa a cotação informada pelo usuário."""
    # Como o usuário agora fornece o Ticker, usamos o Ticker para buscar as métricas
    resultado = df_csv[df_csv['idAcao'] == ticker_alvo] 
    
    if resultado.empty:
        # Tenta buscar pelo primeiro ticker de opção, se disponível, para obter o vencimento
        resultado = df_csv[df_csv['ticker'].str.startswith(ticker_alvo)].head(1) 
    
    if resultado.empty:
        return {'preco_acao_base': cotação_atual, 'diasUteis': np.nan, 'vencimento': 'N/A'}

    preco_acao_base = cotação_atual
    
    try:
        # Garante que, se a primeira linha tiver NaN, o resultado não será usado no retorno
        dias_uteis = resultado['diasUteis'].iloc[0] if 'diasUteis' in resultado.columns and not resultado.empty else np.nan
        vencimento = resultado['vencimento'].iloc[0] if 'vencimento' in resultado.columns and not resultado.empty else 'N/A'
    except Exception:
        dias_uteis = np.nan
        vencimento = 'N/A'

    return {
        'preco_acao_base': preco_acao_base,
        # Nota: O 'diasUteis' aqui será o primeiro valor após o recálculo na main()
        'diasUteis': dias_uteis, 
        'vencimento': vencimento,
    }

def calcular_net_gregas(res: pd.Series, grega: str, is_credito: bool) -> float:
    """
    Calcula a Net Grega (diferença entre a grega da perna Comprada e Vendida).
    """
    col_venda = f'{grega}_venda'
    col_compra = f'{grega}_compra'
    
    if col_venda not in res or col_compra not in res:
        return np.nan
        
    grega_venda = np.nan_to_num(res[col_venda])
    grega_compra = np.nan_to_num(res[col_compra])
    
    if is_credito:
        # Bear Call (Vende strike baixo) ou Bull Put (Vende strike alto)
        return grega_venda - grega_compra
    else:
        # Bull Call (Compra strike baixo) ou Bear Put (Compra strike alto)
        return grega_compra - grega_venda

# =========================================================================
# 3. LÓGICA DE CÁLCULO DE SPREAD
# =========================================================================

def calcular_spreads(df_opcoes: pd.DataFrame, metrics: dict, tipo_operacao_filtro: str):
    """
    Identifica e calcula os Spreads, filtrando pelo tipo_operacao_filtro.
    """
    
    # Mapeamento do filtro de entrada para os tipos de spread:
    filtro_tipo_opcao = []
    
    if tipo_operacao_filtro in ['1', '3']:
        filtro_tipo_opcao.append('CALL')
    elif tipo_operacao_filtro in ['2', '4']:
        filtro_tipo_opcao.append('PUT')
    elif tipo_operacao_filtro == '0':
        filtro_tipo_opcao = ['CALL', 'PUT']
    else:
        return pd.DataFrame() 

    resultados_spread = []
    
    colunas_agrupamento = ['idAcao', 'tipo']
    if 'vencimento' in df_opcoes.columns:
        colunas_agrupamento.append('vencimento')
        
    print(f"\nAgrupando por: {colunas_agrupamento}")

    grega_cols = ['delta', 'gamma', 'theta', 'vega', 'volImplicita']
    
    # NOVO: Verifica se a coluna 'premio' existe no DataFrame
    usa_premio_direto = 'premio' in df_opcoes.columns 

    for chave, grupo in df_opcoes.groupby(colunas_agrupamento):
        
        id_ativo = chave[0]
        tipo = chave[1]
        vencimento = chave[-1] if 'vencimento' in colunas_agrupamento else 'N/A'
        
        if tipo not in filtro_tipo_opcao:
            continue
            
        # ---------------------------------------------------------------------
        # SPREADS COM CALL (Filtro para 1 e 3)
        # ---------------------------------------------------------------------
        
        if tipo == 'CALL':
            grupo_calls = grupo.sort_values(by='strike', ascending=True)

            for i in range(len(grupo_calls)):
                for j in range(i + 1, len(grupo_calls)):
                    op_strike_baixo = grupo_calls.iloc[i] 
                    op_strike_alto = grupo_calls.iloc[j]
                    
                    K_baixo = op_strike_baixo['strike']
                    K_alto = op_strike_alto['strike']
                    
                    # === BLOCO CORRIGIDO PARA EVITAR KEYERROR: 'premio' ===
                    if usa_premio_direto:
                        # Usa a coluna 'premio' (R$ direto)
                        premio_baixo_R = np.nan_to_num(op_strike_baixo['premio']) 
                        premio_alto_R = np.nan_to_num(op_strike_alto['premio'])
                    else:
                        # Usa a coluna 'premioPct' (Cálculo original: Strike * Pct)
                        premio_baixo_R = np.nan_to_num(K_baixo * op_strike_baixo['premioPct'] / 100.0)
                        premio_alto_R = np.nan_to_num(K_alto * op_strike_alto['premioPct'] / 100.0)
                    # =======================================================
                    
                    ganho_max_strike = K_alto - K_baixo
                    
                    # === 1. Bull Call Spread (DÉBITO) - Compra K_baixo, Vende K_alto ===
                    if tipo_operacao_filtro in ['0', '3']:
                        compra = op_strike_baixo; venda = op_strike_alto
                        premio_liquido = premio_alto_R - premio_baixo_R # Débito (negativo)
                        risco_max = abs(premio_liquido) # Custo inicial
                        retorno_max_pct = (ganho_max_strike + premio_liquido) / max(0.01, risco_max)
                        
                        resultado_debito = {
                            'ativo': id_ativo, 'vencimento': vencimento, 
                            'diasUteis': compra['diasUteis'], # <--- CORREÇÃO APLICADA
                            'tipo_spread': 'Bull Call Spread (Débito)',
                            'strike_compra': K_baixo, 'ticker_compra': compra['ticker'],
                            'strike_venda': K_alto, 'ticker_venda': venda['ticker'],
                            'premio_compra_R$': premio_baixo_R, 'premio_venda_R$': premio_alto_R,
                            'premio_liquido_R$': premio_liquido, 'ganho_max_R$': ganho_max_strike,
                            'risco_max_R$': risco_max, 'retorno_max_pct': retorno_max_pct,
                        }
                        for col in grega_cols:
                            resultado_debito[f'{col}_compra'] = compra[col] if col in grupo.columns else np.nan
                            resultado_debito[f'{col}_venda'] = venda[col] if col in grupo.columns else np.nan
                        resultados_spread.append(resultado_debito)

                    
                    # === 2. Bear Call Spread (CRÉDITO) - Vende K_baixo, Compra K_alto ===
                    if tipo_operacao_filtro in ['0', '1']:
                        venda = op_strike_baixo; compra = op_strike_alto
                        premio_liquido = premio_baixo_R - premio_alto_R # Crédito (positivo)
                        risco_max = ganho_max_strike - premio_liquido
                        risco_ajustado = max(0.01, risco_max)
                        retorno_max_pct = premio_liquido / risco_ajustado

                        resultado_credito = {
                            'ativo': id_ativo, 'vencimento': vencimento, 
                            'diasUteis': venda['diasUteis'], # <--- CORREÇÃO APLICADA
                            'tipo_spread': 'Bear Call Spread (Crédito)',
                            'strike_compra': K_alto, 'ticker_compra': compra['ticker'],
                            'strike_venda': K_baixo, 'ticker_venda': venda['ticker'],
                            'premio_compra_R$': premio_alto_R, 'premio_venda_R$': premio_baixo_R,
                            'premio_liquido_R$': premio_liquido, 'ganho_max_R$': premio_liquido,
                            'risco_max_R$': risco_max, 'retorno_max_pct': retorno_max_pct,
                        }
                        for col in grega_cols:
                            resultado_credito[f'{col}_compra'] = compra[col] if col in grupo.columns else np.nan
                            resultado_credito[f'{col}_venda'] = venda[col] if col in grupo.columns else np.nan
                        resultados_spread.append(resultado_credito)

            
        # ---------------------------------------------------------------------
        # SPREADS COM PUT (Filtro para 2 e 4)
        # ---------------------------------------------------------------------
        
        elif tipo == 'PUT':
            grupo_puts = grupo.sort_values(by='strike', ascending=True)

            for i in range(len(grupo_puts)):
                for j in range(i + 1, len(grupo_puts)):
                    op_strike_baixo = grupo_puts.iloc[i] 
                    op_strike_alto = grupo_puts.iloc[j]
                    
                    K_baixo = op_strike_baixo['strike']
                    K_alto = op_strike_alto['strike']
                    
                    # === BLOCO CORRIGIDO PARA EVITAR KEYERROR: 'premio' ===
                    if usa_premio_direto:
                        # Usa a coluna 'premio' (R$ direto)
                        premio_baixo_R = np.nan_to_num(op_strike_baixo['premio']) 
                        premio_alto_R = np.nan_to_num(op_strike_alto['premio'])
                    else:
                        # Usa a coluna 'premioPct' (Cálculo original: Strike * Pct)
                        premio_baixo_R = np.nan_to_num(K_baixo * op_strike_baixo['premioPct'] / 100.0)
                        premio_alto_R = np.nan_to_num(K_alto * op_strike_alto['premioPct'] / 100.0)
                    # =======================================================

                    ganho_max_strike = K_alto - K_baixo
                    
                    # === 3. Bull Put Spread (CRÉDITO) - Vende K_alto, Compra K_baixo ===
                    if tipo_operacao_filtro in ['0', '4']:
                        venda = op_strike_alto; compra = op_strike_baixo
                        premio_liquido = premio_alto_R - premio_baixo_R # Crédito (positivo)
                        risco_max = ganho_max_strike - premio_liquido
                        risco_ajustado = max(0.01, risco_max)
                        retorno_max_pct = premio_liquido / risco_ajustado

                        resultado_credito = {
                            'ativo': id_ativo, 'vencimento': vencimento, 
                            'diasUteis': venda['diasUteis'], # <--- CORREÇÃO APLICADA
                            'tipo_spread': 'Bull Put Spread (Crédito)', 
                            'strike_compra': K_baixo, 'ticker_compra': compra['ticker'],
                            'strike_venda': K_alto, 'ticker_venda': venda['ticker'],
                            'premio_compra_R$': premio_baixo_R, 'premio_venda_R$': premio_alto_R,
                            'premio_liquido_R$': premio_liquido, 'ganho_max_R$': premio_liquido,
                            'risco_max_R$': risco_max, 'retorno_max_pct': retorno_max_pct,
                        }
                        for col in grega_cols:
                            resultado_credito[f'{col}_compra'] = compra[col] if col in grupo.columns else np.nan
                            resultado_credito[f'{col}_venda'] = venda[col] if col in grupo.columns else np.nan
                        resultados_spread.append(resultado_credito)

                    
                    # === 4. Bear Put Spread (DÉBITO) - Compra K_alto, Vende K_baixo ===
                    if tipo_operacao_filtro in ['0', '2']:
                        compra = op_strike_alto; venda = op_strike_baixo
                        premio_liquido = premio_baixo_R - premio_alto_R # Débito (negativo)
                        risco_max = abs(premio_liquido) # Custo inicial
                        retorno_max_pct = (ganho_max_strike + premio_liquido) / max(0.01, risco_max)

                        resultado_debito = {
                            'ativo': id_ativo, 'vencimento': vencimento, 
                            'diasUteis': compra['diasUteis'], # <--- CORREÇÃO APLICADA
                            'tipo_spread': 'Bear Put Spread (Débito)',
                            'strike_compra': K_alto, 'ticker_compra': compra['ticker'],
                            'strike_venda': K_baixo, 'ticker_venda': venda['ticker'],
                            'premio_compra_R$': premio_alto_R, 'premio_venda_R$': premio_baixo_R,
                            'premio_liquido_R$': premio_liquido, 'ganho_max_R$': ganho_max_strike,
                            'risco_max_R$': risco_max, 'retorno_max_pct': retorno_max_pct,
                        }
                        for col in grega_cols:
                            resultado_debito[f'{col}_compra'] = compra[col] if col in grupo.columns else np.nan
                            resultado_debito[f'{col}_venda'] = venda[col] if col in grupo.columns else np.nan
                        resultados_spread.append(resultado_debito)

    return pd.DataFrame(resultados_spread)

# =========================================================================
# 4. FUNÇÃO DE RELATÓRIO DETALHADO (Ajustada com Valor Nocional)
# =========================================================================

def imprimir_relatorio_detalhado(res: pd.Series, cotação_atual: float):
    """
    Imprime um relatório detalhado para o melhor spread encontrado, incluindo o Valor Nocional.
    """
    print("=" * 75)
    print(f"RELATÓRIO DE OTIMIZAÇÃO: {res['tipo_spread'].upper()}")
    print("=" * 75)
    
    # --- CÁLCULO DE VALORES TOTAIS E LÍQUIDOS ---
    
    premio_liquido_unit = res['premio_liquido_R$'] 
    premio_bruto_total = premio_liquido_unit * QUANTIDADE_CONTRATOS
    
    # Valores unitários limpos (para evitar NaN no cálculo do Ponto de Equilíbrio)
    strike_compra_limpo = np.nan_to_num(res['strike_compra'])
    strike_venda_limpo = np.nan_to_num(res['strike_venda'])
    premio_liquido_unit_limpo = np.nan_to_num(premio_liquido_unit)
    
    is_credito = premio_liquido_unit >= 0
    
    if is_credito: # Spread de CRÉDITO (Embolsando Prêmio, positivo ou zero)
        natureza = "CRÉDITO (Embolsando Prêmio)"
        lucro_maximo_total = premio_bruto_total - TAXAS_TOTAIS_OPERACAO
        
        risco_maximo_total_teorico = res['risco_max_R$'] * QUANTIDADE_CONTRATOS
        risco_maximo_total_liquido = risco_maximo_total_teorico + TAXAS_TOTAIS_OPERACAO
        
        # Ponto de Equilíbrio (Crédito): O preço do ativo tem que estar "fora" da perna vendida.
        if 'Put' in res['tipo_spread']: # Bull Put Spread (Vende PUT Strike Alto)
            # PE = Strike Venda - Prêmio Líquido
            ponto_equilibrio = strike_venda_limpo - premio_liquido_unit_limpo
        else: # Bear Call Spread (Vende CALL Strike Baixo)
            # PE = Strike Venda + Prêmio Líquido
            ponto_equilibrio = strike_venda_limpo + premio_liquido_unit_limpo
            
        fluxo_inicial = "CRÉDITO BRUTO TOTAL"
        lucro_label = "LUCRO MÁXIMO TOTAL (CRÉDITO LÍQUIDO APÓS TAXAS)"
        risco_label = "PREJUÍZO MÁXIMO TOTAL"

    else: # Spread de DÉBITO (Desembolsando Custo, negativo)
        natureza = "DÉBITO (Desembolsando Custo)"
        
        ganho_max_strike_total = res['ganho_max_R$'] * QUANTIDADE_CONTRATOS
        lucro_maximo_total = ganho_max_strike_total + premio_bruto_total - TAXAS_TOTAIS_OPERACAO
        
        risco_maximo_total_teorico = abs(premio_bruto_total)
        risco_maximo_total_liquido = risco_maximo_total_teorico + TAXAS_TOTAIS_OPERACAO
        
        # Ponto de Equilíbrio (Débito): O preço do ativo tem que estar "além" da perna comprada.
        if 'Call' in res['tipo_spread']: # Bull Call Spread (Compra CALL Strike Baixo)
            # PE = Strike Compra + Custo Líquido (Abs Prêmio Líquido)
            ponto_equilibrio = strike_compra_limpo + abs(premio_liquido_unit_limpo)
        else: # Bear Put Spread (Compra PUT Strike Alto)
            # PE = Strike Compra - Custo Líquido (Abs Prêmio Líquido)
            ponto_equilibrio = strike_compra_limpo - abs(premio_liquido_unit_limpo)

        fluxo_inicial = "CUSTO BRUTO TOTAL"
        lucro_label = "LUCRO MÁXIMO TOTAL"
        risco_label = "PREJUÍZO MÁXIMO TOTAL (DÉBITO LÍQUIDO APÓS TAXAS)"
        
    # --- CÁLCULO DO VALOR NOCIONAL (ADICIONADO AQUI) ---
    diferenca_strike = abs(res['strike_venda'] - res['strike_compra'])
    valor_nocional_total = diferenca_strike * QUANTIDADE_CONTRATOS 
    # -------------------------------------------------------------------
    
    # --- INFORMAÇÕES GERAIS ---
    print(f"Ativo Base: {res['ativo']} | Cotação Atual: {formatar_moeda_br(cotação_atual)}")
    
    # DIAS ÚTEIS VEM DIRETAMENTE DO SPREAD VENCEDOR (Corrigido na função calcular_spreads)
    dias_uteis_limpos = int(np.nan_to_num(res['diasUteis'])) 
    print(f"Vencimento: {res['vencimento']} ({dias_uteis_limpos} dias)")
    
    print(f"Lotes de {QUANTIDADE_CONTRATOS} Contratos | Taxas Totais: {formatar_moeda_br(TAXAS_TOTAIS_OPERACAO)}")
    print(f"Natureza: {natureza}")
    print("-" * 75)
    
    # --- PERNAS DA OPERAÇÃO (Usa Ticker Compra/Venda, que já estão corretos) ---
    print(f"VENDA: {res['ticker_venda']} (Strike: {formatar_moeda_br(res['strike_venda'])} | Prêmio Unitário: {formatar_moeda_br(res['premio_venda_R$'])})")
    print(f"COMPRA: {res['ticker_compra']} (Strike: {formatar_moeda_br(res['strike_compra'])} | Prêmio Unitário: {formatar_moeda_br(res['premio_compra_R$'])})")

    print("-" * 75)
    
    # --- FLUXO DE CAIXA E RETORNO ---
    print(f"PRÊMIO/CUSTO LÍQUIDO por unidade: {formatar_moeda_br(premio_liquido_unit)}")
    print(f"FLUXO DE CAIXA INICIAL ({fluxo_inicial}): {formatar_moeda_br(premio_bruto_total)}")
    print(f"TAXAS: {formatar_moeda_br(TAXAS_TOTAIS_OPERACAO)}")
    print("-" * 75)
    
    print(f"PONTO DE EQUILÍBRIO (Breakeven): {formatar_moeda_br(ponto_equilibrio)} (Por Unidade)")
    
    # --- APRESENTAÇÃO DO VALOR NOCIONAL (ADICIONADO AQUI) ---
    print("-" * 75)
    print(f"VALOR NOCIONAL TOTAL (Exposição Máx.): {formatar_moeda_br(valor_nocional_total)}")
    print("-" * 75)
    
    print(f"{lucro_label}: {formatar_moeda_br(lucro_maximo_total)}")
    print(f"{risco_label}: {formatar_moeda_br(abs(risco_maximo_total_liquido))}")

    # --- Retorno Máximo sobre Risco em FATOR ---
    retorno_fator_unitario = res['retorno_max_pct']
    
    # RELAÇÃO RISCO/RETORNO LÍQUIDA
    if abs(risco_maximo_total_liquido) > 0:
        risco_retorno_liquido = lucro_maximo_total / abs(risco_maximo_total_liquido)
    else:
        risco_retorno_liquido = inf
        
    print(f"RELAÇÃO RISCO/RETORNO (LÍQUIDA, após taxas): {risco_retorno_liquido:.2f}")
    print(f"RETORNO MÁXIMO SOBRE RISCO (TEÓRICO, unitário): {retorno_fator_unitario:.2f}") 
    print("-" * 75)
    
    # --- ANÁLISE DE GREGAS (Se existirem) ---
    
    if all(col in res for col in ['delta_venda', 'delta_compra']) and not isnan(res['delta_venda']):
        
        # Uso da função refatorada
        net_delta = calcular_net_gregas(res, 'delta', is_credito)
        net_gamma = calcular_net_gregas(res, 'gamma', is_credito)
        net_theta = calcular_net_gregas(res, 'theta', is_credito)
        net_vega = calcular_net_gregas(res, 'vega', is_credito)

        print("--- ANÁLISE DE GREGAS (UNITÁRIA) ---")
        print(f"| Venda: Delta={res['delta_venda']:.2f}, Gamma={res['gamma_venda']:.2f}, Theta={res['theta_venda']:.2f}, Vega={res['vega_venda']:.2f}")
        print(f"| Compra: Delta={res['delta_compra']:.2f}, Gamma={res['gamma_compra']:.2f}, Theta={res['theta_compra']:.2f}, Vega={res['vega_compra']:.2f}")
        
        print("\n--- POSIÇÃO LÍQUIDA DO SPREAD ---")
        print(f"| NET DELTA: {net_delta:.2f}")
        print(f"| NET GAMMA: {net_gamma:.2f}")
        print(f"| NET THETA: {net_theta:.2f} (Ganho/Perda Diária Teórica)")
        print(f"| NET VEGA: {net_vega:.2f}")

        if 'volImplicita_venda' in res and not isnan(res['volImplicita_venda']):
            print("-" * 75)
            # Conversão para porcentagem * 100 para o formato mais comum (Ex: 30.50%)
            vi_venda = np.nan_to_num(res['volImplicita_venda'])
            vi_compra = np.nan_to_num(res['volImplicita_compra'])
            print(f"VI VENDIDA: {vi_venda * 100:.2f}% | VI COMPRADA: {vi_compra * 100:.2f}%")
        
    print("=" * 75)

# =========================================================================
# 5. FUNÇÃO PRINCIPAL
# =========================================================================

def main():
    """Função principal que carrega os dados e executa o cálculo do spread."""
    # Defina a data de análise aqui (usando a data do seu contexto/output anterior)
    DATA_ATUAL_STR = '14/10/2025' 
    DATA_ATUAL = datetime.strptime(DATA_ATUAL_STR, '%d/%m/%Y')
    
    print(f"Iniciando o Cálculo de Spreads (V44 - Filtro de Qualidade e Fluxo Aprimorado). Data da Análise: {DATA_ATUAL_STR}")

    # --- 1. Entrada do Ticker e Cotação Atual ---
    print("\n--- PARÂMETROS DE MERCADO ---")
    activo_base = input("Digite o Ticker do ativo (Ex: VALE3): ").strip().upper() or 'VALE3'
    
    cotação_actual = 0.0
    while True:
        try:
            preco_input = input(f"Digite o Valor Atual de {activo_base} (Ex: 60,00): ").strip().replace(',', '.')
            # Se o usuário apertar Enter, usamos um valor padrão (para fins de teste se necessário)
            if not preco_input: 
                cotação_actual = 60.00
            else:
                cotação_actual = float(preco_input)
                
            if cotação_actual <= 0:
                print("A cotação deve ser um valor positivo. Tente novamente.")
                continue
            break
        except ValueError:
            print("Entrada inválida. Por favor, digite um número (Ex: 60.00).")
    
    # --- 2. Entrada do Tipo de Operação ---
    print("\n--- SELEÇÃO DO TIPO DE SPREAD ---")
    tipo_input = None
    tipo_map = {'1': 'Bear Call (Crédito)', '2': 'Bear Put (Débito)', 
                '3': 'Bull Call (Débito)', '4': 'Bull Put (Crédito)', '0': 'TODOS'}
    while tipo_input not in tipo_map:
        print("Opções:")
        print(" 1: Bear Call (Crédito) - Baixa")
        print(" 2: Bear Put (Débito) - Baixa")
        print(" 3: Bull Call (Débito) - Alta")
        print(" 4: Bull Put (Crédito) - Alta")
        tipo_input = input("Qual Spread deseja otimizar? (1, 2, 3, 4 ou 0 para TODOS): ").strip() or '0'
        if tipo_input not in tipo_map:
            print("Opção inválida. Tente novamente.")
            
    tipo_operacao_filtro = tipo_input
    print(f"Filtro selecionado: {tipo_map[tipo_operacao_filtro]}")
    print("---------------------------------------------------------------------------")

    try:
        df_opcoes = pd.read_csv(NOME_ARQUIVO_CSV)
    except FileNotFoundError:
        print(f"ERRO: Arquivo '{NOME_ARQUIVO_CSV}' não encontrado.")
        print("Certifique-se de que o script 'limpar_excel_final.py' foi executado primeiro.")
        return
    except Exception as e:
        print(f"Ocorreu um erro ao carregar o CSV: {e}")
        return

    print(f"CSV carregado com {len(df_opcoes)} linhas.")

    # ----------------------------------------------------------------------
    # CORREÇÃO 1: RECALCULAR DIAS ÚTEIS (T-Dias)
    # CORREÇÃO DO ERRO 'TypeError' do np.busday_count
    # ----------------------------------------------------------------------
    
    # 1. Filtra o DataFrame apenas para o ativo que o usuário digitou
    df_opcoes_filtrado = df_opcoes[df_opcoes['idAcao'] == activo_base].copy()
    
    if df_opcoes_filtrado.empty:
        print(f"ERRO: Não foram encontradas opções para o ativo base '{activo_base}' no CSV.")
        return

    # 2. Converte a coluna 'vencimento' para o formato datetime
    df_opcoes_filtrado['vencimento'] = pd.to_datetime(df_opcoes_filtrado['vencimento'], errors='coerce')

    # 3. Prepara as datas no formato numpy.datetime64[D] (CORREÇÃO DE TIPO)
    
    # Data de Início (A data atual é uma única data para todas as linhas)
    data_inicio_np = np.array([DATA_ATUAL.strftime('%Y-%m-%d')], dtype='datetime64[D]')
    
    # Datas Finais (Os vencimentos da coluna)
    data_fim_np = df_opcoes_filtrado['vencimento'].values.astype('datetime64[D]')

    # 4. Recalcula os dias úteis entre a DATA_ATUAL e o VENCIMENTO
    df_opcoes_filtrado['diasUteis'] = np.busday_count(
        data_inicio_np, 
        data_fim_np
    )
    
    # 5. Garante que é um número inteiro
    df_opcoes_filtrado['diasUteis'] = df_opcoes_filtrado['diasUteis'].astype(int)

    # ----------------------------------------------------------------------
    # FIM DA CORREÇÃO 1
    # ----------------------------------------------------------------------

    primeiro_ticker_valido = df_opcoes_filtrado['ticker'].iloc[0] if not df_opcoes_filtrado.empty else None
    
    if primeiro_ticker_valido:
        metrics = buscar_metrics_do_csv(df_opcoes_filtrado, activo_base, cotação_actual)
        
        # O cálculo do spread passa a usar o DF filtrado e atualizado
        df_spreads = calcular_spreads(df_opcoes_filtrado, metrics, tipo_operacao_filtro)
    else:
        print(f"ERRO: Não há dados válidos de opções no CSV para {activo_base}.")
        return

    # =========================================================================
    # 6. FILTRAGEM, VIABILIDADE E ORDENAÇÃO (COM FILTRO DE QUALIDADE)
    # =========================================================================
    
    if df_spreads.empty:
        print("\nNenhum spread válido encontrado após o filtro por tipo.")
        return

    # 0. CÁLCULO PRÉ-FILTRO: Adiciona a coluna 'abs_retorno' ao DataFrame completo.
    df_spreads['abs_retorno'] = df_spreads['retorno_max_pct'].abs()
    
    # 1. CÁLCULO: Lucro Máximo Líquido Total
    # Spreads de CRÉDITO
    df_spreads.loc[df_spreads['premio_liquido_R$'] >= 0, 'lucro_max_liquido_total'] = \
        (df_spreads['premio_liquido_R$'] * QUANTIDADE_CONTRATOS) - TAXAS_TOTAIS_OPERACAO
        
    # Spreads de DÉBITO
    df_spreads.loc[df_spreads['premio_liquido_R$'] < 0, 'lucro_max_liquido_total'] = \
        (df_spreads['ganho_max_R$'] * QUANTIDADE_CONTRATOS) + \
        (df_spreads['premio_liquido_R$'] * QUANTIDADE_CONTRATOS) - TAXAS_TOTAIS_OPERACAO
        
    # 2. CÁLCULO: Prejuízo Máximo Líquido Total
    # Custo/Risco total = Risco Teórico + Taxas
    df_spreads['risco_max_liquido_total'] = \
        (df_spreads['risco_max_R$'] * QUANTIDADE_CONTRATOS) + TAXAS_TOTAIS_OPERACAO
    
    # 3. CÁLCULO: Relação Risco/Retorno Líquida
    df_spreads['relacao_risco_retorno_liquida'] = np.where(
        df_spreads['risco_max_liquido_total'] > 0,
        df_spreads['lucro_max_liquido_total'] / df_spreads['risco_max_liquido_total'],
        0.0 # Evita divisão por zero
    )
    
    # 4. FILTRAGEM DE VIABILIDADE:
    
    # a) Remove operações onde o lucro máximo é <= 0 (onde as taxas "comem" o lucro)
    df_viavel = df_spreads[df_spreads['lucro_max_liquido_total'] > 0].copy()
    
    # b) NOVO FILTRO DE QUALIDADE: A relação Risco/Retorno Líquida deve ser >= ao MÍNIMO
    df_viavel = df_viavel[df_viavel['relacao_risco_retorno_liquida'] >= MIN_RISCO_RETORNO_LIQUIDO].copy()
    
    
    if df_viavel.empty:
        print("\n===================================================================")
        print("AVISO: NENHUM SPREAD É VIAVEL OU ATENDE AO CRITÉRIO DE QUALIDADE.")
        print(f"O Lucro Máximo Líquido é menor que zero ou a Relação Risco/Retorno é inferior a {MIN_RISCO_RETORNO_LIQUIDO:.2f}.")
        print(f"Taxas Totais: {formatar_moeda_br(TAXAS_TOTAIS_OPERACAO)}")
        print("===================================================================")
        
        # Exibimos o melhor que passou na viabilidade básica (lucro > 0), mas não no Risco/Retorno
        melhor_nao_qualificado = df_spreads[df_spreads['lucro_max_liquido_total'] > 0]
        
        if not melhor_nao_qualificado.empty:
            melhor_nao_qualificado = melhor_nao_qualificado.sort_values(
                by='relacao_risco_retorno_liquida', ascending=False
            ).iloc[0]
            print("\nRELATÓRIO DO MELHOR SPREAD NÃO QUALIFICADO (Lucro > 0, mas Risco/Retorno < 1.0):")
            imprimir_relatorio_detalhado(melhor_nao_qualificado, cotação_actual)
        return

    # 5. ORDENAÇÃO: Ordena pelo melhor Retorno/Risco Líquido
    df_viavel = df_viavel.sort_values(by='relacao_risco_retorno_liquida', ascending=False)
    
    # 6. Seleciona o melhor (primeira linha após ordenação)
    melhor_spread = df_viavel.iloc[0]
    
    # 7. Imprime o relatório detalhado do melhor spread viável
    print("\n===================================================================")
    print(f"RELATÓRIO: MELHOR SPREAD (Risco/Retorno Líquido >= {MIN_RISCO_RETORNO_LIQUIDO:.2f})")
    print("===================================================================")
    imprimir_relatorio_detalhado(melhor_spread, cotação_actual)

if __name__ == '__main__':
    # Você pode alterar a data de análise padrão aqui se precisar testar outro dia
    main()