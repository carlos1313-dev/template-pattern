/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.template.miners;

import com.mycompany.template.core.DataMiner;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Minador de archivos DOC (texto plano / Word simple).
 *
 * extractData : lee el archivo byte a byte con FileChannel + ByteBuffer (NIO),
 *               decodifica a UTF-8 y filtra caracteres no imprimibles que
 *               pueden aparecer en binarios DOC legacy.
 * parseData   : identifica pares "Clave: Valor" en el texto y los agrupa
 *               en un mapa estructurado.
 */
public class DocDataMiner extends DataMiner {
 
    // ─── Paso variable 1: extracción ────────────────────────────────────────────
    @Override
    protected String extractData(String path) throws IOException {
        System.out.println("[DocDataMiner] extractData() → leyendo con FileChannel + ByteBuffer...");
 
        try (FileChannel channel = FileChannel.open(Paths.get(path), StandardOpenOption.READ)) {
            long fileSize = channel.size();
            // Lectura completa en un único ByteBuffer (eficiente para archivos < 10 MB)
            ByteBuffer buffer = ByteBuffer.allocate((int) fileSize);
            int bytesRead = channel.read(buffer);
            buffer.flip();
 
            // Decodificar como UTF-8 y limpiar caracteres de control (excepto \n \r \t)
            String raw = StandardCharsets.UTF_8.decode(buffer).toString();
            String cleaned = raw.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "")
                                .trim();
 
            System.out.printf("[DocDataMiner] Leídos %d bytes → %d caracteres útiles%n",
                    bytesRead, cleaned.length());
            return cleaned;
        }
    }
 
    // ─── Paso variable 2: parseo ─────────────────────────────────────────────────
    @Override
    protected Object parseData(String rawData) {
        System.out.println("[DocDataMiner] parseData()   → extrayendo campos clave-valor...");
 
        Map<String, String> fields = new LinkedHashMap<>();
        // Detectar líneas con patrón  "Clave: Valor"
        Pattern pattern = Pattern.compile("^([A-Za-záéíóúÁÉÍÓÚñÑ\\s]+):\\s*(.+)$",
                Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(rawData);
 
        while (matcher.find()) {
            fields.put(matcher.group(1).trim(), matcher.group(2).trim());
        }
 
        if (fields.isEmpty()) {
            fields.put("contenido_raw", rawData.length() > 200
                    ? rawData.substring(0, 200) + "…" : rawData);
        }
 
        // Construir representación legible del mapa
        StringBuilder sb = new StringBuilder("DocData{\n");
        fields.forEach((k, v) -> sb.append("  ").append(k).append(": ").append(v).append("\n"));
        sb.append("}");
 
        System.out.println("[DocDataMiner] Campos encontrados: " + fields.size());
        return sb.toString();
    }
}