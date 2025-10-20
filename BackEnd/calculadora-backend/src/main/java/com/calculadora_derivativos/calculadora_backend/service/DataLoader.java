package com.calculadora_derivativos.calculadora_backend.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime; // Importação adicionada
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.calculadora_derivativos.calculadora_backend.model.Option;
import com.calculadora_derivativos.calculadora_backend.repository.OptionRepository;

@Component("dataLoaderService") 
public class DataLoader implements CommandLineRunner {

    private final OptionRepository optionRepository;
    
    private static final String CSV_FILE = "opcoes_final_tratado.csv"; 
    
    // Formatadores para data/hora
    private static final DateTimeFormatter DATE_TIME_FORMATTER_FULL = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Formatadores para o Vencimento (mantidos do ajuste anterior)
    private static final DateTimeFormatter VENCIMENTO_FORMATTER_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter VENCIMENTO_FORMATTER_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");


    public DataLoader(OptionRepository optionRepository) {
        this.optionRepository = optionRepository;
    }

    // Método auxiliar para tentar analisar o vencimento em múltiplos formatos (mantido)
    private LocalDate parseVencimento(String vencimentoStr) {
        if (vencimentoStr.isEmpty()) {
            return null;
        }
        
        try {
            return LocalDate.parse(vencimentoStr, VENCIMENTO_FORMATTER_ISO);
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(vencimentoStr, VENCIMENTO_FORMATTER_BR);
            } catch (DateTimeParseException ex) {
                throw ex; // Falha em ambos
            }
        }
    }
    
    // NOVO MÉTODO AUXILIAR: Para analisar o campo dataHora que pode vir sem a hora.
    private LocalDateTime parseDataHora(String dataHoraStr) {
        if (dataHoraStr.isEmpty()) {
            return null;
        }
        
        // 1. Tenta analisar como LocalDateTime completo (com horas)
        try {
            return LocalDateTime.parse(dataHoraStr, DATE_TIME_FORMATTER_FULL);
        } catch (DateTimeParseException e) {
            // 2. Se falhar, tenta analisar como LocalDate (apenas data)
            try {
                LocalDate localDate = LocalDate.parse(dataHoraStr, DATE_FORMATTER_BR);
                // 3. Combina o LocalDate com a meia-noite para criar um LocalDateTime
                return LocalDateTime.of(localDate, LocalTime.MIDNIGHT);
            } catch (DateTimeParseException ex) {
                // Se ambos falharem, lança a exceção original (ou a última)
                throw ex;
            }
        }
    }


    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (optionRepository.count() > 0) {
            System.out.println("Dados de opções já carregados. Pulando carregamento do CSV.");
            return;
        }

        System.out.println("Iniciando carregamento de dados...");
        
        List<Option> opcoesParaSalvar = new ArrayList<>(); 
        
        try {
            Resource resource = new ClassPathResource(CSV_FILE);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                
                String line = reader.readLine(); 
                // ... (Mapeamento de cabeçalho e verificação de colunas permanecem o mesmo) ...

                String[] headers = line.split(",");
                Map<String, Integer> headerMap = new HashMap<>();
                
                for (int i = 0; i < headers.length; i++) { 
                    headerMap.put(headers[i].trim().toLowerCase(), i); 
                }
                
                System.out.println("Cabeçalho do CSV lido (Normalizado): " + headerMap.keySet());

                Integer idAcaoIndex = headerMap.get("idacao"); 
                Integer tickerIndex = headerMap.get("ticker");
                Integer vencimentoIndex = headerMap.get("vencimento");
                Integer diasUteisIndex = headerMap.get("diasuteis"); 
                Integer dataHoraIndex = headerMap.get("datahora"); 
                Integer tipoIndex = headerMap.get("tipo");
                Integer strikeIndex = headerMap.get("strike");
                Integer precoIndex = headerMap.get("premiopct"); 
                Integer volImplicitaIndex = headerMap.get("volimplicita"); 
                Integer deltaIndex = headerMap.get("delta");
                Integer gammaIndex = headerMap.get("gamma");
                Integer thetaIndex = headerMap.get("theta");
                Integer vegaIndex = headerMap.get("vega");

                if (tickerIndex == null || tipoIndex == null || strikeIndex == null || deltaIndex == null ||
                    vencimentoIndex == null || thetaIndex == null || gammaIndex == null || vegaIndex == null ||
                    idAcaoIndex == null || precoIndex == null || volImplicitaIndex == null || dataHoraIndex == null || diasUteisIndex == null) {
                    
                    System.err.println("Erro: Uma ou mais colunas necessárias não foram encontradas no CSV. Verifique a ortografia.");
                    if (precoIndex == null) System.err.println(" - O campo 'preco' não foi encontrado (Esperado: 'premiopct')");
                    if (dataHoraIndex == null) System.err.println(" - O campo 'datahora' não foi encontrado.");
                    if (diasUteisIndex == null) System.err.println(" - O campo 'diasuteis' não foi encontrado.");
                    return;
                }

                // 3. Processamento das Linhas do CSV
                String dataLine;
                long linhasIgnoradas = 0;
                long linhaAtual = 1; 
                
                while ((dataLine = reader.readLine()) != null) {
                    linhaAtual++;
                    String[] values = dataLine.split(",");

                    try {
                        if (values.length < headers.length) { 
                             linhasIgnoradas++;
                             System.err.println("Linha " + linhaAtual + " IGNORADA: Linha incompleta.");
                             continue;
                        }

                        // --- PARSING DE BIGDECIMAL e FILTRAGEM ---
                        BigDecimal strike = new BigDecimal(values[strikeIndex].trim());
                        BigDecimal preco = new BigDecimal(values[precoIndex].trim());
                        BigDecimal delta = new BigDecimal(values[deltaIndex].trim());
                        
                        if (strike.compareTo(BigDecimal.ZERO) == 0 || preco.compareTo(BigDecimal.ZERO) == 0 || delta.compareTo(BigDecimal.ZERO) == 0) {
                            linhasIgnoradas++;
                            continue;
                        }
                        // --- FIM DA FILTRAGEM ---
                        
                        // Mapeamento
                        Option option = new Option();
                        
                        option.setIdAcao(values[idAcaoIndex].trim());
                        option.setTicker(values[tickerIndex].trim());
                        
                        // Vencimento: USA O MÉTODO AUXILIAR FLEXÍVEL (mantido do ajuste anterior)
                        String vencimentoStr = values[vencimentoIndex].trim();
                        LocalDate vencimento = parseVencimento(vencimentoStr);
                        if (vencimento != null) {
                            option.setVencimento(vencimento); 
                        }

                        // diasUteis (Integer)
                        String diasUteisStr = values[diasUteisIndex].trim();
                        if (!diasUteisStr.isEmpty()) {
                            option.setDiasUteis(Integer.parseInt(diasUteisStr));
                        }

                        // dataHora: AGORA USA O NOVO MÉTODO AUXILIAR
                        String dataHoraStr = values[dataHoraIndex].trim();
                        option.setDataHora(parseDataHora(dataHoraStr));
                        
                        option.setTipo(values[tipoIndex].trim());
                        option.setStrike(strike); 
                        option.setPreco(preco); 
                        
                        // Outros BigDecimals
                        option.setVolImplicita(new BigDecimal(values[volImplicitaIndex].trim())); 
                        option.setDelta(delta);
                        option.setGamma(new BigDecimal(values[gammaIndex].trim()));
                        option.setTheta(new BigDecimal(values[thetaIndex].trim()));
                        option.setVega(new BigDecimal(values[vegaIndex].trim()));

                        opcoesParaSalvar.add(option);
                        
                    } catch (Exception e) {
                        linhasIgnoradas++;
                        System.err.println("Linha " + linhaAtual + " IGNORADA por erro de PARSE/FORMATO/Incompleta: " + dataLine + " | Erro: " + e.getMessage());
                    }
                }

                // 4. SALVAMENTO EM LOTE
                if (!opcoesParaSalvar.isEmpty()) {
                    System.out.println("\nIniciando persistência de " + opcoesParaSalvar.size() + " opções no MySQL...");
                    optionRepository.saveAll(opcoesParaSalvar); 
                    System.out.println("Persistência em lote concluída!");
                }
                
                // Log Final
                long totalOpcoesSalvas = optionRepository.count();
                System.out.println("\nCarregamento de dados finalizado. Total de opções carregadas: " + totalOpcoesSalvas);
                System.out.println("Total de linhas ignoradas: " + linhasIgnoradas);
            }

        } catch (IOException e) {
            System.err.println("Erro ao ler o recurso CSV. Verifique se '" + CSV_FILE + "' está no diretório resources/: " + e.getMessage());
        }
    }
}