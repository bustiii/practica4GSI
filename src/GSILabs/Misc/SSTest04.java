package GSILabs.Misc;

import GSILabs.BModel.Bar;
import GSILabs.BModel.Direccion;
import GSILabs.BModel.Dueno;
import GSILabs.BModel.Local;
import GSILabs.BModel.Pub;
import GSILabs.BModel.Restaurante;
import GSILabs.BSystem.BusinessSystem;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;

/**
 * Ejercicio 5: introduce en un {@link BusinessSystem} varios bares,
 * restaurantes y pubs con sus propietarios, los recupera del sistema y los
 * guarda en locales.ods usando tres hojas (Bar, Restaurante, Pub) y una fila
 * por local.
 *
 * <p>
 * Formato de cada fila (sin cabecera, como en la Figura 3 del enunciado):</p>
 * <ul>
 * <li>A: nombre del local.</li>
 * <li>B: calle y numero en una sola celda (por ejemplo "Calle Cervantes
 * 161").</li>
 * <li>C: localidad. D: provincia.</li>
 * <li>E: nicks de los propietarios separados por ";" (cardinalidad variable en
 * una celda).</li>
 * <li>F en adelante: datos propios. Bar: una especialidad por celda.
 * Restaurante: precio del menu, capacidad por mesa y capacidad total. Pub: hora
 * de apertura y de clausura.</li>
 * </ul>
 *
 * @author Oihan
 */
public class SSTest04 {

    /**
     * Nombre del fichero generado.
     */
    public static final String FICHERO = "locales.ods";
    /**
     * Columna (base 0) de los propietarios.
     */
    public static final int COL_PROPIETARIOS = 4;
    /**
     * Primera columna (base 0) con datos propios del tipo de local.
     */
    public static final int COL_DATOS = 5;

    private static final String[][] CIUDADES = {
        {"Mataro", "Barcelona"}, {"Tarrasa", "Barcelona"}, {"Algeciras", "Cadiz"},
        {"Tudela", "Navarra"}, {"Pamplona", "Navarra"}, {"Sevilla", "Sevilla"}
    };

    /**
     * Carga datos de prueba, los recupera del sistema y los guarda en el
     * fichero ODS.
     *
     * @param args no se utilizan
     */
    public static void main(String[] args) {
        BusinessSystem sistema = new BusinessSystem();
        cargarDatos(sistema);

        List<Bar> bares = new ArrayList<>();
        List<Restaurante> restaurantes = new ArrayList<>();
        List<Pub> pubs = new ArrayList<>();
        for (String[] ciudad : CIUDADES) {
            Collections.addAll(bares, sistema.listarBares(ciudad[0], ciudad[1]));
            Collections.addAll(restaurantes, sistema.listarRestaurantes(ciudad[0], ciudad[1]));
            Collections.addAll(pubs, sistema.listarPubs(ciudad[0], ciudad[1]));
        }

        try {
            guardar(sistema, bares, restaurantes, pubs, new File(FICHERO));
            System.out.println("Archivo " + FICHERO + " generado: " + bares.size() + " bares, "
                    + restaurantes.size() + " restaurantes y " + pubs.size() + " pubs.");
        } catch (final IOException e) {
            System.err.println("Error al escribir el archivo ODS: " + e.getMessage());
        }
    }

    /**
     * Escribe las tres hojas del fichero.
     *
     * @param sistema sistema del que se obtienen los propietarios
     * @param bares bares a guardar
     * @param restaurantes restaurantes a guardar
     * @param pubs pubs a guardar
     * @param destino fichero ODS de salida
     * @throws IOException si no se puede escribir el fichero
     */
    static void guardar(BusinessSystem sistema, List<Bar> bares, List<Restaurante> restaurantes,
            List<Pub> pubs, File destino) throws IOException {

        int maxEspecialidades = 0;
        for (final Bar b : bares) {
            maxEspecialidades = Math.max(maxEspecialidades, b.getEspecialidades().size());
        }

        final int columnas = COL_DATOS + Math.max(3, maxEspecialidades);
        final int filas = Math.max(1, Math.max(bares.size(), Math.max(restaurantes.size(), pubs.size())));

        SpreadSheet libro = SpreadSheet.create(3, columnas, filas);
        Sheet hojaBar = libro.getSheet(0);
        Sheet hojaRestaurante = libro.getSheet(1);
        Sheet hojaPub = libro.getSheet(2);
        hojaBar.setName("Bar");
        hojaRestaurante.setName("Restaurante");
        hojaPub.setName("Pub");

        for (int i = 0; i < bares.size(); i++) {
            Bar bar = bares.get(i);
            escribirComunes(sistema, hojaBar, bar, i);
            int col = COL_DATOS;
            for (String especialidad : bar.getEspecialidades()) {
                hojaBar.setValueAt(especialidad, col++, i);
            }
        }
        for (int i = 0; i < restaurantes.size(); i++) {
            Restaurante r = restaurantes.get(i);
            escribirComunes(sistema, hojaRestaurante, r, i);
            hojaRestaurante.setValueAt(r.getPrecioMenu(), COL_DATOS, i);
            hojaRestaurante.setValueAt(r.getCapacidadMesa(), COL_DATOS + 1, i);
            hojaRestaurante.setValueAt(r.getCapacidadTotal(), COL_DATOS + 2, i);
        }
        for (int i = 0; i < pubs.size(); i++) {
            Pub p = pubs.get(i);
            escribirComunes(sistema, hojaPub, p, i);
            hojaPub.setValueAt(p.getHoraApertura().toString(), COL_DATOS, i);
            hojaPub.setValueAt(p.getHoraClausura().toString(), COL_DATOS + 1, i);
        }

        libro.saveAs(destino);
    }

