/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.template.core;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Clase abstracta que define el esqueleto del algoritmo de minería de datos.
 *
 * PATRÓN TEMPLATE METHOD:
 *   mine() es el template method — declara el flujo completo (final).
 *   extractData() y parseData() son los pasos variables — abstractos,
 *   implementados por cada subclase según su formato de archivo.
 *
 *   Los pasos openFile / analyzeData / sendReport / closeFile son concretos
 *   y residen aquí una sola vez, eliminando la duplicación.
 */
public abstract class DataMiner {

    // ─── Template Method ────────────────────────────────────────────────────────
    /**
     * Algoritmo completo de minería. Marcado final: ninguna subclase
     * puede alterar el orden de los pasos.
     */
    public final void mine(String path) {
        FileChannel channel = null;
        try {
            channel         = openFile(path);
            String rawData  = extractData(channel);
            Object data     = parseData(rawData);
            String analysis = analyzeData(data);
            sendReport(path, analysis);
        } catch (IOException e) {
            System.err.println("[ERROR] No se pudo procesar '"
                    + path + "': " + e.getMessage());
        } finally {
            closeFile(channel, path);
        }
    }

    // ─── Pasos concretos ────────────────────────────────────────────────────────

    /**
     * Abre el archivo usando un FileChannel (Java NIO) y valida que exista
     * y sea legible antes de continuar con el flujo.
     */
    private FileChannel openFile(String path) throws IOException {
        Path filePath = Paths.get(path);
        if (!Files.exists(filePath))
            throw new IOException("El archivo no existe: " + path);
        if (!Files.isReadable(filePath))
            throw new IOException("Sin permisos de lectura: " + path);

        FileChannel channel = FileChannel.open(filePath, StandardOpenOption.READ);
        System.out.printf("[openFile] Abierto | %s | %.1f KB%n",
                filePath.getFileName(), (double) Files.size(filePath) / 1024);
        return channel;
    }

    /** Analiza los datos ya parseados para producir un resumen textual. */
    private String analyzeData(Object data) {
        String content  = data.toString();
        long words      = countWords(content);
        long lines      = content.lines().count();
        long chars      = content.length();

        String summary = String.format(
                "Líneas: %d | Palabras: %d | Caracteres: %d | Fragmento: \"%s\"",
                lines, words, chars,
                content.length() > 60 ? content.substring(0, 60).trim() + "…" : content.trim()
        );

        System.out.println("[analyzeData] " + summary);
        return summary;
    }

    /** Envía (imprime) el reporte de análisis con marca de tiempo. */
    private void sendReport(String path, String analysis) {
        String fileName = Paths.get(path).getFileName().toString();
        System.out.println("[sendReport]  -------------------------------");
        System.out.println("[sendReport]  Archivo  : " + fileName);
        System.out.println("[sendReport]  Resultado: " + analysis);
        System.out.println("[sendReport]  -------------------------------");
    }

    /** Cierra el FileChannel liberando el descriptor del sistema operativo. */
    private void closeFile(FileChannel channel, String path) {
        if (channel != null && channel.isOpen()) {
            try {
                channel.close();
                System.out.println("[closeFile]   Canal cerrado   | " + Paths.get(path).getFileName());
            } catch (IOException e) {
                System.err.println("[closeFile]   Error al cerrar : " + e.getMessage());
            }
        }
    }

    //  Utilidad privada
    private long countWords(String text) {
        if (text == null || text.isBlank()) return 0;
        return java.util.Arrays.stream(text.trim().split("\\s+"))
                .filter(w -> !w.isBlank()).count();
    }

    // Pasos abstractos (implementados por cada subclase)

    /**
     * Lee y extrae el contenido crudo del archivo según su formato.
     * @param path ruta absoluta o relativa al archivo
     * @return texto plano con el contenido extraído
     * @throws IOException si ocurre un error de lectura
     */
    protected abstract String extractData(FileChannel channel) throws IOException;

    /**
     * Transforma el contenido crudo en una representación estructurada.
     * @param rawData texto plano devuelto por extractData()
     * @return objeto con los datos ya estructurados/parseados
     */
    protected abstract Object parseData(String rawData);
}