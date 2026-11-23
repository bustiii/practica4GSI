package Pruebas;

import GSILabs.BModel.Bar;
import GSILabs.BModel.Cliente;
import GSILabs.BModel.Contestacion;
import GSILabs.BModel.Direccion;
import GSILabs.BModel.Dueno;
import GSILabs.BModel.Local;
import GSILabs.BModel.Restaurante;
import GSILabs.BModel.Review;
import GSILabs.BModel.Usuario;
import GSILabs.connect.AdminGateway;
import GSILabs.connect.ClientGateway;
import GSILabs.connect.PublicBusinessSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Arrays;

/**
 * Pruebas de la Practica 04 (issue #3): reviews de los clientes
 * ({@code insertaReview}, {@code quitaReview}) y reviews falsas del
 * administrador ({@code insertaReviewFalsa}), invocadas en local, sin RMI.
 * <p>
 * Para imitar lo que ocurre con RMI, muchos argumentos se pasan como copias
 * serializadas y deserializadas, que es lo que recibe el servidor.
 */
public class P04ReviewsTester {

    private static int correctos = 0;
    private static int fallidos = 0;

    private static final LocalDate AYER = LocalDate.now().minusDays(1);

    private static PublicBusinessSystem sis;
    private static Cliente ana, iker;
    private static Dueno marta;
    private static Bar bar;
    private static Restaurante rest;
    private static Review contestada, sinContestar;

    /**
     * Ejecuta todas las pruebas y muestra un resumen.
     *
     * @param args no se usan
     */
    public static void main(String[] args) {
        crearSistema();

        probarInsertaReview();
        probarQuitaReview();
        probarInsertaReviewFalsa();

        System.out.println();
        System.out.println("RESUMEN: " + correctos + " correctos, " + fallidos + " fallidos (de "
                + (correctos + fallidos) + ")");
        if (fallidos > 0) {
            System.exit(1);
        }
    }

    // ---------------------------------------------------------------- Pruebas
    private static void probarInsertaReview() {
        ClientGateway gw = sis;

        Review nueva = new Review(ana.getId(), rest.getId(), 4, "Buen menu", AYER);
        intentar("I1", "inserta una review correcta y queda en el sistema y en el local", () -> {
            boolean ok = gw.insertaReview(copia(nueva));
            return ok && contiene(sis.verReviews(rest), nueva) && rest.getReviews().contains(nueva);
        });
        intentar("I2", "rechaza otra review del mismo cliente, local y fecha",
                () -> !gw.insertaReview(new Review(ana.getId(), rest.getId(), 1, "Otra vez", AYER)));
        intentar("I3", "admite otra review del mismo cliente y local en otra fecha",
                () -> gw.insertaReview(new Review(ana.getId(), rest.getId(), 5, "", AYER.minusDays(7))));
        intentar("I4", "rechaza cliente inexistente, local inexistente y review nula",
                () -> !gw.insertaReview(new Review(999_999, rest.getId(), 3, "", AYER))
                && !gw.insertaReview(new Review(ana.getId(), 999_999, 3, "", AYER))
                && !gw.insertaReview(null));
        intentar("I5", "rechaza una review cuyo autor es un dueno",
                () -> !gw.insertaReview(new Review(marta.getId(), rest.getId(), 5, "", AYER)));

        intentar("I6", "rechaza reviews manipuladas que no pasarian las validaciones del constructor", () -> {
            Review mala = new Review(iker.getId(), rest.getId(), 3, "", AYER.minusDays(3));
            forzar(mala, "valoracion", 9);
            Review futura = new Review(iker.getId(), rest.getId(), 3, "", AYER.minusDays(4));
            forzar(futura, "fechaVisita", LocalDate.now().plusDays(10));
            Review larga = new Review(iker.getId(), rest.getId(), 3, "", AYER.minusDays(5));
            forzar(larga, "comentario", "x".repeat(Review.COMENTARIO_MAX + 1));
            return !gw.insertaReview(copia(mala)) && !gw.insertaReview(copia(futura))
                    && !gw.insertaReview(copia(larga));
        });

        Review choque = new Review(sinContestar.getId(), iker.getId(), rest.getId(), 2, "Mismo id",
                AYER.minusDays(2), AYER.minusDays(2));
        intentar("I7", "si el id ya existe en el servidor, la review se guarda con otro id", () -> {
            boolean ok = gw.insertaReview(copia(choque));
            Review guardada = guardada(rest, choque);
            return ok && guardada != null && guardada.getId() != sinContestar.getId()
                    && sis.obtenerReview(sinContestar.getId()) == sinContestar;
        });
        intentar("I8", "se guarda una copia: cambiar el objeto enviado no altera la guardada", () -> {
            Review enviada = new Review(iker.getId(), bar.getId(), 4, "", AYER.minusDays(9));
            gw.insertaReview(enviada);
            enviada.setValoracion(0);
            Review guardada = guardada(bar, enviada);
            return guardada != null && guardada != enviada && guardada.getValoracion() == 4;
        });
        intentar("I9", "la fecha de creacion de la review guardada es la del servidor", () -> {
            Review vieja = new Review(1_500_000, iker.getId(), rest.getId(), 3, "", AYER.minusDays(20),
                    AYER.minusDays(15));
            return gw.insertaReview(copia(vieja))
                    && guardada(rest, vieja).getFechaCreacion().equals(LocalDate.now());
        });
        intentar("I10", "conserva el id recibido si esta libre", () -> {
            Review libre = new Review(1_600_000, ana.getId(), bar.getId(), 3, "", AYER.minusDays(30), AYER);
            return gw.insertaReview(copia(libre)) && guardada(bar, libre).getId() == 1_600_000;
        });
    }

