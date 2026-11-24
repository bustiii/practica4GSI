package Pruebas;

import GSILabs.BModel.Bar;
import GSILabs.BModel.Cliente;
import GSILabs.BModel.Local;
import GSILabs.BModel.Restaurante;
import GSILabs.BModel.Review;
import GSILabs.connect.AdminGateway;
import GSILabs.connect.BusinessServer;
import GSILabs.connect.ClientGateway;
import GSILabs.connect.DatosIniciales;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

/**
 * Prueba de extremo a extremo de la Practica 04 (issue #5): lanza
 * {@link BusinessServer} en otra JVM, en un puerto libre de esta maquina, y lo
 * usa por RMI a traves de {@link ClientGateway} y {@link AdminGateway}, como
 * haran los clientes. Al final detiene el servidor con la orden {@code salir}
 * y comprueba que las llamadas fallan con RemoteException.
 * <p>
 * Los resultados esperados salen de los datos de {@link DatosIniciales}.
 */
public class P04RMITester {

    private static int correctos = 0;
    private static int fallidos = 0;

    private static Process servidor;
    private static final StringBuilder salidaServidor = new StringBuilder();

    /**
     * Ejecuta todas las pruebas y muestra un resumen.
     *
     * @param args no se usan
     * @throws Exception si no se puede lanzar el servidor
     */
    public static void main(String[] args) throws Exception {
        int puerto = puertoLibre();
        try {
            arrancarServidor(puerto);
            Registry registro = LocateRegistry.getRegistry("127.0.0.1", puerto);

            intentar("S1", "el registro publica ClientGateway y AdminGateway (el mismo objeto)", () -> {
                Object c = registro.lookup(BusinessServer.TAG_CLIENTE);
                Object a = registro.lookup(BusinessServer.TAG_ADMIN);
                return c instanceof ClientGateway && a instanceof AdminGateway && c.equals(a);
            });
            esperarNotBound("S2", "un tag que no existe produce NotBoundException", registro);

            ClientGateway cli = (ClientGateway) registro.lookup(BusinessServer.TAG_CLIENTE);
            AdminGateway adm = (AdminGateway) registro.lookup(BusinessServer.TAG_ADMIN);
            probarConsultas(cli);
            probarCliente(cli);
            probarAdmin(adm, cli);
            probarCaida(cli, registro);
        } finally {
            if (servidor != null && servidor.isAlive()) {
                servidor.destroyForcibly();
            }
        }

        System.out.println();
        System.out.println("RESUMEN: " + correctos + " correctos, " + fallidos + " fallidos (de "
                + (correctos + fallidos) + ")");
        if (fallidos > 0) {
            System.out.println("Salida del servidor:\n" + salidaServidor);
            System.exit(1);
        }
    }

    // ---------------------------------------------------------------- Pruebas
    private static void probarConsultas(ClientGateway cli) {
        intentar("C1", "getLocal devuelve el local con sus reviews", () -> {
            Local l = cli.getLocal("  txoko BERRI");
            return l instanceof Bar && l.getNombre().equals("Txoko Berri") && l.getReviews().size() == 3;
        });
        intentar("C2", "getLocals devuelve coincidencias parciales", () -> {
            Local[] ls = cli.getLocals("txoko");
            return ls.length == 2 && ls[0].getNombre().equals("Bar Txoko") && ls[1].getNombre().equals("Txoko Berri");
        });
        intentar("C3", "mejorBar(Pamplona) es Txoko Berri",
                () -> cli.mejorBar(DatosIniciales.CIUDAD).getNombre().equals("Txoko Berri"));
        intentar("C4", "mejoresRestaurantes(Pamplona, 5) llega ordenado y con un null al final", () -> {
            Restaurante[] rs = cli.mejoresRestaurantes(DatosIniciales.CIUDAD, 5);
            return rs.length == 5 && rs[0].getNombre().equals("La Cocina de Marta")
                    && rs[1].getNombre().equals("Asador Ribera") && rs[2].getNombre().equals("Casa Pepe")
                    && rs[3].getNombre().equals("Restaurante Ciudadela") && rs[4] == null;
        });
    }

    private static void probarCliente(ClientGateway cli) {
        intentar("K1", "insertaReview por RMI: se inserta una vez y no dos", () -> {
            Local pepe = cli.getLocal("Casa Pepe");
            Review r = new Review(DatosIniciales.ID_ANA, pepe.getId(), 4, "Probado por RMI", LocalDate.now());
            return cli.insertaReview(r) && !cli.insertaReview(r)
                    && cli.getLocal("Casa Pepe").getReviews().size() == 3;
        });
        intentar("K2", "quitaReview por RMI con la copia del cliente", () -> {
            Local pepe = cli.getLocal("Casa Pepe");
            Review r = new Review(DatosIniciales.ID_ANA, pepe.getId(), 4, "Probado por RMI", LocalDate.now());
            return cli.quitaReview(r) && cli.getLocal("Casa Pepe").getReviews().size() == 2;
        });
        intentar("K3", "quitaReview rechaza una review contestada", () -> {
            Review contestada = cli.getLocal("Bar Txoko").getReviews().get(0);
            return contestada.getCliente() == DatosIniciales.ID_ANA && !cli.quitaReview(contestada);
        });
    }

