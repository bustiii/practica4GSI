package GSILabs.connect;

import GSILabs.BModel.Bar;
import GSILabs.BModel.Cliente;
import GSILabs.BModel.Contestacion;
import GSILabs.BModel.Direccion;
import GSILabs.BModel.Dueno;
import GSILabs.BModel.Local;
import GSILabs.BModel.Pub;
import GSILabs.BModel.Restaurante;
import GSILabs.BModel.Review;
import GSILabs.BSystem.BusinessSystem;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Datos con los que {@link BusinessServer} puebla el sistema cuando no se le
 * indica un fichero XML. Son suficientes para probar todos los metodos de las
 * interfaces remotas y sus resultados son conocidos de antemano, de modo que
 * los clientes de prueba ({@code ClientHub} y {@code AdminHub}) pueden usarlos.
 * <p>
 * Usuarios (los identificadores son fijos):
 * <ul>
 * <li>Clientes: ana (1), iker (2) y eva (3).</li>
 * <li>Duenos: marta (4), duena de Bar Txoko, Asador Ribera y La Cocina de
 * Marta; y jon (5), dueno de Txoko Berri y Pub Luna.</li>
 * </ul>
 * Locales (puntuacion media entre parentesis):
 * <ul>
 * <li>Pamplona, bares: Bar Txoko (4), Txoko Berri (4,33) y Bar El Rincon
 * (2).</li>
 * <li>Pamplona, restaurantes: La Cocina de Marta (4,5), Asador Ribera (3,5),
 * Casa Pepe (3) y Restaurante Ciudadela (sin reviews).</li>
 * <li>Pamplona, pub: Pub Luna (3).</li>
 * <li>Tudela: bar La Mejana (4) y restaurante Meson del Ebro (5).</li>
 * </ul>
 * Por tanto, {@code mejorBar("Pamplona")} es Txoko Berri y
 * {@code mejoresRestaurantes("Pamplona", 5)} devuelve La Cocina de Marta,
 * Asador Ribera, Casa Pepe, Restaurante Ciudadela y null.
 * <p>
 * Hay dos reviews contestadas (ana en Bar Txoko e iker en Asador Ribera), que
 * un cliente no puede quitar, y una reserva de ana en Asador Ribera. Ana no
 * tiene ninguna review de Casa Pepe ni de Bar El Rincon, asi que puede
 * insertarlas.
 */
public final class DatosIniciales {

    /**
     * Identificador de la clienta ana.
     */
    public static final int ID_ANA = 1;
    /**
     * Identificador del cliente iker.
     */
    public static final int ID_IKER = 2;
    /**
     * Identificador de la clienta eva.
     */
    public static final int ID_EVA = 3;
    /**
     * Identificador de la duena marta.
     */
    public static final int ID_MARTA = 4;
    /**
     * Identificador del dueno jon.
     */
    public static final int ID_JON = 5;
    /**
     * Ciudad con mas locales, la que usan las consultas de ejemplo.
     */
    public static final String CIUDAD = "Pamplona";

    private DatosIniciales() {
    }