    private static void probarQuitaReview() {
        ClientGateway gw = sis;
        intentar("Q1", "quita una review sin contestacion del sistema y del local", () -> {
            boolean ok = gw.quitaReview(copia(sinContestar));
            return ok && !contiene(sis.verReviews(bar), sinContestar) && !bar.getReviews().contains(sinContestar);
        });
        intentar("Q2", "no quita una review con contestacion",
                () -> !gw.quitaReview(copia(contestada)) && contiene(sis.verReviews(bar), contestada)
                && sis.tieneContestacion(contestada));
        intentar("Q3", "no quita una review inexistente ni una nula",
                () -> !gw.quitaReview(new Review(ana.getId(), bar.getId(), 3, "", AYER.minusDays(60)))
                && !gw.quitaReview(null));
        intentar("Q4", "una review ya quitada no se puede quitar dos veces",
                () -> !gw.quitaReview(sinContestar));
        intentar("Q5", "quita con la copia del cliente aunque el servidor cambiara su id", () -> {
            Review delCliente = new Review(contestada.getId(), ana.getId(), rest.getId(), 1, "",
                    AYER.minusDays(40), AYER.minusDays(40));
            boolean insertada = sis.insertaReview(copia(delCliente));
            boolean idCambiado = guardada(rest, delCliente).getId() != delCliente.getId();
            return insertada && idCambiado && gw.quitaReview(copia(delCliente))
                    && guardada(rest, delCliente) == null && sis.obtenerReview(contestada.getId()) == contestada;
        });
    }

    private static void probarInsertaReviewFalsa() {
        AdminGateway gw = sis;
        intentar("F1", "crea un cliente falso valido y una review con la puntuacion pedida", () -> {
            int antes = sis.verReviews(bar).length;
            boolean ok = gw.insertaReviewFalsa(copia(bar), 2);
            Review[] rs = sis.verReviews(bar);
            Review falsa = rs[rs.length - 1];
            Usuario autor = sis.obtenerUsuario(falsa.getCliente());
            return ok && rs.length == antes + 1 && falsa.getValoracion() == 2
                    && autor instanceof Cliente && autor.getNick().startsWith("usuario")
                    && autor.getEdad() >= Usuario.EDAD_MIN && bar.getReviews().contains(falsa);
        });
        intentar("F2", "dos reviews falsas en el mismo local usan clientes distintos", () -> {
            boolean ok = gw.insertaReviewFalsa(bar, 5) && gw.insertaReviewFalsa(bar, 5);
            Review[] rs = sis.verReviews(bar);
            return ok && rs[rs.length - 1].getCliente() != rs[rs.length - 2].getCliente();
        });
        intentar("F3", "rechaza puntuaciones fuera de rango o nulas sin crear clientes", () -> {
            String antes = ultimoNickFalso();
            boolean rechazadas = !gw.insertaReviewFalsa(bar, 6) && !gw.insertaReviewFalsa(bar, -1)
                    && !gw.insertaReviewFalsa(bar, null);
            gw.insertaReviewFalsa(bar, 3);
            int n = Integer.parseInt(antes.substring("usuario".length()));
            return rechazadas && ultimoNickFalso().equals("usuario" + (n + 1));
        });
        intentar("F4", "rechaza locales no registrados, nulos o de otro tipo en la misma direccion",
                () -> !gw.insertaReviewFalsa(new Bar("Fantasma", new Direccion("Olite", "Navarra", "Rua", 1)), 4)
                && !gw.insertaReviewFalsa(null, 4)
                && !gw.insertaReviewFalsa(new Restaurante(10, 2, 10, "Impostor", bar.getUbicacion()), 4));
        intentar("F5", "salta los nicks que ya existen", () -> {
            PublicBusinessSystem otro = new PublicBusinessSystem();
            Bar b = new Bar("Unico", new Direccion("Tafalla", "Navarra", "Plaza", 3));
            otro.nuevoLocal(b);
            otro.nuevoUsuario(new Cliente("usuario1", "real", LocalDate.of(1990, 1, 1)));
            boolean ok = otro.insertaReviewFalsa(b, 4);
            Review r = otro.verReviews(b)[0];
            return ok && otro.obtenerUsuario(r.getCliente()).getNick().equals("usuario2");
        });
    }

