/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.template.factory;

import com.mycompany.template.core.DataMiner;
import com.mycompany.template.miners.CSVDataMiner;
import com.mycompany.template.miners.DocDataMiner;
import com.mycompany.template.miners.PDFDataMiner;


 
/**
 * Fábrica de minadores.
 * Determina la subclase concreta de DataMiner según la extensión del archivo.
 * Desacopla la lógica de selección de la clase Cliente.
 */
public class DataMinerFactory {
 
    private DataMinerFactory() {}   // no instanciable
 
    /**
     * Crea y devuelve el minador adecuado para el archivo indicado.
     *
     * @param path ruta del archivo a procesar
     * @return instancia concreta de DataMiner, o null si el formato no está soportado
     */
    public static DataMiner create(String path) {
        String ext = extractExtension(path);
        return switch (ext) {
            case "doc", "docx" -> new DocDataMiner();
            case "csv"         -> new CSVDataMiner();
            case "pdf"         -> new PDFDataMiner();
            default -> {
                System.err.println("[Factory] Formato no soportado: '." + ext
                        + "' para el archivo: " + path);
                yield null;
            }
        };
    }
 
    private static String extractExtension(String path) {
        int dot = path.lastIndexOf('.');
        return (dot >= 0 && dot < path.length() - 1)
                ? path.substring(dot + 1).toLowerCase()
                : "";
    }
}