package Pruebas;

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
import GSILabs.connect.AdminGateway;
import GSILabs.connect.PublicBusinessSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;

/**
 * Pruebas de la Practica 04 (issue #4): borrados del administrador
 * ({@code eliminaReview}, {@code eliminaReviewsDeLocal},
 * {@code eliminaReviewsDeUsuario} y {@code eliminaLocal}), invocados en
 * local, sin RMI. Cada grupo de pruebas parte de un sistema nuevo.
 * <p>
 * Datos: Marta es duena del bar y del restaurante. Reviews: en el bar, de Ana
 * (contestada) y de Iker; en el restaurante, de Ana y de Iker (contestada); en
 * el pub, de Ana. Ana tiene una reserva en el restaurante.
 */
public class P04AdminTester {

    private static int correctos = 0;
    private static int fallidos = 0;

    private static final LocalDate AYER = LocalDate.now().minusDays(1);

    private static PublicBusinessSystem sis;
    private static AdminGateway gw;
    private static Cliente ana, iker, eva;
    private static Dueno marta;
    private static Bar bar;
    private static Restaurante rest;
    private static Pub pub;
    private static Review anaBar, ikerBar, anaRest, ikerRest, anaPub;

    /**
     * Ejecuta todas las pruebas y muestra un resumen.
     *
     * @param args no se usan
     */
    public static void main(String[] args) {
        probarEliminaReview();
        probarEliminaReviewsDeLocal();
        probarEliminaReviewsDeUsuario();
        probarEliminaLocal();

        System.out.println();
        System.out.println("RESUMEN: " + correctos + " correctos, " + fallidos + " fallidos (de "
                + (correctos + fallidos) + ")");
        if (fallidos > 0) {
            System.exit(1);
        }
    }

    // ---------------------------------------------------------------- Pruebas
    private static void probarEliminaReview() {
        crearSistema();
        intentar("R1", "elimina una review sin contestacion del sistema y del local",
                () -> gw.eliminaReview(copia(ikerBar)) && !existe(bar, ikerBar)
                && !bar.getReviews().contains(ikerBar));
        intentar("R2", "elimina una review contestada junto con su contestacion",
                () -> gw.eliminaReview(copia(anaBar)) && !existe(bar, anaBar) && sinContestacion(anaBar));
        intentar("R3", "devuelve false si la review no existe, ya se elimino o es nula",
                () -> !gw.eliminaReview(anaBar) && !gw.eliminaReview(null)
                && !gw.eliminaReview(new Review(eva.getId(), bar.getId(), 3, "", AYER)));
        intentar("R4", "no toca las demas reviews",
                () -> existe(rest, anaRest) && existe(rest, ikerRest) && sis.tieneContestacion(ikerRest)
                && existe(pub, anaPub));
        intentar("R5", "BusinessSystem sigue sin borrar reviews contestadas (la regla del cliente)", () -> {
            BusinessSystem base = new BusinessSystem();
            Cliente c = new Cliente("baseCli", "pass", LocalDate.of(2000, 1, 1));
            Dueno d = new Dueno("baseDue", "pass", LocalDate.of(1980, 1, 1));
            Bar b = new Bar("Base", new Direccion("Olite", "Navarra", "Rua", 1));
            base.nuevoUsuario(c);
            base.nuevoUsuario(d);
            base.nuevoLocal(b);
            base.asociarLocal(b, d);
            Review r = new Review(c.getId(), b.getId(), 4, "", AYER);
            base.nuevaReview(r);
            base.nuevaContestacion(new Contestacion("Gracias", r, d), r);
            return !base.eliminaReview(r) && !sis.quitaReview(ikerRest) && existe(rest, ikerRest);
        });
    }

    private static void probarEliminaReviewsDeLocal() {
        crearSistema();
        intentar("L1", "elimina todas las reviews del local y sus contestaciones, pero no el local",
                () -> gw.eliminaReviewsDeLocal(copia(bar)) && sis.verReviews(bar).length == 0
                && bar.getReviews().isEmpty() && sinContestacion(anaBar) && sis.getLocal("Bar Txoko") == bar);
        intentar("L2", "no toca las reviews de otros locales",
                () -> sis.verReviews(rest).length == 2 && sis.tieneContestacion(ikerRest)
                && sis.verReviews(pub).length == 1);
        intentar("L3", "devuelve true con un local registrado sin reviews",
                () -> gw.eliminaReviewsDeLocal(bar));
        intentar("L4", "devuelve false con un local no registrado, nulo o de otro tipo en la misma direccion",
                () -> !gw.eliminaReviewsDeLocal(new Bar("Fantasma", new Direccion("Olite", "Navarra", "Rua", 2)))
                && !gw.eliminaReviewsDeLocal(null)
                && !gw.eliminaReviewsDeLocal(new Bar("Impostor", rest.getUbicacion()))
                && sis.verReviews(rest).length == 2);
    }

