package GSILabs.connect;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMISocketFactory;
import java.util.Arrays;

/**
 * Conexion con un objeto remoto publicado por {@link BusinessServer}, comun a
 * {@code ClientHub} y {@code AdminHub} (Practica 4, Ejercicio 3).
 * <p>
 * Pide por consola la direccion del servidor, el puerto del registro y el tag
 * del objeto remoto; localiza el registro con
 * {@link LocateRegistry#getRegistry(String, int)} y obtiene el objeto con
 * {@link Registry#lookup(String)}. Despues ejecuta las operaciones remotas a
 * traves de {@link #ejecutar(String, Operacion)}.
 * <p>
 * Gestion de errores:
 * <ul>
 * <li>Si el servidor no esta disponible al conectar, o se cae durante una
 * operacion ({@link RemoteException}), muestra un aviso y queda a la espera,
 * reintentando cada pocos segundos (5 por defecto, configurable con la
 * propiedad {@value #PROPIEDAD_REINTENTO}). Cuando el servidor vuelve, se
 * obtiene un stub nuevo y la operacion se repite.</li>
 * <li>Si el tag no existe ({@link NotBoundException}) o el objeto no
 * implementa la interfaz esperada, muestra los tags publicados y vuelve a
 * pedir el tag.</li>
 * <li>Las conexiones tienen un tiempo maximo de establecimiento y de
 * respuesta, para que una direccion inalcanzable no bloquee el programa
 * durante minutos.</li>
 * </ul>
 * Una operacion que falla por la red se repite al reconectar. Si el fallo se
 * produjo despues de que el servidor la ejecutara (por ejemplo, se perdio la
 * respuesta), una insercion repetida devolvera false porque ya existe.
 *
 * @param <T> interfaz remota del objeto ({@link ClientGateway} o
 * {@link AdminGateway})
 */
public class RemoteConnector<T extends Remote> {

    /**
     * Segundos de espera por defecto entre reintentos.
     */
    public static final int SEGUNDOS_REINTENTO = 5;
    /**
     * Propiedad del sistema que permite cambiar los segundos entre
     * reintentos.
     */
    public static final String PROPIEDAD_REINTENTO = "gsi.reintento";
    /**
     * Direccion por defecto del servidor.
     */
    public static final String HOST_POR_DEFECTO = "localhost";
    /**
     * Tiempo maximo para establecer una conexion, en milisegundos.
     */
    private static final int TIMEOUT_CONEXION_MS = 5_000;
    /**
     * Tiempo maximo de espera de una respuesta, en milisegundos.
     */
    private static final String TIMEOUT_RESPUESTA_MS = "15000";

    private static boolean tiemposConfigurados = false;

    private final Class<T> tipo;
    private final String tagPorDefecto;
    private final BufferedReader in;
    private final PrintStream out;
    private final long esperaMs;

    private String host = HOST_POR_DEFECTO;
    private int puerto = BusinessServer.PUERTO_POR_DEFECTO;
    private String tag;
    private T remoto;
    private boolean entradaAgotada = false;

