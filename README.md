
📈 Calculadora de Derivativos - Estratégias com Opções (Call & Put)
Este projeto é uma Calculadora de Estratégias com Derivativos que combina o poder de Python para análise de dados e Java Spring Boot para servir a lógica de cálculo de Payoff via API. O objetivo é analisar uma tabela de opções disponíveis, identificar a combinação mais rentável, e calcular o risco/benefício (Lucro Máximo, Prejuízo Máximo e Breakeven Point) para estratégias de spread com opções Call e Put.

🌟 Funcionalidades Principais
Processamento de Dados (Python): Limpeza e tratamento automatizado de dados de opções (excluindo dados nulos, zeros ou faltantes) para gerar um CSV limpo.

Análise de Mercado (Python): Lógica para buscar e identificar combinações de opções (pernas) que formam uma estratégia de spread viável e otimizada.

API de Cálculo (Java Spring Boot): Um backend robusto que recebe a configuração da estratégia e calcula o Payoff (Lucro Máximo, Prejuízo Máximo e Ponto de Breakeven).

Cálculo Financeiro Preciso: Uso de BigDecimal no Java para garantir a precisão em cálculos de custo líquido e payoff.

🛠️ Tecnologias Utilizadas
Componente	Linguagem/Framework	Ferramentas Chave
Análise/Tratamento de Dados	Python	Pandas (para manipulação de dados), Script de limpeza (limpar_excel_final.py).
Backend / API	Java 17+	Spring Boot (3.x), Spring Data JPA, Maven, MySQL (Banco de Dados).
Cálculo Financeiro	Java	BigDecimal e lógica de simulação de Payoff no SpreadService.

Exportar para Sheets
📁 Estrutura do Projeto
O projeto está dividido em duas partes principais: o módulo de processamento em Python e o backend em Java.

1. Módulo Python (Script Python para Limpar e Gerar CSV)
Contém a lógica de preparação dos dados:

Arquivo	Descrição
limpar_excel_final.py	Executa o tratamento inicial do arquivo Excel de opções, removendo inconsistências e preparando os dados para o banco. Gera o arquivo .CSV que é consumido pelo Java.
spread_calculator_final.py	Contém o núcleo da lógica analítica de busca por oportunidades e a combinação mais rentável de opções (embora o cálculo final seja feito na API, este script pode guiar a escolha das pernas).

Exportar para Sheets
2. Módulo Java (calculadora-backend)
A API REST que realiza a busca e o cálculo em tempo real:

Componente	Descrição
DataLoader.java	Responsável por ler o CSV gerado pelo Python e popular o banco de dados MySQL com as opções e o preço do ativo subjacente.
Opcao.java / Ativo.java	Entidades JPA que mapeiam os dados das opções e do ativo no banco.
SpreadService.java	O coração da aplicação. Contém a lógica de cálculo de custo líquido, simulação de payoff e determinação dos pontos de Lucro Máximo, Prejuízo Máximo e Breakeven.
.../dto/PernaSpread.java	DTO para receber as pernas da estratégia (ticker, quantidade, operação: "COMPRA"/"VENDA").

Exportar para Sheets
🚀 Como Rodar o Projeto
Siga os passos para configurar e executar a aplicação:

Pré-requisitos
Java JDK 17+

Python 3.x

Maven

MySQL (ou outro banco de dados configurado no application.properties)

1. Configuração do Banco de Dados
Crie um banco de dados MySQL (derivativos_db, por exemplo).

Configure as credenciais e a URL de conexão no arquivo src/main/resources/application.properties do projeto Java.

2. Preparação dos Dados (Módulo Python)
Certifique-se de que seu arquivo Excel de dados de opções está no local esperado pelo limpar_excel_final.py.

Execute o script Python de limpeza para gerar o arquivo CSV de entrada:

Bash

python limpar_excel_final.py
(Este CSV deve ser colocado na pasta de recursos do projeto Java: src/main/resources).

3. Execução do Backend (Java Spring Boot)
Navegue até o diretório calculadora-backend.

Compile e execute o projeto usando Maven:

Bash

mvn clean install -U
mvn spring-boot:run

O servidor será iniciado na porta padrão 8080. O DataLoader irá automaticamente carregar os dados do CSV para o MySQL.

📌 Endpoint da API
O cálculo de spread é realizado através de uma requisição POST:

URL: http://localhost:8080/api/spread/calcular

Método: POST

Corpo da Requisição (JSON):

JSON

{
  "ativoSubjacente": "BOVA11",
  "pernas": [
    {
      "ticker": "BOVAJ133", 
      "quantidade": 100,
      "operacao": "COMPRA" 
    },
    {
      "ticker": "BOVAV133", 
      "quantidade": 100,
      "operacao": "VENDA" 
    }
  ]
}
Exemplo de Resposta de Sucesso
JSON

{
  "statusMessage":"Sucesso! Spread de BOVA11 calculado. Custo Líquido: 332.00",
  "lucroMaximo": 8852.00,
  "prejuizoMaximo": 332.00,
  "breakevenPoint": 142.00
}


✒️ Autor
[Márcio Almeida de Souza / https://github.com/marciosouzagcm]

(Você pode adicionar links para o seu LinkedIn ou outras redes sociais no final.)
