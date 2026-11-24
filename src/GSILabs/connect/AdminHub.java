package GSILabs.connect;

import GSILabs.BModel.Cliente;
import GSILabs.BModel.Local;
import GSILabs.BModel.Review;
import java.time.LocalDate;

/**
 * Cliente de prueba de la pasarela {@link AdminGateway} (Practica 4,
 * Ejercicio 3).
 * <p>
 * Pide por consola la direccion del servidor, el puerto y el tag (con
 * {@link RemoteConnector}) y ejecuta una serie de operaciones predefinidas
 * sobre el objeto remoto, mostrando cada llamada y su resultado:
 * {@code getLocals}, {@code insertaReviewFalsa}, {@code eliminaReview},
 * {@code eliminaReviewsDeUsuario}, {@code eliminaReviewsDeLocal} y
 * {@code eliminaLocal}. Al terminar pregunta si se quieren repetir.
 * <p>
 * Las operaciones estan pensadas para los datos de {@link DatosIniciales} y
 * modifican el sistema: la primera ronda contra un servidor recien arrancado
 * da los resultados indicados en cada paso; en las siguientes, lo ya borrado
 * no existe y los borrados devuelven false, 0 o -1. Para volver a los datos
 * iniciales basta con reiniciar el servidor.
 * <p>
 * Uso: {@code java GSILabs.connect.AdminHub}
 */
public final class AdminHub {

    /**
     * Local que recibe la review falsa (pasa de media 2 a 3,5).
     */
    private static final String LOCAL_REVIEW_FALSA = "Bar El Rincon";
    private static final int PUNTUACION_FALSA = 5;
    private static final int PUNTUACION_INVALIDA = 9;
    /**
     * Local con la review contestada de ana que se elimina como administrador.
     */
    private static final String LOCAL_CONTESTADO = "Bar Txoko";
    /**
     * Local cuyas reviews se eliminan.
     */
    private static final String LOCAL_SIN_REVIEWS = "Txoko Berri";
    /**
     * Local que se elimina.
     */
    private static final String LOCAL_A_ELIMINAR = "Pub Luna";

    private AdminHub() {
    }

    /**
     * Conecta con el servidor y ejecuta las operaciones predefinidas.
     *
     * @param args no se usan
     */
    public static void main(String[] args) {
        System.out.println("=== AdminHub: cliente de la pasarela " + BusinessServer.TAG_ADMIN + " ===");
        RemoteConnector<AdminGateway> conector = new RemoteConnector<>(AdminGateway.class, BusinessServer.TAG_ADMIN);
        conector.pedirDatos();
        conector.conectar();
        int ronda = 1;
        do {
            System.out.println();
            System.out.println("--- Ronda " + ronda++ + " de operaciones ---");
            ejecutarOperaciones(conector);
        } while (conector.preguntarSiNo("Repetir las operaciones?", false));
        System.out.println("Fin de AdminHub.");
    }

