package Pruebas;

import GSILabs.connect.BusinessServer;
import GSILabs.connect.ClientHub;
import GSILabs.connect.RemoteConnector;
import java.util.ArrayList;
import java.util.List;

/**
 * Pruebas de la Practica 04 (issue #6): {@link ClientHub} y
 * {@link RemoteConnector} contra un {@link BusinessServer} real, cada uno en
 * su propia JVM y en esta maquina (127.0.0.1). Los reintentos se hacen cada
 * segundo para que las pruebas sean rapidas.
 * <p>
 * Escenarios:
 * <ol>
 * <li>Tag incorrecto: el cliente muestra los tags publicados y lo vuelve a
 * pedir.</li>
 * <li>Servidor no disponible al arrancar: el cliente avisa, espera y se
 * conecta cuando el servidor arranca.</li>
 * <li>Caida del servidor entre dos rondas de operaciones: el cliente avisa,
 * espera, y completa la ronda cuando el servidor vuelve.</li>
 * </ol>
 */
public class P04ClientHubTester {

    private static int correctos = 0;
    private static int fallidos = 0;
    private static final List<ProcesoJava> procesos = new ArrayList<>();

    /**
     * Ejecuta todas las pruebas y muestra un resumen.
     *
     * @param args no se usan
     * @throws Exception si falla algo fuera de las pruebas
     */
    public static void main(String[] args) throws Exception {
        try {
            probarTagIncorrecto();
            probarServidorTardioYCaida();
        } finally {
            for (ProcesoJava p : procesos) {
                p.destruir();
            }
        }
        System.out.println();
        System.out.println("RESUMEN: " + correctos + " correctos, " + fallidos + " fallidos (de "
                + (correctos + fallidos) + ")");
        if (fallidos > 0) {
            System.exit(1);
        }
    }

    // ---------------------------------------------------------------- Pruebas
    private static void probarTagIncorrecto() throws Exception {
        int puerto = ProcesoJava.puertoLibre();
        ProcesoJava servidor = servidor(puerto);
        ProcesoJava hub = hub();
        hub.escribir("127.0.0.1");
        hub.escribir(String.valueOf(puerto));
        hub.escribir("NoExiste");
        boolean avisa = hub.esperarTexto("Tags publicados: [ClientGateway, AdminGateway]", 1, 20);
        hub.escribir("");
        boolean conecta = hub.esperarTexto("Repetir las operaciones?", 1, 30);
        hub.escribir("n");
        informar("T1", avisa && conecta && hub.esperarFin(10) == 0,
                "con un tag incorrecto muestra los tags publicados, lo vuelve a pedir y se conecta", hub);
        informar("T2", hub.contar("-> true") == 2 && hub.contar("-> false") == 2
                && hub.contar("Txoko Berri [Bar") == 3 && hub.contar("(vacio)") == 1,
                "la ronda muestra los resultados esperados con DatosIniciales", hub);
        parar(servidor);
    }

    private static void probarServidorTardioYCaida() throws Exception {
        int puerto = ProcesoJava.puertoLibre();
        ProcesoJava hub = hub();
        hub.escribir("127.0.0.1");
        hub.escribir(String.valueOf(puerto));
        hub.escribir("");
        boolean espera = hub.esperarTexto("AVISO: no se puede contactar", 2, 20);
        informar("A1", espera, "sin servidor, el cliente avisa y sigue reintentando", hub);

        ProcesoJava servidor = servidor(puerto);
        boolean ronda1 = hub.esperarTexto("Repetir las operaciones?", 1, 30);
        informar("A2", ronda1 && hub.contar("Conectado a 'ClientGateway'") == 1 && hub.contar("[9]") == 1,
                "al arrancar el servidor, el cliente se conecta y completa la ronda", hub);

        parar(servidor);
        hub.escribir("s");
        boolean avisaCaida = hub.esperarTexto("AVISO: fallo la operacion", 1, 20)
                && hub.esperarTexto("AVISO: no se puede contactar", 3, 20);
        informar("C1", avisaCaida, "con el servidor caido, el cliente avisa y queda a la espera", hub);

        ProcesoJava nuevo = servidor(puerto);
        boolean ronda2 = hub.esperarTexto("Repetir las operaciones?", 2, 40);
        hub.escribir("n");
        informar("C2", ronda2 && hub.contar("[9]") == 2 && hub.contar("--- Ronda 2") == 1
                && hub.esperarFin(10) == 0,
                "cuando el servidor vuelve, el cliente completa la ronda y termina normalmente", hub);
        parar(nuevo);
    }

    // -------------------------------------------------------------- Utilidades
    private static ProcesoJava servidor(int puerto) throws Exception {
        ProcesoJava s = ProcesoJava.lanzar(BusinessServer.class, List.of(),
                "--host", "127.0.0.1", "--puerto", String.valueOf(puerto));
        procesos.add(s);
        if (!s.esperarTexto(BusinessServer.MENSAJE_LISTO, 1, 20)) {
            throw new IllegalStateException("El servidor no arranco\n" + s.salida());
        }
        return s;
    }

    private static ProcesoJava hub() throws Exception {
        ProcesoJava h = ProcesoJava.lanzar(ClientHub.class, List.of(RemoteConnector.PROPIEDAD_REINTENTO + "=1"));
        procesos.add(h);
        return h;
    }

    private static void parar(ProcesoJava servidor) throws Exception {
        servidor.escribir("salir");
        if (servidor.esperarFin(15) != 0) {
            throw new IllegalStateException("El servidor no se detuvo\n" + servidor.salida());
        }
    }

    private static void informar(String id, boolean ok, String descripcion, ProcesoJava p) {
        if (ok) {
            correctos++;
        } else {
            fallidos++;
        }
        System.out.println((ok ? "[OK]    " : "[FALLO] ") + id + " - " + descripcion);
        if (!ok) {
            System.out.println(p.salida());
        }
    }
}
