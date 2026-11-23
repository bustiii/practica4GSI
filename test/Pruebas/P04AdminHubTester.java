package Pruebas;

import GSILabs.connect.AdminHub;
import GSILabs.connect.BusinessServer;
import GSILabs.connect.ClientHub;
import GSILabs.connect.RemoteConnector;
import java.util.ArrayList;
import java.util.List;

/**
 * Pruebas de la Practica 04 (issue #7): {@link AdminHub} contra un
 * {@link BusinessServer} real, cada uno en su propia JVM y en esta maquina
 * (127.0.0.1).
 * <ol>
 * <li>Una ronda de AdminHub sobre los datos iniciales da los resultados
 * esperados.</li>
 * <li>Un ClientHub lanzado despues ve los cambios del administrador (el
 * mejor bar de Pamplona pasa a ser Bar El Rincon).</li>
 * <li>Dos ClientHub conectados a la vez completan sus operaciones.</li>
 * </ol>
 * Las pruebas de caidas del servidor y tags incorrectos estan en
 * {@link P04ClientHubTester}, porque la conexion es comun a los dos hubs.
 */
public class P04AdminHubTester {

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
            int puerto = ProcesoJava.puertoLibre();
            ProcesoJava servidor = ProcesoJava.lanzar(BusinessServer.class, List.of(),
                    "--host", "127.0.0.1", "--puerto", String.valueOf(puerto));
            procesos.add(servidor);
            if (!servidor.esperarTexto(BusinessServer.MENSAJE_LISTO, 1, 20)) {
                throw new IllegalStateException("El servidor no arranco\n" + servidor.salida());
            }

            ProcesoJava admin = hub(AdminHub.class, puerto);
            boolean termina = admin.esperarTexto("Repetir las operaciones?", 1, 30);
            admin.escribir("n");
            informar("H1", termina && admin.esperarFin(10) == 0
                    && admin.contar("Conectado a 'AdminGateway'") == 1,
                    "AdminHub se conecta a AdminGateway y termina", admin);
            informar("H2", admin.contar("-> true") == 4 && admin.contar("-> false") == 1
                    && admin.contar("-> 5\n") == 1 && admin.contar("-> 0\n") == 1 && admin.contar("-> -1\n") == 1,
                    "las operaciones dan los resultados esperados con DatosIniciales", admin);
            informar("H3", admin.contar("-> 10 locales") == 1 && admin.contar("-> 9 locales") == 1
                    && admin.contar("getLocal(\"Pub Luna\") ahora: null") == 1
                    && admin.contar("-> ahora: Txoko Berri [Bar, id 2] - Estafeta 20, Pamplona - 0 reviews") == 1,
                    "los borrados se reflejan en las consultas posteriores", admin);

            ProcesoJava cliente = hub(ClientHub.class, puerto);
            cliente.esperarTexto("Repetir las operaciones?", 1, 30);
            cliente.escribir("n");
            informar("H4", cliente.esperarFin(10) == 0
                    && cliente.contar("[4] mejorBar(\"Pamplona\")\n    -> Bar El Rincon") == 1
                    && cliente.contar("No se encontro la review de ana en Bar Txoko") == 1,
                    "ClientHub ve los cambios del administrador", cliente);

            ProcesoJava c1 = hub(ClientHub.class, puerto);
            ProcesoJava c2 = hub(ClientHub.class, puerto);
            boolean ambos = c1.esperarTexto("Repetir las operaciones?", 1, 30)
                    && c2.esperarTexto("Repetir las operaciones?", 1, 30);
            c1.escribir("n");
            c2.escribir("n");
            informar("H5", ambos && c1.esperarFin(10) == 0 && c2.esperarFin(10) == 0
                    && c1.contar("[8] quitaReview") == 1 && c2.contar("[8] quitaReview") == 1
                    && c1.contar("AVISO") == 0 && c2.contar("AVISO") == 0,
                    "dos ClientHub conectados a la vez completan sus operaciones", c1);

            servidor.escribir("salir");
            servidor.esperarFin(15);
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

    private static ProcesoJava hub(Class<?> clase, int puerto) throws Exception {
        ProcesoJava h = ProcesoJava.lanzar(clase, List.of(RemoteConnector.PROPIEDAD_REINTENTO + "=1"));
        procesos.add(h);
        h.escribir("127.0.0.1");
        h.escribir(String.valueOf(puerto));
        h.escribir("");
        return h;
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
