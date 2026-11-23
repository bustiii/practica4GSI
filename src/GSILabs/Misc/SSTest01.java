package GSILabs.Misc;

import java.io.File;
import java.io.IOException;
import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;

/**
 * Ejercicio 2: Clase ejecutable para probar la generacion de archivos ODS
 * basicos.
 * <p>
 * Crea un array bidimensional de 4x6 numeros enteros y lo almacena en un
 * fichero de nombre test01.ods. La matriz ocupa las primeras 4 filas y 6
 * columnas de la primera hoja del documento.
 * </p>
 *
 */
public class SSTest01 {

    /**
     * Metodo principal que genera el archivo test01.ods.
     *
     * @param args argumentos pasados por linea de comandos (no se utilizan)
     */
    public static void main(String[] args) {

        final int[][] matriz = {
            {7, 4, 5, 9, 8, 1},
            {4, 7, 3, 4, 3, 8},
            {1, 9, 8, 8, 6, 7},
            {8, 5, 2, 6, 7, 5}
        };

        try {
            File outputFile = new File("test01.ods");

            SpreadSheet spreadSheet = SpreadSheet.create(1, 6, 4);

            Sheet sheet = spreadSheet.getSheet(0);

            for (int fila = 0; fila < 4; fila++) {
                for (int col = 0; col < 6; col++) {
                    sheet.setValueAt(matriz[fila][col], col, fila);
                }
            }

            spreadSheet.saveAs(outputFile);
            System.out.println("Archivo test01.ods generado correctamente en la raíz del proyecto.");

        } catch (final IOException e) {
            System.err.println("Error al escribir el archivo ODS: " + e.getMessage());
        }
    }
}