    private static void probarEliminaReviewsDeUsuario() {
        crearSistema();
        intentar("U1", "elimina todas las reviews del cliente (tambien las contestadas) y devuelve cuantas",
                () -> gw.eliminaReviewsDeUsuario(copia(iker)) == 2 && !existe(bar, ikerBar)
                && !existe(rest, ikerRest) && sinContestacion(ikerRest));
        intentar("U2", "el cliente sigue registrado y las reviews de otros clientes se mantienen",
                () -> sis.obtenerUsuario("ikerAdm") == iker && existe(bar, anaBar) && sis.tieneContestacion(anaBar)
                && existe(rest, anaRest) && existe(pub, anaPub));
        intentar("U3", "devuelve 0 si el cliente existe pero no tiene reviews",
                () -> gw.eliminaReviewsDeUsuario(iker) == 0 && gw.eliminaReviewsDeUsuario(eva) == 0);
        intentar("U4", "devuelve -1 si el cliente no existe, es nulo o su nick es de un dueno",
                () -> gw.eliminaReviewsDeUsuario(new Cliente("nadie", "pass", LocalDate.of(2000, 1, 1))) == -1
                && gw.eliminaReviewsDeUsuario(null) == -1
                && gw.eliminaReviewsDeUsuario(new Cliente("martaAdm", "pass", LocalDate.of(1990, 1, 1))) == -1);
        intentar("U5", "cuenta reviews de varios locales", () -> gw.eliminaReviewsDeUsuario(ana) == 3
                && sis.verReviews(bar).length == 0 && sis.verReviews(rest).length == 0
                && sis.verReviews(pub).length == 0);
    }

    private static void probarEliminaLocal() {
        crearSistema();
        int idAnaRest = anaRest.getId();
        int idIkerRest = ikerRest.getId();
        intentar("D1", "elimina el local recibido como copia", () -> gw.eliminaLocal(copia(rest))
                && sis.getLocal("Asador Ribera") == null && sis.obtenerLocal(rest.getId()) == null);
        intentar("D2", "elimina en cascada sus reviews, contestaciones y reservas",
                () -> sis.obtenerReview(idAnaRest) == null && sis.obtenerReview(idIkerRest) == null
                && sis.obtenerReservas(ana).length == 0);
        intentar("D3", "el local deja de figurar entre los de su duena",
                () -> !((Dueno) sis.obtenerUsuario("martaAdm")).getLocales().contains(rest)
                && ((Dueno) sis.obtenerUsuario("martaAdm")).getLocales().contains(bar));
        intentar("D4", "no toca los demas locales ni sus reviews",
                () -> sis.getLocal("Bar Txoko") == bar && sis.verReviews(bar).length == 2
                && sis.tieneContestacion(anaBar) && sis.verReviews(pub).length == 1);
        intentar("D5", "devuelve false si el local ya se elimino, no existe, es nulo o es de otro tipo",
                () -> !gw.eliminaLocal(rest) && !gw.eliminaLocal(null)
                && !gw.eliminaLocal(new Pub("Fantasma", new Direccion("Olite", "Navarra", "Rua", 3),
                        LocalTime.of(3, 0), LocalTime.of(22, 0)))
                && !gw.eliminaLocal(new Restaurante(10, 2, 10, "Impostor", bar.getUbicacion()))
                && sis.getLocal("Bar Txoko") == bar);
    }

    // ------------------------------------------------------------------ Datos
    private static void crearSistema() {
        sis = new PublicBusinessSystem();
        gw = sis;
        ana = new Cliente("anaAdm", "pass", LocalDate.of(2000, 1, 1));
        iker = new Cliente("ikerAdm", "pass", LocalDate.of(1998, 7, 2));
        eva = new Cliente("evaAdm", "pass", LocalDate.of(1990, 3, 3));
        marta = new Dueno("martaAdm", "pass", LocalDate.of(1975, 11, 20));
        sis.nuevoUsuario(ana);
        sis.nuevoUsuario(iker);
        sis.nuevoUsuario(eva);
        sis.nuevoUsuario(marta);

        bar = new Bar("Bar Txoko", new Direccion("Pamplona", "Navarra", "San Nicolas", 4));
        rest = new Restaurante(22.0, 6, 80, "Asador Ribera", new Direccion("Pamplona", "Navarra", "Navarreria", 10));
        pub = new Pub("Pub Luna", new Direccion("Pamplona", "Navarra", "Jarauta", 30),
                LocalTime.of(4, 0), LocalTime.of(23, 0));
        sis.nuevoLocal(bar);
        sis.nuevoLocal(rest);
        sis.nuevoLocal(pub);
        sis.asociarLocal(bar, marta);
        sis.asociarLocal(rest, marta);

        anaBar = resena(ana, bar, 5);
        ikerBar = resena(iker, bar, 2);
        anaRest = resena(ana, rest, 4);
        ikerRest = resena(iker, rest, 3);
        anaPub = resena(ana, pub, 4);
        comprobar(sis.nuevaContestacion(new Contestacion("Gracias", anaBar, marta), anaBar), "contestacion");
        comprobar(sis.nuevaContestacion(new Contestacion("Lo tendremos en cuenta", ikerRest, marta), ikerRest),
                "contestacion");
        comprobar(sis.nuevaReserva(ana, rest, LocalDate.now().plusDays(5), LocalTime.of(21, 30)), "reserva");
    }

    private static Review resena(Cliente c, Local l, int puntuacion) {
        Review r = new Review(c.getId(), l.getId(), puntuacion, "", AYER);
        comprobar(sis.nuevaReview(r), "review de " + l.getNombre());
        return r;
    }

    private static void comprobar(boolean ok, String que) {
        if (!ok) {
            throw new IllegalStateException("No se pudo crear: " + que);
        }
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

    private static boolean existe(Local l, Review r) {
        Review[] rs = sis.verReviews(l);
        return rs != null && Arrays.asList(rs).contains(r);
    }

    /**
     * Comprueba que una review borrada no conserva su contestacion: se vuelve
     * a insertar una review equivalente y no debe aparecer contestada. Despues
     * se retira para no alterar el resto de pruebas.
     */
    private static boolean sinContestacion(Review borrada) {
        Review igual = new Review(borrada.getCliente(), borrada.getLocal(), borrada.getValoracion(),
                borrada.getComentario(), borrada.getFechaVisita());
        boolean ok = sis.nuevaReview(igual) && !sis.tieneContestacion(igual);
        sis.quitaReview(igual);
        return ok;
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
