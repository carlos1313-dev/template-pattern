/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.template.miners;

 
import com.mycompany.template.core.DataMiner;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
 
/**
 * Minador de archivos CSV.
 *
 * extractData : abre el archivo con Files.newBufferedReader (NIO, UTF-8),
 *               lee línea por línea y reconstruye el contenido completo.
 * parseData   : interpreta la primera fila como cabecera y las siguientes
 *               como registros, construyendo una lista de mapas columna→valor.
 */
public class CSVDataMiner extends DataMiner {
 
    private static final String DELIMITER = ",";
 
    // ─── Paso variable 1: extracción ────────────────────────────────────────────
    @Override
    protected String extractData(String path) throws IOException {
        System.out.println("[CSVDataMiner] extractData() → leyendo con BufferedReader (NIO)...");
 
        StringBuilder content = new StringBuilder();
        int lineCount = 0;
 
        // Files.newBufferedReader usa NIO internamente y maneja el charset correctamente
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
                lineCount++;
            }
        }
 
        System.out.printf("[CSVDataMiner] Leídas %d líneas → %d caracteres%n",
                lineCount, content.length());
        return content.toString().trim();
    }
 
    // ─── Paso variable 2: parseo ─────────────────────────────────────────────────
    @Override
    protected Object parseData(String rawData) {
        System.out.println("[CSVDataMiner] parseData()   → estructurando filas y columnas...");
 
        String[] lines = rawData.split("\n");
        if (lines.length == 0) return "CSVData{vacío}";
 
        // Primera fila → cabeceras
        String[] headers = splitCSVLine(lines[0]);
 
        List<Map<String, String>> records = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank()) continue;
            String[] values = splitCSVLine(lines[i]);
            Map<String, String> record = new LinkedHashMap<>();
            for (int j = 0; j < headers.length; j++) {
                record.put(headers[j].trim(),
                        j < values.length ? values[j].trim() : "");
            }
            records.add(record);
        }
 
        // Representación textual estructurada
        StringBuilder sb = new StringBuilder();
        sb.append("CSVData{\n");
        sb.append("  columnas : ").append(headers.length).append(" → ")
          .append(String.join(", ", headers)).append("\n");
        sb.append("  registros: ").append(records.size()).append("\n");
        records.forEach(r -> sb.append("  ").append(r).append("\n"));
        sb.append("}");
 
        System.out.println("[CSVDataMiner] Registros parseados: " + records.size());
        return sb.toString();
    }
 
    /**
     * Divide una línea CSV respetando valores entre comillas dobles.
     */
    private String[] splitCSVLine(String line) {
        List<String> tokens = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder token = new StringBuilder();
 
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(token.toString());
                token.setLength(0);
            } else {
                token.append(c);
            }
        }
        tokens.add(token.toString());
        return tokens.toArray(new String[0]);
    }
}
 