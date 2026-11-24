package GSILabs.connect;

import GSILabs.BModel.Direccion;
import GSILabs.BModel.Local;
import GSILabs.BModel.Review;
import java.util.List;
import java.util.Locale;

/**
 * Formato de texto de los objetos que devuelven las pasarelas remotas, comun a
 * {@link ClientHub} y {@code AdminHub}. La puntuacion media se calcula en el
 * cliente con las reviews que llegan dentro de cada local.
 */
final class Presentacion {

    private Presentacion() {
    }

    /**
     * Describe un local en una linea: nombre, tipo, direccion, numero de
     * reviews y puntuacion media.
     *
     * @param l local (puede ser null)
     * @return la descripcion, o "null" si no hay local
     */
    static String local(Local l) {
        if (l == null) {
            return "null";
        }
        Direccion d = l.getUbicacion();
        List<Review> rs = l.getReviews();
        String media = rs.isEmpty() ? "sin reviews"
                : String.format(Locale.ROOT, "media %.2f", rs.stream().mapToInt(Review::getValoracion).average()
                        .orElse(0));
        return String.format(Locale.ROOT, "%s [%s, id %d] - %s %d, %s - %d reviews, %s", l.getNombre(),
                l.getClass().getSimpleName(), l.getId(), d.getCalle(), d.getNumero(), d.getLocalidad(),
                rs.size(), media);
    }

    /**
     * Describe una review en una linea.
     *
     * @param r review (puede ser null)
     * @return la descripcion, o "null" si no hay review
     */
    static String review(Review r) {
        if (r == null) {
            return "null";
        }
        return String.format(Locale.ROOT, "review %d del cliente %d en el local %d: %d estrellas, visita %s, \"%s\"",
                r.getId(), r.getCliente(), r.getLocal(), r.getValoracion(), r.getFechaVisita(), r.getComentario());
    }

    /**
     * Escribe una lista numerada de locales, con "(vacio)" en las posiciones
     * nulas.
     *
     * @param locales locales a escribir
     */
    static void lista(Local[] locales) {
        if (locales == null) {
            System.out.println("      null");
            return;
        }
        if (locales.length == 0) {
            System.out.println("      (ninguno)");
        }
        for (int i = 0; i < locales.length; i++) {
            System.out.println("      " + (i + 1) + ". " + (locales[i] == null ? "(vacio)" : local(locales[i])));
        }
    }

    /**
     * Escribe la cabecera de una operacion.
     *
     * @param n numero de la operacion
     * @param descripcion llamada que se hace
     */
    static void operacion(int n, String descripcion) {
        System.out.println();
        System.out.println("[" + n + "] " + descripcion);
    }

    /**
     * Escribe el resultado de una operacion.
     *
     * @param resultado texto del resultado
     */
    static void resultado(String resultado) {
        System.out.println("    -> " + resultado);
    }
}
