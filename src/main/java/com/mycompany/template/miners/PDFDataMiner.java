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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
 
/**
 * Minador de archivos PDF.
 *
 * extractData : abre el archivo con FileChannel (NIO), lee el contenido
 *               binario completo en un ByteBuffer y lo decodifica en Latin-1
 *               (preserva todos los bytes del PDF sin pérdida).
 * parseData   : aplica expresiones regulares sobre la estructura interna del
 *               PDF para extraer:
 *                 - texto de streams BT...ET (operadores Tj / TJ / ')
 *                 - metadatos del diccionario /Info (Title, Author, etc.)
 *                 - número de páginas desde /Count
 */
public class PDFDataMiner extends DataMiner {
 
    // ─── Paso variable 1: extracción ────────────────────────────────────────────
    @Override
    protected String extractData(String path) throws IOException {
        System.out.println("[PDFDataMiner] extractData() → leyendo binario PDF con FileChannel...");
 
        try (FileChannel channel = FileChannel.open(Paths.get(path), StandardOpenOption.READ)) {
            long size = channel.size();
            if (size > 50_000_000L) {
                throw new IOException("Archivo PDF demasiado grande (>50 MB): " + size + " bytes");
            }
 
            ByteBuffer buffer = ByteBuffer.allocate((int) size);
            int bytesRead = channel.read(buffer);
            buffer.flip();
 
            // Latin-1 preserva cada byte 1:1, necesario para trabajar con PDF binario
            String raw = StandardCharsets.ISO_8859_1.decode(buffer).toString();
 
            // Validar firma PDF
            if (!raw.startsWith("%PDF-")) {
                throw new IOException("El archivo no tiene firma PDF válida (esperado '%PDF-')");
            }
 
            System.out.printf("[PDFDataMiner] Leídos %d bytes | Versión PDF: %s%n",
                    bytesRead, detectVersion(raw));
            return raw;
        }
    }
 
    // ─── Paso variable 2: parseo ─────────────────────────────────────────────────
    @Override
    protected Object parseData(String rawData) {
        System.out.println("[PDFDataMiner] parseData()   → extrayendo texto de streams BT/ET...");
 
        List<String> textLines = extractTextFromStreams(rawData);
        String metadata        = extractMetadata(rawData);
        int pageCount          = extractPageCount(rawData);
 
        StringBuilder sb = new StringBuilder("PDFData{\n");
        sb.append("  version : ").append(detectVersion(rawData)).append("\n");
        sb.append("  páginas : ").append(pageCount).append("\n");
        if (!metadata.isBlank()) {
            sb.append("  metadata: ").append(metadata).append("\n");
        }
        sb.append("  texto   : ").append(textLines.size()).append(" fragmento(s)\n");
        textLines.forEach(t -> sb.append("    » ").append(t).append("\n"));
        sb.append("}");
 
        System.out.println("[PDFDataMiner] Fragmentos de texto extraídos: " + textLines.size());
        return sb.toString();
    }
 
    // ─── Utilidades privadas de parseo PDF ──────────────────────────────────────
 
    /**
     * Extrae el texto de todos los streams BT...ET del PDF.
     * Soporta operadores: Tj  (string simple)
     *                     TJ  (array de strings/kerning)
     *                     '   (nueva línea + texto)
     */
    private List<String> extractTextFromStreams(String raw) {
        List<String> results = new ArrayList<>();
 
        // Localizar cada bloque stream...endstream
        Pattern streamPat = Pattern.compile("stream\r?\n(.*?)\r?\nendstream",
                Pattern.DOTALL);
        Matcher streamMatcher = streamPat.matcher(raw);
 
        while (streamMatcher.find()) {
            String stream = streamMatcher.group(1);
 
            // Extraer bloques de texto BT...ET
            Pattern btPat = Pattern.compile("BT(.*?)ET", Pattern.DOTALL);
            Matcher btMatcher = btPat.matcher(stream);
 
            while (btMatcher.find()) {
                String block = btMatcher.group(1);
                String text = extractStringsFromBlock(block);
                if (!text.isBlank()) {
                    results.add(text.trim());
                }
            }
        }
 
        return results;
    }
 
    /** Extrae cadenas de texto dentro de un bloque BT/ET usando Tj, TJ y '. */
    private String extractStringsFromBlock(String block) {
        StringBuilder text = new StringBuilder();
 
        // Operador Tj: (texto) Tj
        Pattern tjPat = Pattern.compile("\\(([^)]*)\\)\\s*Tj");
        Matcher tj = tjPat.matcher(block);
        while (tj.find()) {
            text.append(decodePdfString(tj.group(1))).append(" ");
        }
 
        // Operador ': (texto) '
        Pattern tickPat = Pattern.compile("\\(([^)]*)\\)\\s*'");
        Matcher tick = tickPat.matcher(block);
        while (tick.find()) {
            text.append(decodePdfString(tick.group(1))).append(" ");
        }
 
        // Operador TJ: [(texto)(texto) ...] TJ
        Pattern tjArrayPat = Pattern.compile("\\[([^]]+)]\\s*TJ");
        Matcher tjArray = tjArrayPat.matcher(block);
        while (tjArray.find()) {
            Pattern innerStr = Pattern.compile("\\(([^)]*)\\)");
            Matcher inner = innerStr.matcher(tjArray.group(1));
            while (inner.find()) {
                text.append(decodePdfString(inner.group(1)));
            }
            text.append(" ");
        }
 
        return text.toString().trim();
    }
 
    /** Decodifica secuencias de escape PDF: \n \r \t \( \) \\ y octal \ddd */
    private String decodePdfString(String s) {
        return s.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\(", "(")
                .replace("\\)", ")")
                .replace("\\\\", "\\")
                .replaceAll("\\\\(\\d{3})", ""); // eliminar octal (simplificado)
    }
 
    /** Extrae metadatos del diccionario /Info si existe. */
    private String extractMetadata(String raw) {
        StringBuilder meta = new StringBuilder();
        String[] fields = {"Title", "Author", "Subject", "Creator", "Producer"};
 
        for (String field : fields) {
            Pattern p = Pattern.compile("/" + field + "\\s*\\(([^)]*)\\)");
            Matcher m = p.matcher(raw);
            if (m.find()) {
                meta.append(field).append(": ").append(m.group(1)).append(" | ");
            }
        }
 
        return meta.length() > 0
                ? meta.substring(0, meta.length() - 3)   // quitar último " | "
                : "";
    }
 
    /** Extrae el número de páginas desde /Count en el diccionario /Pages. */
    private int extractPageCount(String raw) {
        Pattern p = Pattern.compile("/Count\\s+(\\d+)");
        Matcher m = p.matcher(raw);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }
 
    /** Detecta la versión del PDF desde la firma inicial. */
    private String detectVersion(String raw) {
        if (raw.length() < 8) return "desconocida";
        // %PDF-1.4  →  posición 0..7
        int end = raw.indexOf('\n');
        return (end > 0 && end < 12)
                ? raw.substring(0, end).trim()
                : raw.substring(0, Math.min(8, raw.length())).trim();
    }
}