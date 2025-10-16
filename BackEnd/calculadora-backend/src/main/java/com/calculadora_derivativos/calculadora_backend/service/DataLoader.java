package com.calculadora_derivativos.calculadora_backend.service;

import com.calculadora_derivativos.calculadora_backend.model.Option;
import com.calculadora_derivativos.calculadora_backend.repository.OptionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

// CORREÇÃO: Nome explícito para o bean para evitar ConflictingBeanDefinitionException
@Component("dataLoaderService") 
public class DataLoader implements CommandLineRunner {

    private final OptionRepository optionRepository;
    
    // Nome exato do arquivo
    private static final String CSV_FILE = "opcoes_final_tratado.csv"; 

    public DataLoader(OptionRepository optionRepository) {
        this.optionRepository = optionRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (optionRepository.count() > 0) {
            System.out.println("Dados de opções já carregados. Pulando carregamento do CSV.");
            return;
        }

        System.out.println("Iniciando carregamento de dados...");
        
        try {
            // Carrega o arquivo do Classpath (src/main/resources)
            Resource resource = new ClassPathResource(CSV_FILE);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                
                String line = reader.readLine(); // Lê a linha do cabeçalho
                if (line == null) {
                    System.out.println("Arquivo CSV vazio.");
                    return;
                }

                // 1. Mapear Colunas do CSV para Índices
                String[] headers = line.split(",");
                Map<String, Integer> headerMap = new HashMap<>();
                
                for (int i = 0; i < headers.length; i++) { 
                    headerMap.put(headers[i].trim().toLowerCase(), i); 
                }
                
                System.out.println("Cabeçalho do CSV lido (Normalizado): " + headerMap.keySet());

                // 2. Mapeamento dos Campos NECESSÁRIOS, ordenados para CLAREZA no código
                Integer idAcaoIndex = headerMap.get("idacao"); 
                Integer tickerIndex = headerMap.get("ticker");
                Integer vencimentoIndex = headerMap.get("vencimento");
                
                // Estes campos existem no CSV mas podem não estar na sua entidade Option, 
                // e são mapeados apenas para verificação de erro ou uso futuro.
                Integer diasUteisIndex = headerMap.get("diasuteis"); 
                Integer dataHoraIndex = headerMap.get("datahora"); 
                
                Integer tipoIndex = headerMap.get("tipo");
                Integer strikeIndex = headerMap.get("strike");
                Integer precoIndex = headerMap.get("premiopct"); // Mapeado de 'premiopct'
                Integer volImplicitaIndex = headerMap.get("volimplicita"); 
                Integer deltaIndex = headerMap.get("delta");
                Integer gammaIndex = headerMap.get("gamma");
                Integer thetaIndex = headerMap.get("theta");
                Integer vegaIndex = headerMap.get("vega");

                // Verificação de Nulos
                if (tickerIndex == null || tipoIndex == null || strikeIndex == null || deltaIndex == null ||
                    vencimentoIndex == null || thetaIndex == null || gammaIndex == null || vegaIndex == null ||
                    idAcaoIndex == null || precoIndex == null || volImplicitaIndex == null) {
                    
                    System.err.println("Erro: Uma ou mais colunas necessárias não foram encontradas no CSV. Verifique a ortografia.");
                    if (precoIndex == null) System.err.println(" - O campo 'preco' não foi encontrado (Esperado: 'premiopct')");
                    return;
                }

                // 3. Processamento das Linhas do CSV
                String dataLine;
                while ((dataLine = reader.readLine()) != null) {
                    String[] values = dataLine.split(",");

                    try {
                        if (values.length < headers.length) { 
                             continue;
                        }

                        Option option = new Option();
                        
                        // Preenchendo na ordem de leitura (que agora está mais organizada)
                        option.setIdAcao(values[idAcaoIndex].trim());
                        option.setTicker(values[tickerIndex].trim());
                        option.setVencimento(LocalDate.parse(values[vencimentoIndex].trim())); 
                        
                        // NOTE: diasuteis e datahora são ignorados aqui, pois não fazem parte da entidade Option
                        
                        option.setTipo(values[tipoIndex].trim());
                        option.setStrike(new BigDecimal(values[strikeIndex].trim()));
                        option.setPreco(new BigDecimal(values[precoIndex].trim())); // premiopct
                        option.setVolImplicita(new BigDecimal(values[volImplicitaIndex].trim())); 
                        
                        option.setDelta(new BigDecimal(values[deltaIndex].trim()));
                        option.setGamma(new BigDecimal(values[gammaIndex].trim()));
                        option.setTheta(new BigDecimal(values[thetaIndex].trim()));
                        option.setVega(new BigDecimal(values[vegaIndex].trim()));

                        // Salva a entidade
                        optionRepository.save(option);
                        
                    } catch (Exception e) {
                        System.err.println("Linha ignorada por erro de parse de valor/data ou cabeçalho: " + dataLine + " | Erro: " + e.getMessage());
                    }
                }

                System.out.println("Carregamento de dados finalizado. Total de opções carregadas: " + optionRepository.count());
            }

        } catch (IOException e) {
            System.err.println("Erro ao ler o recurso CSV. Verifique se '" + CSV_FILE + "' está no diretório resources/ e se há permissão de acesso: " + e.getMessage());
        }
    }
}