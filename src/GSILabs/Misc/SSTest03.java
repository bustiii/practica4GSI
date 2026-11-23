package GSILabs.Misc;

import java.io.File;
import java.io.IOException;
import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;

/**
 * Ejercicio 4: Clase ejecutable para la lectura de datos numericos desde un
 * archivo ODS existente.
 * <p>
 * Lee el archivo test02.ods generado en el ejercicio anterior y recupera los
 * datos de la matriz iterando sobre su posicion explicita (hard-coded). Ignora
 * el formato de color de las celdas.
 * </p>
 */
public class SSTest03 {

    /**
     * Metodo principal que lee el archivo test02.ods y muestra sus valores
     * numericos a traves de la salida estandar (consola).
     *
     * @param args argumentos pasados por linea de comandos (no se utilizan)
     */
    public static void main(String[] args) {
        File inputFile = new File("test02.ods");

        if (!inputFile.exists()) {
            System.out.println("El archivo test02.ods no existe. Ejecuta primero SSTest02.");
            return;
        }

        try {
            // Cargar el documento de hoja de cálculo desde el archivo
            SpreadSheet spreadSheet = SpreadSheet.createFromFile(inputFile);

            // Obtener la primera hoja (índice 0)
            Sheet sheet = spreadSheet.getSheet(0);

            // Posición hard-coded según el Ejercicio 3 (5 filas y 3 columnas vacías)
            final int startCol = 3; // Columna D (Índice 3)
            final int startRow = 5; // Fila 6 (Índice 5)
            final int numFilas = 4;
            final int numColumnas = 6;

            System.out.println("Datos numéricos recuperados del archivo test02.ods:");

            // Leer los datos de la hoja y mostrarlos por consola
            for (int fila = 0; fila < numFilas; fila++) {
                for (int col = 0; col < numColumnas; col++) {
                    // El método getCellAt recibe (columna, fila)
                    Object valor = sheet.getCellAt(startCol + col, startRow + fila).getValue();
                    System.out.print(valor + "\t");
                }
                System.out.println(); // Salto de línea para la siguiente fila de la matriz
            }

        } catch (final IOException e) {
            System.err.println("Error al leer el archivo ODS: " + e.getMessage());
        }
    }
}