    // ------------------------------------------------------------------ Datos
    private static void crearSistema() {
        sis = new PublicBusinessSystem();
        ana = new Cliente("anaRev", "pass", LocalDate.of(2000, 1, 1));
        iker = new Cliente("ikerRev", "pass", LocalDate.of(1998, 7, 2));
        marta = new Dueno("martaRev", "pass", LocalDate.of(1975, 11, 20));
        sis.nuevoUsuario(ana);
        sis.nuevoUsuario(iker);
        sis.nuevoUsuario(marta);

        bar = new Bar("Bar Txoko", new Direccion("Pamplona", "Navarra", "San Nicolas", 4));
        rest = new Restaurante(22.0, 6, 80, "Asador Ribera", new Direccion("Pamplona", "Navarra", "Navarreria", 10));
        sis.nuevoLocal(bar);
        sis.nuevoLocal(rest);
        sis.asociarLocal(bar, marta);

        contestada = new Review(ana.getId(), bar.getId(), 5, "Muy bueno", AYER);
        sinContestar = new Review(iker.getId(), bar.getId(), 2, "Lento", AYER);
        sis.nuevaReview(contestada);
        sis.nuevaReview(sinContestar);
        sis.nuevaContestacion(new Contestacion("Gracias", contestada, marta), contestada);
    }

    // -------------------------------------------------------------- Utilidades
    /**
     * Copia serializada de un objeto, como la que recibe el servidor por RMI.
     */
    @SuppressWarnings("unchecked")
    private static <T> T copia(T o) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
                out.writeObject(o);
            }
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                return (T) in.readObject();
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Cambia un atributo saltandose las validaciones, como podria llegar un
     * objeto manipulado desde un cliente.
     */
    private static void forzar(Object o, String campo, Object valor) throws ReflectiveOperationException {
        Field f = o.getClass().getDeclaredField(campo);
        f.setAccessible(true);
        f.set(o, valor);
    }

    private static boolean contiene(Review[] rs, Review r) {
        return rs != null && Arrays.asList(rs).contains(r);
    }

    /**
     * Review guardada en el sistema equivalente a r (mismo cliente, local y
     * fecha de visita), o null.
     */
    private static Review guardada(Local l, Review r) {
        for (Review s : sis.verReviews(l)) {
            if (s.equals(r)) {
                return s;
            }
        }
        return null;
    }

    private static String ultimoNickFalso() {
        Review[] rs = sis.verReviews(bar);
        return sis.obtenerUsuario(rs[rs.length - 1].getCliente()).getNick();
    }

    /**
     * Prueba que devuelve true si se cumple lo esperado.
     */
    @FunctionalInterface
    private interface Prueba {

        boolean ejecutar() throws Exception;
    }

    private static void intentar(String id, String descripcion, Prueba prueba) {
        boolean ok;
        String detalle = "";
        try {
            ok = prueba.ejecutar();
        } catch (Exception e) {
            ok = false;
            detalle = " -> excepcion inesperada: " + e;
        }
        informar(id, ok, descripcion + detalle);
    }

    private static void informar(String id, boolean ok, String descripcion) {
        if (ok) {
            correctos++;
        } else {
            fallidos++;
        }
        System.out.println((ok ? "[OK]    " : "[FALLO] ") + id + " - " + descripcion);
    }
}