    /**
     * Crea un conector que usa la consola (entrada y salida estandar).
     *
     * @param tipo interfaz remota esperada
     * @param tagPorDefecto tag que se propone al usuario
     */
    public RemoteConnector(Class<T> tipo, String tagPorDefecto) {
        this(tipo, tagPorDefecto, new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)),
                System.out);
    }

    /**
     * Crea un conector con una entrada y una salida concretas.
     *
     * @param tipo interfaz remota esperada
     * @param tagPorDefecto tag que se propone al usuario
     * @param in entrada de la que se leen las respuestas del usuario
     * @param out salida en la que se escriben preguntas y avisos
     */
    public RemoteConnector(Class<T> tipo, String tagPorDefecto, BufferedReader in, PrintStream out) {
        this.tipo = tipo;
        this.tagPorDefecto = tagPorDefecto;
        this.tag = tagPorDefecto;
        this.in = in;
        this.out = out;
        this.esperaMs = 1000L * Math.max(1, Integer.getInteger(PROPIEDAD_REINTENTO, SEGUNDOS_REINTENTO));
        configurarTiempos();
    }

    /**
     * Pide al usuario la direccion del servidor, el puerto y el tag. Si el
     * usuario pulsa Enter sin escribir nada se usa el valor propuesto entre
     * corchetes.
     */
    public void pedirDatos() {
        host = preguntar("Direccion del servidor", HOST_POR_DEFECTO);
        while (true) {
            String p = preguntar("Puerto del registro RMI", String.valueOf(BusinessServer.PUERTO_POR_DEFECTO));
            try {
                puerto = Integer.parseInt(p);
                if (puerto >= 1 && puerto <= 65535) {
                    break;
                }
            } catch (NumberFormatException e) {
                // Se vuelve a preguntar
            }
            out.println("El puerto debe ser un numero entre 1 y 65535.");
            if (entradaAgotada) {
                puerto = BusinessServer.PUERTO_POR_DEFECTO;
                break;
            }
        }
        tag = preguntar("Tag del objeto remoto (" + tipo.getSimpleName() + ")", tagPorDefecto);
    }

    /**
     * Obtiene el objeto remoto. Si el servidor no responde, avisa y reintenta
     * hasta conseguirlo; si el tag no es valido, lo vuelve a pedir.
     *
     * @return el stub del objeto remoto
     */
    public T conectar() {
        int intento = 0;
        while (true) {
            intento++;
            Registry registro = null;
            try {
                registro = LocateRegistry.getRegistry(host, puerto);
                Remote obj = registro.lookup(tag);
                if (tipo.isInstance(obj)) {
                    remoto = tipo.cast(obj);
                    out.println("Conectado a '" + tag + "' en " + host + ":" + puerto + ".");
                    return remoto;
                }
                out.println("El objeto publicado como '" + tag + "' no es un " + tipo.getSimpleName() + ".");
                pedirOtroTag(registro);
            } catch (NotBoundException e) {
                out.println("El servidor no tiene ningun objeto publicado como '" + tag + "'.");
                pedirOtroTag(registro);
            } catch (RemoteException e) {
                out.println("AVISO: no se puede contactar con el servidor en " + host + ":" + puerto + " ("
                        + causa(e) + "). Intento " + intento + "; se reintenta en " + esperaMs / 1000
                        + " s (Ctrl+C para salir).");
                esperar();
            }
        }
    }

    /**
     * Ejecuta una operacion sobre el objeto remoto. Si falla porque el
     * servidor no esta disponible, avisa, espera a que vuelva (obteniendo un
     * stub nuevo) y la repite.
     *
     * @param <R> tipo del resultado
     * @param descripcion descripcion de la operacion para los avisos
     * @param operacion operacion que invoca uno o varios metodos remotos
     * @return el resultado de la operacion
     */
    public <R> R ejecutar(String descripcion, Operacion<T, R> operacion) {
        while (true) {
            if (remoto == null) {
                conectar();
            }
            try {
                return operacion.ejecutar(remoto);
            } catch (RemoteException e) {
                out.println("AVISO: fallo la operacion '" + descripcion + "' (" + causa(e)
                        + "). El servidor no esta disponible; se esperara a que vuelva.");
                remoto = null;
                esperar();
            }
        }
    }

    /**
     * Hace una pregunta de si o no por consola.
     *
     * @param pregunta texto de la pregunta
     * @param porDefecto respuesta si el usuario pulsa Enter o no hay entrada
     * @return true si la respuesta es si
     */
    public boolean preguntarSiNo(String pregunta, boolean porDefecto) {
        String r = preguntar(pregunta + " (s/n)", porDefecto ? "s" : "n");
        return r.toLowerCase().startsWith("s");
    }

    /**
     * @return la direccion del servidor
     */
    public String getHost() {
        return host;
    }

    /**
     * @return el puerto del registro
     */
    public int getPuerto() {
        return puerto;
    }

    /**
     * @return el tag del objeto remoto
     */
    public String getTag() {
        return tag;
    }

    /**
     * Operacion sobre un objeto remoto, que puede lanzar RemoteException.
     *
     * @param <T> interfaz remota
     * @param <R> tipo del resultado
     */
    @FunctionalInterface
    public interface Operacion<T, R> {

        /**
         * Ejecuta la operacion.
         *
         * @param remoto stub del objeto remoto
         * @return el resultado
         * @throws RemoteException si falla la comunicacion con el servidor
         */
        R ejecutar(T remoto) throws RemoteException;
    }

    // -------------------------------------------------------------- Privados
    /**
     * Muestra los tags publicados (si el registro responde) y pide otro tag.
     * Si ya no queda entrada que leer, espera antes de reintentar con el
     * mismo tag, por si el servidor lo publica mas tarde.
     */
    private void pedirOtroTag(Registry registro) {
        if (registro != null) {
            try {
                out.println("Tags publicados: " + Arrays.toString(registro.list()));
            } catch (RemoteException e) {
                // El registro ha dejado de responder; se pide el tag igualmente
            }
        }
        if (entradaAgotada) {
            esperar();
            return;
        }
        tag = preguntar("Tag del objeto remoto (" + tipo.getSimpleName() + ")", tagPorDefecto);
    }

    /**
     * Escribe una pregunta y lee la respuesta; devuelve el valor por defecto
     * si la respuesta esta vacia o no queda entrada.
     */
    private String preguntar(String pregunta, String porDefecto) {
        out.print(pregunta + " [" + porDefecto + "]: ");
        out.flush();
        String linea = null;
        if (!entradaAgotada) {
            try {
                linea = in.readLine();
            } catch (IOException e) {
                linea = null;
            }
        }
        if (linea == null) {
            entradaAgotada = true;
            out.println();
        }
        return (linea == null || linea.isBlank()) ? porDefecto : linea.trim();
    }

    private void esperar() {
        try {
            Thread.sleep(esperaMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Causa mas concreta de una excepcion remota, para mostrarla al usuario.
     */
    private static String causa(RemoteException e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        String msg = t.getMessage();
        return t.getClass().getSimpleName() + (msg == null ? "" : ": " + msg);
    }

    /**
     * Limita el tiempo de establecimiento de conexiones y de espera de
     * respuestas de RMI. Se hace una sola vez por JVM.
     */
    private static synchronized void configurarTiempos() {
        if (tiemposConfigurados) {
            return;
        }
        tiemposConfigurados = true;
        if (System.getProperty("sun.rmi.transport.tcp.responseTimeout") == null) {
            System.setProperty("sun.rmi.transport.tcp.responseTimeout", TIMEOUT_RESPUESTA_MS);
        }
        try {
            RMISocketFactory.setSocketFactory(new RMISocketFactory() {
                @Override
                public Socket createSocket(String h, int p) throws IOException {
                    Socket s = new Socket();
                    s.connect(new InetSocketAddress(h, p), TIMEOUT_CONEXION_MS);
                    return s;
                }

                @Override
                public ServerSocket createServerSocket(int p) throws IOException {
                    return new ServerSocket(p);
                }
            });
        } catch (IOException | IllegalStateException e) {
            // Ya habia una factoria instalada: se mantiene la existente
        }
    }
}