    /**
     * Ejecuta la serie de operaciones predefinidas.
     *
     * @param c conector con el servidor
     */
    static void ejecutarOperaciones(RemoteConnector<AdminGateway> c) {
        Presentacion.operacion(1, "getLocals(\"\")  (todos los locales)");
        Local[] todos = c.ejecutar("getLocals", g -> g.getLocals(""));
        Presentacion.resultado(todos.length + " locales");
        Presentacion.lista(todos);

        Local rincon = c.ejecutar("getLocal", g -> g.getLocal(LOCAL_REVIEW_FALSA));
        Presentacion.operacion(2, "insertaReviewFalsa(" + nombre(rincon, LOCAL_REVIEW_FALSA) + ", "
                + PUNTUACION_FALSA + ")  (con los datos iniciales: true)");
        Presentacion.resultado(String.valueOf(c.<Boolean>ejecutar("insertaReviewFalsa",
                g -> g.insertaReviewFalsa(rincon, PUNTUACION_FALSA))));
        Presentacion.resultado("ahora: " + Presentacion.local(c.ejecutar("getLocal",
                g -> g.getLocal(LOCAL_REVIEW_FALSA))));

        Presentacion.operacion(3, "insertaReviewFalsa(" + nombre(rincon, LOCAL_REVIEW_FALSA) + ", "
                + PUNTUACION_INVALIDA + ")  (puntuacion fuera de rango: esperado false)");
        Presentacion.resultado(String.valueOf(c.<Boolean>ejecutar("insertaReviewFalsa",
                g -> g.insertaReviewFalsa(rincon, PUNTUACION_INVALIDA))));

        Review contestada = reviewDe(c.ejecutar("getLocal", g -> g.getLocal(LOCAL_CONTESTADO)),
                DatosIniciales.ID_ANA);
        Presentacion.operacion(4, "eliminaReview(" + Presentacion.review(contestada)
                + ")  (contestada: el administrador puede borrarla)");
        Presentacion.resultado(String.valueOf(c.<Boolean>ejecutar("eliminaReview", g -> g.eliminaReview(contestada))));

        Cliente eva = new Cliente(DatosIniciales.ID_EVA, "eva", "eva2026", LocalDate.of(1990, 11, 30));
        Presentacion.operacion(5, "eliminaReviewsDeUsuario(eva)  (con los datos iniciales: 5)");
        Presentacion.resultado(String.valueOf(c.<Integer>ejecutar("eliminaReviewsDeUsuario",
                g -> g.eliminaReviewsDeUsuario(eva))));
        Presentacion.operacion(6, "eliminaReviewsDeUsuario(eva) otra vez  (ya no tiene: esperado 0)");
        Presentacion.resultado(String.valueOf(c.<Integer>ejecutar("eliminaReviewsDeUsuario",
                g -> g.eliminaReviewsDeUsuario(eva))));
        Cliente nadie = new Cliente("clienteInexistente", "x", LocalDate.of(2000, 1, 1));
        Presentacion.operacion(7, "eliminaReviewsDeUsuario(clienteInexistente)  (con los datos iniciales: -1)");
        Presentacion.resultado(String.valueOf(c.<Integer>ejecutar("eliminaReviewsDeUsuario",
                g -> g.eliminaReviewsDeUsuario(nadie))));

        Local berri = c.ejecutar("getLocal", g -> g.getLocal(LOCAL_SIN_REVIEWS));
        Presentacion.operacion(8, "eliminaReviewsDeLocal(" + nombre(berri, LOCAL_SIN_REVIEWS) + ")  (con los datos iniciales: true)");
        Presentacion.resultado(String.valueOf(c.<Boolean>ejecutar("eliminaReviewsDeLocal",
                g -> g.eliminaReviewsDeLocal(berri))));
        Presentacion.resultado("ahora: " + Presentacion.local(c.ejecutar("getLocal",
                g -> g.getLocal(LOCAL_SIN_REVIEWS))));

        Local pub = c.ejecutar("getLocal", g -> g.getLocal(LOCAL_A_ELIMINAR));
        Presentacion.operacion(9, "eliminaLocal(" + nombre(pub, LOCAL_A_ELIMINAR) + ")  (con los datos iniciales: true)");
        Presentacion.resultado(String.valueOf(c.<Boolean>ejecutar("eliminaLocal", g -> g.eliminaLocal(pub))));
        Presentacion.resultado("getLocal(\"" + LOCAL_A_ELIMINAR + "\") ahora: "
                + Presentacion.local(c.ejecutar("getLocal", g -> g.getLocal(LOCAL_A_ELIMINAR))));

        Presentacion.operacion(10, "getLocals(\"\")  (estado final)");
        Local[] finales = c.ejecutar("getLocals", g -> g.getLocals(""));
        Presentacion.resultado(finales.length + " locales");
        Presentacion.lista(finales);
    }

    /**
     * Nombre del local para mostrarlo, o un aviso si no existe en el servidor.
     */
    private static String nombre(Local l, String buscado) {
        return l == null ? "null (no existe " + buscado + ")" : l.getNombre();
    }

    /**
     * Primera review de un cliente dentro de un local, o null.
     */
    private static Review reviewDe(Local l, int cliente) {
        if (l != null) {
            for (Review r : l.getReviews()) {
                if (r.getCliente() == cliente) {
                    return r;
                }
            }
        }
        return null;
    }
}
