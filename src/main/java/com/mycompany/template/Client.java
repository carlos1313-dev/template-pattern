/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.template;

import com.mycompany.template.core.DataMiner;
import com.mycompany.template.factory.DataMinerFactory;

/**
 * Punto de entrada de la aplicación de minería de datos corporativos.
 *
 * Uso:
 *   java dataminer.Client archivo1 archivo2 ...
 *
 * Si no se pasan argumentos, se procesan los archivos de muestra
 * ubicados en ./samples/.
 */
public class Client {
 
    public static void main(String[] args) {
 
        String[] archivos = (args.length > 0) ? args : new String[]{
            "samples/informe_anual.doc",
            "samples/ventas_q3.csv",
            "samples/contrato_proveedor.pdf"
        };
 
        System.out.println("-------------------------------------------");
        System.out.println("       Aplicación de Minería de Datos v2.0   ");
        System.out.println("        Patrón de diseño: Template Method    ");
        System.out.println("-------------------------------------------");
 
        for (String archivo : archivos) {
            DataMiner miner = DataMinerFactory.create(archivo);
            if (miner != null) {
                System.out.println("\n-------------------------------------------");
                System.out.println(" Procesando: " + archivo);
                System.out.println("---------------------------------------------");
                miner.mine(archivo);     // ← Template Method en acción
            }
        }
 
        System.out.println("\n Procesamiento finalizado.");
    }
}