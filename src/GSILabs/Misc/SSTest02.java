package GSILabs.Misc;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import org.jopendocument.dom.spreadsheet.MutableCell;
import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;

/**
 * Ejercicio 3: Clase ejecutable que posiciona una matriz con desplazamientos y
 * aplica formato condicional a las celdas.
 * <p>
 * Almacena un array de 4x6 enteros en el archivo test02.ods dejando las
 * primeras 5 filas y 3 columnas vacias. Aplica color azul si el valor es mayor
 * o igual a 10, y color rojo en caso contrario.
 * </p>
 */
public class SSTest02 {

    /**
     * Metodo principal que genera el archivo test02.ods con formato.
     *
     * @param args argumentos pasados por linea de comandos (no se utilizan)
     */
    public static void main(String[] args) {
        final int[][] matriz = {
            {7, 4, 7, 5, 19, 5},
            {4, 7, 6, 18, 1, 6},
            {8, 11, 8, 15, 8, 9},
            {9, 5, 7, 22, 3, 13}
        };

        try {
            File outputFile = new File("test02.ods");

            SpreadSheet spreadSheet = SpreadSheet.create(1, 10, 10);
            Sheet sheet = spreadSheet.getSheet(0);

            final int startCol = 3; // Columna D (Índice 3, saltando A, B, C)
            final int startRow = 5; // Fila 6 (Índice 5, saltando 1, 2, 3, 4, 5)

            for (int fila = 0; fila < 4; fila++) {
                for (int col = 0; col < 6; col++) {
                    int valor = matriz[fila][col];

                    MutableCell<SpreadSheet> cell = sheet.getCellAt(startCol + col, startRow + fila);

                    cell.setValue(valor);

                    // Establecer el color de fondo
                    if (valor >= 10) {
                        cell.setBackgroundColor(Color.CYAN); // Tono de azul claro (similar a la Figura 2)
                    } else {
                        cell.setBackgroundColor(Color.RED);  // Tono rojo
                    }
                }
            }

            // 5. Guardar el archivo
            spreadSheet.saveAs(outputFile);
            System.out.println("Archivo test02.ods generado correctamente.");

        } catch (final IOException e) {
            System.err.println("Error al escribir el archivo ODS: " + e.getMessage());
        }
    }
}