    /**
     * Escribe las columnas comunes a todos los tipos de local (A a E). La
     * direccion no cabe en una celda, asi que se reparte: calle y numero
     * juntos, y localidad y provincia en celdas separadas.
     */
    private static void escribirComunes(BusinessSystem sistema, Sheet hoja, Local local, int fila) {
        Direccion d = local.getUbicacion();
        hoja.setValueAt(local.getNombre(), 0, fila);
        hoja.setValueAt(d.getCalle() + " " + d.getNumero(), 1, fila);
        hoja.setValueAt(d.getLocalidad(), 2, fila);
        hoja.setValueAt(d.getProvincia(), 3, fila);
        hoja.setValueAt(propietarios(sistema, local), COL_PROPIETARIOS, fila);
    }

    private static String propietarios(BusinessSystem sistema, Local local) {
        Dueno[] duenos = sistema.obtenerPropietarios(local);
        StringBuilder sb = new StringBuilder();
        if (duenos != null) {
            for (Dueno d : duenos) {
                if (sb.length() > 0) {
                    sb.append(';');
                }
                sb.append(d.getNick());
            }
        }
        return sb.toString();
    }

    private static Dueno dueno(BusinessSystem sistema, String nick) {
        Dueno d = new Dueno(nick, "clave", LocalDate.of(1980, 1, 1));
        sistema.nuevoUsuario(d);
        return d;
    }

    private static void alta(BusinessSystem sistema, Local local, Dueno... propietarios) {
        sistema.nuevoLocal(local);
        for (Dueno p : propietarios) {
            sistema.asociarLocal(local, p);
        }
    }

    private static void cargarDatos(BusinessSystem sistema) {
        Dueno carlosPerez = dueno(sistema, "Carlos Perez");
        Dueno carlosGutierrez = dueno(sistema, "Carlos Gutierrez");
        Dueno juanMartin = dueno(sistema, "Juan Martin");
        Dueno anaGil = dueno(sistema, "Ana Gil");

        Bar palmeras = new Bar("Rincon las palmeras", new Direccion("Mataro", "Barcelona", "Travesia Cervantes", 174));
        palmeras.addEspecialidad("Calamares");
        alta(sistema, palmeras, carlosPerez);

        Bar andalucia = new Bar("Tasca andalucia", new Direccion("Tarrasa", "Barcelona", "Calle Cervantes", 161));
        andalucia.addEspecialidad("Quesadillas");
        andalucia.addEspecialidad("Tapas");
        alta(sistema, andalucia, carlosGutierrez);

        Bar santi = new Bar("Rincon santi", new Direccion("Algeciras", "Cadiz", "Plaza Libertad", 95));
        santi.addEspecialidad("Desayuno");
        santi.addEspecialidad("Cerveza");
        santi.addEspecialidad("Albondigas");
        alta(sistema, santi, juanMartin, carlosPerez);

        alta(sistema, new Restaurante(15.0, 4, 40, "Restaurante Central",
                new Direccion("Tudela", "Navarra", "Ribera", 5)), anaGil);
        alta(sistema, new Restaurante(22.5, 6, 60, "Casa Mari",
                new Direccion("Pamplona", "Navarra", "Mayor", 10)), anaGil, juanMartin);

        // Pub(nombre, direccion, horaClausura, horaApertura)
        alta(sistema, new Pub("Pub Irish", new Direccion("Pamplona", "Navarra", "San Nicolas", 20),
                LocalTime.of(3, 0), LocalTime.of(18, 0)), carlosGutierrez);
        alta(sistema, new Pub("Pub Betis", new Direccion("Sevilla", "Sevilla", "Calle Betis", 7),
                LocalTime.of(2, 0), LocalTime.of(20, 0)), carlosPerez);
    }
}
