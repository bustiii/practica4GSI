package GSILabs.connect;

import GSILabs.BModel.Bar;
import GSILabs.BModel.Local;
import GSILabs.BModel.Restaurante;
import GSILabs.BModel.Review;
import java.time.LocalDate;

/**
 * Cliente de prueba de la pasarela {@link ClientGateway} (Practica 4,
 * Ejercicio 3).
 * <p>
 * Pide por consola la direccion del servidor, el puerto y el tag (con
 * {@link RemoteConnector}) y ejecuta una serie de operaciones predefinidas
 * sobre el objeto remoto, mostrando cada llamada y su resultado:
 * {@code getLocal}, {@code getLocals}, {@code mejorBar},
 * {@code mejoresRestaurantes}, {@code insertaReview} y {@code quitaReview}.
 * Al terminar pregunta si se quieren repetir; asi se puede detener el
 * servidor entre dos rondas y comprobar que el cliente avisa, espera y
 * continua cuando el servidor vuelve.
 * <p>
 * Las operaciones estan pensadas para los datos de {@link DatosIniciales}.
 * La review que se inserta se quita despues, de modo que el sistema queda
 * como estaba y las rondas se pueden repetir.
 * <p>
 * Uso: {@code java GSILabs.connect.ClientHub}
 */
public final class ClientHub {

    private static final String LOCAL_EXACTO = "txoko BERRI";
    private static final String LOCAL_INEXISTENTE = "Bar Inexistente";
    private static final String NOMBRE_PARCIAL = "txoko";
    private static final int NUM_RESTAURANTES = 5;
    /**
     * Local en el que la clienta ana no tiene ninguna review.
     */
    private static final String LOCAL_PARA_RESENA = "Casa Pepe";
    /**
     * Local en el que la review de ana esta contestada por la duena.
     */
    private static final String LOCAL_CONTESTADO = "Bar Txoko";

    private ClientHub() {
    }

    /**
     * Conecta con el servidor y ejecuta las operaciones predefinidas.
     *
     * @param args no se usan
     */
    public static void main(String[] args) {
        System.out.println("=== ClientHub: cliente de la pasarela " + BusinessServer.TAG_CLIENTE + " ===");
        RemoteConnector<ClientGateway> conector =
                new RemoteConnector<>(ClientGateway.class, BusinessServer.TAG_CLIENTE);
        conector.pedirDatos();
        conector.conectar();
        int ronda = 1;
        do {
            System.out.println();
            System.out.println("--- Ronda " + ronda++ + " de operaciones ---");
            ejecutarOperaciones(conector);
        } while (conector.preguntarSiNo("Repetir las operaciones?", false));
        System.out.println("Fin de ClientHub.");
    }

    /**
     * Ejecuta la serie de operaciones predefinidas.
     *
     * @param c conector con el servidor
     */
    static void ejecutarOperaciones(RemoteConnector<ClientGateway> c) {
        Presentacion.operacion(1, "getLocal(\"" + LOCAL_EXACTO + "\")  (coincidencia exacta sin mayusculas)");
        Local local = c.ejecutar("getLocal", g -> g.getLocal(LOCAL_EXACTO));
        Presentacion.resultado(Presentacion.local(local));

        Presentacion.operacion(2, "getLocal(\"" + LOCAL_INEXISTENTE + "\")");
        Local ninguno = c.ejecutar("getLocal", g -> g.getLocal(LOCAL_INEXISTENTE));
        Presentacion.resultado(ninguno == null ? "null (no existe)" : Presentacion.local(ninguno));

        Presentacion.operacion(3, "getLocals(\"" + NOMBRE_PARCIAL + "\")  (coincidencia parcial)");
        Local[] parciales = c.ejecutar("getLocals", g -> g.getLocals(NOMBRE_PARCIAL));
        Presentacion.resultado(parciales.length + " locales");
        Presentacion.lista(parciales);

        Presentacion.operacion(4, "mejorBar(\"" + DatosIniciales.CIUDAD + "\")");
        Bar bar = c.ejecutar("mejorBar", g -> g.mejorBar(DatosIniciales.CIUDAD));
        Presentacion.resultado(Presentacion.local(bar));

        Presentacion.operacion(5, "mejoresRestaurantes(\"" + DatosIniciales.CIUDAD + "\", " + NUM_RESTAURANTES + ")");
        Restaurante[] mejores = c.ejecutar("mejoresRestaurantes",
                g -> g.mejoresRestaurantes(DatosIniciales.CIUDAD, NUM_RESTAURANTES));
        Presentacion.resultado("array de " + mejores.length + " posiciones");
        Presentacion.lista(mejores);

        probarReviews(c);
    }

    /**
     * Inserta una review de ana, intenta insertarla otra vez, la quita y
     * despues intenta quitar una review contestada.
     */
    private static void probarReviews(RemoteConnector<ClientGateway> c) {
        Local destino = c.ejecutar("getLocal", g -> g.getLocal(LOCAL_PARA_RESENA));
        if (destino == null) {
            System.out.println();
            System.out.println("[6-9] No existe el local " + LOCAL_PARA_RESENA
                    + " en el servidor: se omiten las pruebas de reviews (se esperaban los datos de DatosIniciales).");
            return;
        }
        Review r = new Review(DatosIniciales.ID_ANA, destino.getId(), 4, "Insertada desde ClientHub", LocalDate.now());

        Presentacion.operacion(6, "insertaReview(" + Presentacion.review(r) + ")");
        Presentacion.resultado(String.valueOf(c.<Boolean>ejecutar("insertaReview", g -> g.insertaReview(r))));

        Presentacion.operacion(7, "insertaReview(la misma review)  (repetida: debe rechazarse)");
        Presentacion.resultado(String.valueOf(c.<Boolean>ejecutar("insertaReview", g -> g.insertaReview(r))));

        Presentacion.operacion(8, "quitaReview(la review insertada)");
        Presentacion.resultado(String.valueOf(c.<Boolean>ejecutar("quitaReview", g -> g.quitaReview(r))));

        Local contestado = c.ejecutar("getLocal", g -> g.getLocal(LOCAL_CONTESTADO));
        Review deAna = null;
        if (contestado != null) {
            for (Review x : contestado.getReviews()) {
                if (x.getCliente() == DatosIniciales.ID_ANA) {
                    deAna = x;
                }
            }
        }
        if (deAna == null) {
            System.out.println();
            System.out.println("[9] No se encontro la review de ana en " + LOCAL_CONTESTADO + ": se omite.");
            return;
        }
        Review contestada = deAna;
        Presentacion.operacion(9, "quitaReview(" + Presentacion.review(contestada)
                + ")  (tiene contestacion: debe rechazarse)");
        Presentacion.resultado(String.valueOf(c.<Boolean>ejecutar("quitaReview", g -> g.quitaReview(contestada))));
    }
}