    private static void probarAdmin(AdminGateway adm, ClientGateway cli) {
        intentar("A1", "insertaReviewFalsa por RMI", () -> adm.insertaReviewFalsa(adm.getLocal("Bar El Rincon"), 5)
                && adm.getLocal("Bar El Rincon").getReviews().size() == 2);
        intentar("A2", "eliminaReview por RMI borra una review contestada", () -> {
            Review contestada = adm.getLocal("Bar Txoko").getReviews().get(0);
            return adm.eliminaReview(contestada) && adm.getLocal("Bar Txoko").getReviews().size() == 1;
        });
        intentar("A3", "eliminaReviewsDeUsuario por RMI devuelve cuantas borra (eva tiene 5)", () -> {
            Cliente eva = new Cliente(DatosIniciales.ID_EVA, "eva", "otra", LocalDate.of(1990, 11, 30));
            return adm.eliminaReviewsDeUsuario(eva) == 5 && adm.eliminaReviewsDeUsuario(eva) == 0;
        });
        intentar("A4", "eliminaReviewsDeLocal por RMI", () -> adm.eliminaReviewsDeLocal(adm.getLocal("Txoko Berri"))
                && adm.getLocal("Txoko Berri").getReviews().isEmpty());
        intentar("A5", "eliminaLocal por RMI y el cambio lo ve tambien la pasarela de clientes",
                () -> adm.eliminaLocal(adm.getLocal("Pub Luna")) && cli.getLocal("Pub Luna") == null);
    }

    private static void probarCaida(ClientGateway cli, Registry registro) {
        intentar("D1", "la orden salir detiene el servidor", () -> {
            OutputStream in = servidor.getOutputStream();
            in.write("salir\n".getBytes(StandardCharsets.UTF_8));
            in.flush();
            return servidor.waitFor(15, TimeUnit.SECONDS) && servidor.exitValue() == 0;
        });
        intentar("D2", "con el servidor caido, las llamadas y el lookup lanzan RemoteException", () -> {
            boolean llamada = false;
            boolean lookup = false;
            try {
                cli.getLocal("Bar Txoko");
            } catch (RemoteException e) {
                llamada = true;
            }
            try {
                registro.lookup(BusinessServer.TAG_CLIENTE);
            } catch (RemoteException e) {
                lookup = true;
            }
            return llamada && lookup;
        });
    }

    // ---------------------------------------------------------------- Servidor
    private static void arrancarServidor(int puerto) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", System.getProperty("java.class.path"),
                BusinessServer.class.getName(), "--host", "127.0.0.1", "--puerto", String.valueOf(puerto));
        pb.redirectErrorStream(true);
        servidor = pb.start();
        Object listo = new Object();
        boolean[] arrancado = {false};
        Thread lector = new Thread(() -> {
            try (BufferedReader out = new BufferedReader(
                    new InputStreamReader(servidor.getInputStream(), StandardCharsets.UTF_8))) {
                String linea;
                while ((linea = out.readLine()) != null) {
                    synchronized (listo) {
                        salidaServidor.append("    | ").append(linea).append('\n');
                        if (linea.startsWith(BusinessServer.MENSAJE_LISTO)) {
                            arrancado[0] = true;
                            listo.notifyAll();
                        }
                    }
                }
            } catch (IOException e) {
                // El servidor ha terminado
            }
        });
        lector.setDaemon(true);
        lector.start();
        synchronized (listo) {
            long limite = System.currentTimeMillis() + 20_000;
            while (!arrancado[0] && System.currentTimeMillis() < limite && servidor.isAlive()) {
                listo.wait(500);
            }
        }
        if (!arrancado[0]) {
            throw new IllegalStateException("El servidor no arranco:\n" + salidaServidor);
        }
        System.out.println("Servidor arrancado en 127.0.0.1:" + puerto);
    }

    private static int puertoLibre() throws IOException {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

    // -------------------------------------------------------------- Utilidades
    /**
     * Prueba que devuelve true si se cumple lo esperado.
     */
    @FunctionalInterface
    private interface Prueba {

        boolean ejecutar() throws Exception;
    }

    private static void esperarNotBound(String id, String descripcion, Registry registro) {
        boolean ok;
        try {
            registro.lookup("NoExiste");
            ok = false;
        } catch (NotBoundException e) {
            ok = true;
        } catch (Exception e) {
            ok = false;
        }
        informar(id, ok, descripcion);
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
