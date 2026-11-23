package GSILabs.Misc;

import GSILabs.BModel.Bar;
import GSILabs.BModel.Dueno;
import GSILabs.BSystem.BusinessSystem;
import java.io.File;
import java.time.LocalDate;

/**
 * Ejercicio 6: prueba del metodo {@link BusinessSystem#importaBares(File)}. Usa
 * por defecto P02Ej05.ods (en la raiz del proyecto); si no existe, usa
 * locales.ods, generado por {@link SSTest04}. Tambien se puede indicar otro
 * fichero como primer argumento.
 *
 * @author Oihan
 */
public class SSTest05 {

    /**
     * Importa los bares de un fichero ODS y comprueba varios casos.
     *
     * @param args opcionalmente, la ruta del fichero ODS a importar
     */
    public static void main(String[] args) {
        File fichero = new File(args.length > 0 ? args[0] : "P02Ej05.ods");
        if (!fichero.isFile()) {
            fichero = new File(SSTest04.FICHERO);
        }
        System.out.println("Fichero de prueba: " + fichero.getPath());

        BusinessSystem sistema = new BusinessSystem();
        // Propietarios que aparecen en el fichero: si no estan dados de alta, el bar se importa sin dueno
        String[] nicks = {"Carlos Perez", "Carlos Gutierrez", "Juan Martin", "Marcos Perez",
            "Pedro Garcia", "Josefina Garcia", "Carlos Rodriguez", "Ana Gil"};
        for (final String nick : nicks) {
            sistema.nuevoUsuario(new Dueno(nick, "clave", LocalDate.of(1980, 1, 1)));
        }

        int importados = sistema.importaBares(fichero);
        System.out.println("Bares importados: " + importados);

        System.out.println("Reimportar el mismo fichero (direcciones ya ocupadas): "
                + sistema.importaBares(fichero) + " (esperado: 0)");
        System.out.println("Fichero inexistente: " + sistema.importaBares(new File("noExiste.ods")) + " (esperado: 0)");
        System.out.println("Fichero nulo: " + sistema.importaBares(null) + " (esperado: 0)");

        Bar[] muestra = sistema.listarBares("Mataro", "Barcelona");
        for (final Bar bar : muestra) {
            System.out.println(bar);
        }
    }
}