    /**
     * Puebla un sistema vacio con los datos descritos en la clase.
     *
     * @param s sistema vacio
     * @throws IllegalStateException si algun dato no se puede dar de alta (por
     * ejemplo, porque el sistema no estaba vacio)
     */
    public static void poblar(BusinessSystem s) {
        Cliente ana = new Cliente(ID_ANA, "ana", "ana2026", LocalDate.of(2001, 3, 15));
        Cliente iker = new Cliente(ID_IKER, "iker", "iker2026", LocalDate.of(1998, 7, 2));
        Cliente eva = new Cliente(ID_EVA, "eva", "eva2026", LocalDate.of(1990, 11, 30));
        Dueno marta = new Dueno(ID_MARTA, "marta", "marta2026", LocalDate.of(1975, 11, 20));
        Dueno jon = new Dueno(ID_JON, "jon", "jon2026", LocalDate.of(1982, 4, 9));
        for (Cliente c : new Cliente[]{ana, iker, eva}) {
            comprobar(s.nuevoUsuario(c), "usuario " + c.getNick());
        }
        comprobar(s.nuevoUsuario(marta), "usuario marta");
        comprobar(s.nuevoUsuario(jon), "usuario jon");

        Bar txoko = new Bar("Bar Txoko", pamplona("San Nicolas", 4));
        txoko.setDescripcion("Pintxos en el casco viejo");
        txoko.addEspecialidad("pintxos");
        Bar berri = new Bar("Txoko Berri", pamplona("Estafeta", 20));
        berri.addEspecialidad("vermut");
        Bar rincon = new Bar("Bar El Rincon", pamplona("Mayor", 31));
        Restaurante cocina = new Restaurante(32.0, 4, 40, "La Cocina de Marta", pamplona("Chapitela", 5));
        Restaurante asador = new Restaurante(25.0, 6, 80, "Asador Ribera", pamplona("Navarreria", 10));
        Restaurante pepe = new Restaurante(15.5, 4, 30, "Casa Pepe", pamplona("Zapateria", 7));
        Restaurante ciudadela = new Restaurante(20.0, 8, 60, "Restaurante Ciudadela", pamplona("Taconera", 2));
        Pub luna = new Pub("Pub Luna", pamplona("Jarauta", 30), LocalTime.of(4, 0), LocalTime.of(22, 0));
        Bar mejana = new Bar("La Mejana", new Direccion("Tudela", "Navarra", "Gaztambide", 3));
        Restaurante meson = new Restaurante(28.0, 6, 50, "Meson del Ebro",
                new Direccion("Tudela", "Navarra", "Herrerias", 12));
        for (Local l : new Local[]{txoko, berri, rincon, cocina, asador, pepe, ciudadela, luna, mejana, meson}) {
            comprobar(s.nuevoLocal(l), "local " + l.getNombre());
        }
        comprobar(s.asociarLocal(txoko, marta) && s.asociarLocal(asador, marta)
                && s.asociarLocal(cocina, marta), "locales de marta");
        comprobar(s.asociarLocal(berri, jon) && s.asociarLocal(luna, jon), "locales de jon");

        Review anaTxoko = resena(s, ana, txoko, 5, "Los mejores pintxos de la calle", 12);
        resena(s, ana, berri, 4, "Buen vermut", 10);
        resena(s, ana, asador, 4, "Chuleton en su punto", 9);
        resena(s, ana, cocina, 5, "Menu degustacion excelente", 7);
        resena(s, ana, luna, 3, "Musica demasiado alta", 3);
        resena(s, iker, txoko, 3, "Algo caro", 11);
        resena(s, iker, berri, 5, "Ambiente inmejorable", 8);
        resena(s, iker, rincon, 2, "Servicio lento", 6);
        Review ikerAsador = resena(s, iker, asador, 3, "Correcto, sin mas", 5);
        resena(s, iker, pepe, 4, "Buena relacion calidad-precio", 4);
        resena(s, eva, berri, 4, "Repetiria", 9);
        resena(s, eva, cocina, 4, "Postres muy buenos", 6);
        resena(s, eva, pepe, 2, "Raciones escasas", 5);
        resena(s, eva, meson, 5, "Verduras de la Ribera", 15);
        resena(s, eva, mejana, 4, "Terraza agradable", 14);

        comprobar(s.nuevaContestacion(new Contestacion("Gracias, Ana! Te esperamos pronto.", anaTxoko, marta),
                anaTxoko), "contestacion a ana");
        comprobar(s.nuevaContestacion(new Contestacion("Tomamos nota para mejorar.", ikerAsador, marta),
                ikerAsador), "contestacion a iker");
        comprobar(s.nuevaReserva(ana, asador, LocalDate.now().plusDays(3), LocalTime.of(21, 30)), "reserva");
    }

    private static Direccion pamplona(String calle, int numero) {
        return new Direccion(CIUDAD, "Navarra", calle, numero);
    }

    private static Review resena(BusinessSystem s, Cliente c, Local l, int puntuacion, String comentario,
            int haceDias) {
        Review r = new Review(c.getId(), l.getId(), puntuacion, comentario, LocalDate.now().minusDays(haceDias));
        comprobar(s.nuevaReview(r), "review de " + c.getNick() + " en " + l.getNombre());
        return r;
    }

    private static void comprobar(boolean ok, String que) {
        if (!ok) {
            throw new IllegalStateException("No se pudo dar de alta: " + que);
        }
    }
}
