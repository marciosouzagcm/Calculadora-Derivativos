package com.calculadora_derivativos.calculadora_backend.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader; 
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.calculadora_derivativos.calculadora_backend.model.Ativo;
import com.calculadora_derivativos.calculadora_backend.model.Opcao;
import com.calculadora_derivativos.calculadora_backend.repository.AtivoRepository;
import com.calculadora_derivativos.calculadora_backend.repository.OpcaoRepository;

@Component
public class DataLoader implements CommandLineRunner {

    private final OpcaoRepository opcaoRepository;
    private final AtivoRepository ativoRepository;
    private final ResourceLoader resourceLoader; 

    private static final String CSV_FILE = "opcoes_final_tratado.csv"; 

    // --- VARIÁVEIS DE TESTE ---
    private static final String ATIVO_CODIGO = "BOVA11";
    private static final BigDecimal ATIVO_PRECO = new BigDecimal("120.50"); 
    
    // FORMATADOR DE DATA: Ajuste se a data de vencimento no seu CSV não for 'AAAA-MM-DD'
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Injeção do repositório e do ResourceLoader
    public DataLoader(OpcaoRepository opcaoRepository, AtivoRepository ativoRepository, ResourceLoader resourceLoader) {
        this.opcaoRepository = opcaoRepository;
        this.ativoRepository = ativoRepository;
        this.resourceLoader = resourceLoader;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("--- DB: Verificando e Carregando dados iniciais ---");
        
        // 1. CARREGAR ATIVO SUBJACENTE
        this.carregarAtivoSubjacente(ATIVO_CODIGO, ATIVO_PRECO);
        
        // 2. CARREGAR OPÇÕES
        if (opcaoRepository.count() > 0) {
            System.out.println("--- DB: Tabela de opções já populada. Pulando carregamento do CSV.");
            return;
        }

        System.out.println("--- DB: Carregando dados iniciais do CSV: " + CSV_FILE + " ---");

        List<Opcao> opcoes = new ArrayList<>();
        Resource resource = resourceLoader.getResource("classpath:" + CSV_FILE); 

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            String line;
            reader.readLine(); // Pula o cabeçalho: idAcao,ticker,tipo,strike,premioPct,volImplicita,delta,gamma,theta,vega

            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");

                if (fields.length == 10) { 
                    try {
                        Opcao opcao = new Opcao();
                        
                        // Mapeamento dos campos (Índices do CSV: 0, 1, 2, 3, 4, ...)
                        // Coluna 1 (índice 1): Ticker -> Mapeia para Codigo na Entity
                        opcao.setCodigo(fields[1].trim()); 

                        // Coluna 2 (índice 2): Tipo -> Mapeia para TipoOpcao na Entity
                        opcao.setTipoOpcao(fields[2].trim()); 

                        // Coluna 3 (índice 3): Strike -> Mapeia para Strike na Entity (DEVE ser BigDecimal)
                        opcao.setStrike(new BigDecimal(fields[3].trim()));

                        // Coluna 4 (índice 4): PremioPct -> Mapeia para Preco na Entity (DEVE ser BigDecimal)
                        opcao.setPreco(new BigDecimal(fields[4].trim())); 

                        // Tentativa de inferir Vencimento a partir do Ticker. 
                        // **NOTA:** Se você tiver a data de vencimento em outra coluna do CSV, use-a!
                        // Caso contrário, tentaremos deduzir (Ex: "BOVAJ133" -> J = Vencimento em Outubro)
                        // Para simplificar, vamos usar uma data de vencimento fixa ou inferida se o CSV não tiver a coluna:
                        // *** Se o seu CSV tiver uma coluna de Vencimento, substitua a linha abaixo! ***
                        opcao.setVencimento(LocalDate.now().plusMonths(1)); // Exemplo: Vencimento em 1 mês.

                        opcoes.add(opcao);
                    } catch (NumberFormatException e) {
                        System.err.println("Aviso: Pulando linha com formato numérico inválido (Strike/Prêmio): " + line);
                    } catch (Exception e) {
                        System.err.println("Aviso: Erro desconhecido ao processar linha: " + line + ". Erro: " + e.getMessage());
                    }
                } else {
                    System.err.println("Aviso: Pulando linha com número incorreto de campos (" + fields.length + "): " + line);
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao processar o arquivo CSV: " + e.getMessage());
            e.printStackTrace();
            return; 
        }

        opcaoRepository.saveAll(opcoes);
        System.out.println("--- DB: Carregamento concluído. " + opcoes.size() + " opções salvas no banco de dados.");
    }
    
    // --- Carrega o Ativo Subjacente ---
    @Transactional
    private void carregarAtivoSubjacente(String codigo, BigDecimal preco) {
        Optional<Ativo> ativoOptional = ativoRepository.findByCodigo(codigo);
        
        if (ativoOptional.isEmpty()) {
            Ativo ativo = new Ativo();
            ativo.setCodigo(codigo);
            ativo.setPrecoAtual(preco); 
            
            ativoRepository.save(ativo);
            System.out.println("--- DB: Ativo Subjacente " + codigo + " (Preço: " + preco + ") salvo com sucesso. ---");
        } else {
            System.out.println("--- DB: Ativo Subjacente " + codigo + " já existe. Preço atual: " + ativoOptional.get().getPrecoAtual() + " ---");
        }
    }
}