package GSILabs.connect;

import GSILabs.BModel.Local;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Servidor RMI del sistema LeisureOffice (Practica 4, Ejercicio 2).
 * <p>
 * Crea un {@link PublicBusinessSystem}, lo puebla (con
 * {@link DatosIniciales} o con un fichero XML de la Practica 3), lo exporta
 * con {@link UnicastRemoteObject#exportObject(Remote, int)} y publica el stub
 * en un registro RMI creado con {@link LocateRegistry#createRegistry(int)}
 * bajo dos identificadores: {@value #TAG_CLIENTE} y {@value #TAG_ADMIN}.
 * <p>
 * El objeto remoto se exporta en el mismo puerto que el registro (1099 por
 * defecto), de modo que en un cortafuegos basta con abrir ese puerto. Para
 * que las maquinas remotas puedan contactar con el objeto, el stub tiene que
 * llevar una direccion alcanzable: se toma de la opcion {@code --host}, de la
 * propiedad {@code java.rmi.server.hostname} si se ha fijado con {@code -D}, o
 * de la primera direccion IPv4 de red local que se encuentre.
 * <p>
 * Uso:
 * <pre>
 * java GSILabs.connect.BusinessServer [--host IP] [--puerto N] [--xml fichero]
 * </pre>
 * Una vez arrancado, la consola admite las ordenes {@code estado},
 * {@code ayuda} y {@code salir}.
 */
public final class BusinessServer {

    /**
     * Puerto por defecto del registro RMI.
     */
    public static final int PUERTO_POR_DEFECTO = 1099;
    /**
     * Identificador con el que se publica la pasarela de los clientes.
     */
    public static final String TAG_CLIENTE = "ClientGateway";
    /**
     * Identificador con el que se publica la pasarela de los administradores.
     */
    public static final String TAG_ADMIN = "AdminGateway";
    /**
     * Mensaje que indica que el servidor esta listo (lo buscan las pruebas).
     */
    public static final String MENSAJE_LISTO = "Servidor listo";

    private BusinessServer() {
    }

    /**
     * Arranca el servidor.
     *
     * @param args opciones {@code --host IP}, {@code --puerto N} y
     * {@code --xml fichero}, todas opcionales
     */
    public static void main(String[] args) {
        String host = null;
        int puerto = PUERTO_POR_DEFECTO;
        File xml = null;
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--host" -> host = args[++i];
                    case "--puerto" -> puerto = Integer.parseInt(args[++i]);
                    case "--xml" -> xml = new File(args[++i]);
                    default -> throw new IllegalArgumentException("Opcion desconocida: " + args[i]);
                }
            }
            if (puerto < 1 || puerto > 65535) {
                throw new IllegalArgumentException("El puerto debe estar entre 1 y 65535.");
            }
        } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
            System.err.println("Argumentos incorrectos: " + (e.getMessage() == null ? "falta un valor" : e.getMessage()));
            System.err.println("Uso: java GSILabs.connect.BusinessServer [--host IP] [--puerto N] [--xml fichero]");
            System.exit(2);
            return;
        }

        // 0. Direccion que llevara el stub (antes de exportar el objeto)
        host = fijarHostname(host);

        // 1. Instancia del objeto a publicar, poblada
        PublicBusinessSystem sistema = new PublicBusinessSystem();
        if (xml != null) {
            if (!sistema.loadXMLFile(xml)) {
                System.err.println("No se pudo cargar el fichero " + xml + "; el servidor no arranca.");
                System.exit(1);
                return;
            }
            System.out.println("Datos cargados desde " + xml);
        } else {
            DatosIniciales.poblar(sistema);
            System.out.println("Datos de ejemplo cargados (ver DatosIniciales).");
        }

        // 2-4. Registro, stub y asociacion a los dos identificadores
        Registry registro;
        try {
            registro = publicar(sistema, puerto);
        } catch (RemoteException e) {
            System.err.println("No se pudo publicar el servidor en el puerto " + puerto + ": " + e.getMessage());
            System.err.println("Compruebe que el puerto no esta ocupado (por ejemplo, por otro servidor o un rmiregistry).");
            System.exit(1);
            return;
        }

        System.out.println(MENSAJE_LISTO + ".");
        System.out.println("  Direccion: " + host + " (java.rmi.server.hostname)");
        System.out.println("  Puerto:    " + puerto + " (registro y objeto remoto)");
        System.out.println("  Tags:      " + TAG_CLIENTE + ", " + TAG_ADMIN);
        System.out.println("Escriba 'estado', 'ayuda' o 'salir'.");

        consola(sistema, registro);
    }

    /**
     * Crea un registro RMI en el puerto indicado, exporta el sistema en ese
     * mismo puerto y asocia su stub a {@value #TAG_CLIENTE} y
     * {@value #TAG_ADMIN}.
     * <p>
     * Hay que fijar {@code java.rmi.server.hostname} antes de llamar a este
     * metodo si el servidor debe ser accesible desde otras maquinas.
     *
     * @param sistema sistema a publicar
     * @param puerto puerto del registro y del objeto remoto
     * @return el registro creado (hay que conservarlo mientras dure el
     * servidor)
     * @throws RemoteException si no se puede crear el registro (por ejemplo,
     * porque el puerto esta ocupado) o exportar el objeto
     */
    public static Registry publicar(PublicBusinessSystem sistema, int puerto) throws RemoteException {
        Registry registro = LocateRegistry.createRegistry(puerto);
        try {
            Remote stub = UnicastRemoteObject.exportObject(sistema, puerto);
            registro.rebind(TAG_CLIENTE, stub);
            registro.rebind(TAG_ADMIN, stub);
            return registro;
        } catch (RemoteException e) {
            UnicastRemoteObject.unexportObject(registro, true);
            throw e;
        }
    }

    /**
     * Retira los dos identificadores del registro y deja de exportar el
     * sistema y el registro, de modo que los clientes dejan de poder
     * contactar con el servidor.
     *
     * @param sistema sistema publicado
     * @param registro registro devuelto por
     * {@link #publicar(PublicBusinessSystem, int)}
     */
    public static void retirar(PublicBusinessSystem sistema, Registry registro) {
        for (String tag : new String[]{TAG_CLIENTE, TAG_ADMIN}) {
            try {
                registro.unbind(tag);
            } catch (RemoteException | NotBoundException e) {
                // Ya no estaba publicado: no hay nada que retirar
            }
        }
        try {
            UnicastRemoteObject.unexportObject(sistema, true);
        } catch (NoSuchObjectException e) {
            // Ya no estaba exportado
        }
        try {
            UnicastRemoteObject.unexportObject(registro, true);
        } catch (NoSuchObjectException e) {
            // Ya no estaba exportado
        }
    }

    /**
     * Fija la propiedad {@code java.rmi.server.hostname}, que determina la
     * direccion que llevan los stubs. Si se ha indicado {@code --host} se usa
     * esa; si la propiedad ya venia fijada con {@code -D} se respeta; y si no,
     * se busca una direccion IPv4 de red local.
     *
     * @param host direccion indicada con {@code --host}, o null
     * @return la direccion fijada
     */
    private static String fijarHostname(String host) {
        if (host != null) {
            System.setProperty("java.rmi.server.hostname", host);
            return host;
        }
        String propiedad = System.getProperty("java.rmi.server.hostname");
        if (propiedad != null && !propiedad.isBlank()) {
            return propiedad;
        }
        List<String> candidatas = direccionesLocales();
        String elegida;
        if (candidatas.isEmpty()) {
            try {
                elegida = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                elegida = "127.0.0.1";
            }
            System.out.println("Aviso: no se encontro ninguna direccion de red local; se usa " + elegida
                    + ". Indique la correcta con --host si los clientes estan en otras maquinas.");
        } else {
            elegida = candidatas.get(0);
            if (candidatas.size() > 1) {
                System.out.println("Direcciones disponibles: " + candidatas + ". Se usa " + elegida
                        + "; indique otra con --host si hace falta.");
            }
        }
        System.setProperty("java.rmi.server.hostname", elegida);
        return elegida;
    }

    /**
     * Prefijos de nombre de interfaces virtuales (Docker, maquinas virtuales,
     * puentes...) cuyas direcciones no son alcanzables desde otras maquinas.
     */
    private static final String[] INTERFACES_VIRTUALES = {"docker", "br-", "veth", "virbr", "vmnet",
        "vboxnet", "lxc", "lxd", "tun", "tap", "wg", "zt", "tailscale"};

    /**
     * Direcciones IPv4 de las interfaces de red activas que no son de bucle
     * local ni virtuales, ordenadas por preferencia: 192.168.x.x, 10.x.x.x,
     * 172.16-31.x.x y el resto.
     */
    private static List<String> direccionesLocales() {
        List<String> res = new ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual() || esVirtual(ni.getName())) {
                    continue;
                }
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address && !a.isLoopbackAddress() && !a.isLinkLocalAddress()) {
                        res.add(a.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            // Sin acceso a las interfaces: se devuelve lo encontrado hasta ahora
        }
        res.sort(Comparator.comparingInt(BusinessServer::preferencia));
        return res;
    }

    private static boolean esVirtual(String nombre) {
        String n = nombre.toLowerCase(Locale.ROOT);
        for (String prefijo : INTERFACES_VIRTUALES) {
            if (n.startsWith(prefijo)) {
                return true;
            }
        }
        return false;
    }

    private static int preferencia(String ip) {
        if (ip.startsWith("192.168.")) {
            return 0;
        }
        if (ip.startsWith("10.")) {
            return 1;
        }
        if (ip.matches("172\\.(1[6-9]|2\\d|3[01])\\..*")) {
            return 2;
        }
        return 3;
    }

    /**
     * Atiende las ordenes de la consola del servidor hasta recibir
     * {@code salir}. Si la entrada estandar se cierra (por ejemplo, al lanzar
     * el servidor en segundo plano), el servidor sigue funcionando.
     */
    private static void consola(PublicBusinessSystem sistema, Registry registro) {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        try {
            String linea;
            while ((linea = in.readLine()) != null) {
                switch (linea.trim().toLowerCase(Locale.ROOT)) {
                    case "" -> {
                    }
                    case "estado" -> mostrarEstado(sistema);
                    case "ayuda" -> System.out.println("Ordenes: estado (locales y reviews), ayuda, salir.");
                    case "salir" -> {
                        retirar(sistema, registro);
                        System.out.println("Servidor detenido.");
                        System.exit(0);
                    }
                    default -> System.out.println("Orden desconocida: " + linea.trim() + ". Escriba 'ayuda'.");
                }
            }
        } catch (IOException e) {
            System.err.println("Error leyendo la consola: " + e.getMessage());
        }
        System.out.println("Consola cerrada; el servidor sigue atendiendo peticiones.");
    }

    /**
     * Muestra los locales del sistema con su numero de reviews y su
     * puntuacion media; sirve para ver el efecto de las operaciones remotas.
     */
    private static void mostrarEstado(PublicBusinessSystem sistema) {
        Local[] locales = sistema.getLocals("");
        System.out.println(locales.length + " locales:");
        for (Local l : locales) {
            int reviews = sistema.verReviews(l).length;
            System.out.printf(Locale.ROOT, "  [%2d] %-22s %-12s %-10s %2d reviews, media %.2f%n", l.getId(),
                    l.getNombre(), l.getClass().getSimpleName(), l.getUbicacion().getLocalidad(), reviews,
                    sistema.obtenerValoracionMedia(l));
        }
    }
}
