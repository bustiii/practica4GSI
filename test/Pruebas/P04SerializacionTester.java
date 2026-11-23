package Pruebas;

import GSILabs.BModel.Bar;
import GSILabs.BModel.Cliente;
import GSILabs.BModel.Contestacion;
import GSILabs.BModel.Direccion;
import GSILabs.BModel.Dueno;
import GSILabs.BModel.Local;
import GSILabs.BModel.Pub;
import GSILabs.BModel.Reserva;
import GSILabs.BModel.Restaurante;
import GSILabs.BModel.Review;
import GSILabs.BModel.Usuario;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Pruebas de la Practica 04 (issue #1): serializacion Java de las clases de
 * BModel que viajan por RMI. Cada objeto se escribe con
 * {@link ObjectOutputStream} y se vuelve a leer con {@link ObjectInputStream},
 * que es lo mismo que hace RMI con los argumentos y los valores devueltos.
 * <p>
 * La prueba de identificadores se ejecuta en una segunda JVM, porque los
 * generadores de identificadores son estaticos y en esta JVM ya estan
 * adelantados por los objetos creados en las demas pruebas.
 */
public class P04SerializacionTester {

    /**
     * Identificador alto con el que se crean los objetos que lee la segunda
     * JVM.
     */
    private static final int ID_ALTO = 1_000_000;

    private static int correctos = 0;
    private static int fallidos = 0;

    /**
     * Ejecuta todas las pruebas y muestra un resumen. Con los argumentos
     * {@code --hijo fichero} actua como la segunda JVM de la prueba de
     * identificadores.
     *
     * @param args vacio, o {@code --hijo fichero}
     * @throws Exception si falla algo fuera de las pruebas
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 2 && "--hijo".equals(args[0])) {
            System.exit(comprobarIdsEnHijo(new File(args[1])));
        }

        probarClasesSerializables();
        probarLocalConReviews();
        probarSubtiposDeLocal();
        probarUsuarios();
        probarContestacion();
        probarReserva();
        probarArrayDeLocales();
        probarCopiaIndependiente();
        probarIdsTrasDeserializar();

        System.out.println();
        System.out.println("RESUMEN: " + correctos + " correctos, " + fallidos + " fallidos (de "
                + (correctos + fallidos) + ")");
        if (fallidos > 0) {
            System.exit(1);
        }
    }

    // ---------------------------------------------------------------- Pruebas
    /**
     * Todas las clases que viajan por RMI son Serializable y declaran su
     * serialVersionUID.
     */
    private static void probarClasesSerializables() {
        List<Class<?>> clases = List.of(Local.class, Bar.class, Pub.class, Restaurante.class,
                Direccion.class, Review.class, Contestacion.class, Reserva.class, Usuario.class,
                Cliente.class, Dueno.class);
        for (Class<?> c : clases) {
            intentar("Z-" + c.getSimpleName(), c.getSimpleName()
                    + " es Serializable y declara serialVersionUID", () -> {
                if (!Serializable.class.isAssignableFrom(c)) {
                    return false;
                }
                Field f = c.getDeclaredField("serialVersionUID");
                int m = f.getModifiers();
                return Modifier.isStatic(m) && Modifier.isFinal(m) && f.getType() == long.class
                        && ObjectStreamClass.lookup(c) != null;
            });
        }
    }

    /**
     * Un local con reviews se serializa y deserializa conservando sus datos y
     * su lista de reviews.
     */
    private static void probarLocalConReviews() {
        Bar bar = new Bar("El Rincon", new Direccion("Pamplona", "Navarra", "Mayor", 3));
        bar.setDescripcion("Pintxos y vermut");
        bar.addEspecialidad("pintxos");
        bar.addEspecialidad("vermut");
        Cliente ana = new Cliente("anaSer", "secreta", LocalDate.of(2000, 5, 1));
        Cliente luis = new Cliente("luisSer", "secreta", LocalDate.of(1995, 2, 10));
        Review r1 = new Review(ana.getId(), bar.getId(), 4, "Muy bueno", LocalDate.now().minusDays(3));
        Review r2 = new Review(luis.getId(), bar.getId(), 2, "Lento", LocalDate.now().minusDays(1));
        bar.anadirReview(r1);
        bar.anadirReview(r2);

        intentar("L1", "un bar con dos reviews conserva id, nombre, direccion y descripcion", () -> {
            Bar b = (Bar) idaYVuelta(bar);
            return b != bar && b.getId() == bar.getId() && b.equals(bar)
                    && b.getNombre().equals(bar.getNombre())
                    && b.getUbicacion().equals(bar.getUbicacion())
                    && b.getDescripcion().equals(bar.getDescripcion())
                    && b.getEspecialidades().equals(bar.getEspecialidades());
        });
        intentar("L2", "las reviews del local llegan completas y en el mismo orden", () -> {
            Bar b = (Bar) idaYVuelta(bar);
            List<Review> rs = b.getReviews();
            return rs.size() == 2
                    && mismaReview(rs.get(0), r1) && mismaReview(rs.get(1), r2);
        });
        intentar("L3", "el local deserializado admite nuevas reviews", () -> {
            Bar b = (Bar) idaYVuelta(bar);
            b.anadirReview(new Review(ana.getId(), b.getId(), 5, "Repetiria", LocalDate.now()));
            return b.getReviews().size() == 3 && bar.getReviews().size() == 2;
        });
    }

    /**
     * Restaurantes y pubs mantienen su subtipo y sus atributos propios.
     */
    private static void probarSubtiposDeLocal() {
        Restaurante rest = new Restaurante(18.5, 4, 60, "Casa Pepe",
                new Direccion("Estella", "Navarra", "Ruiz", 1));
        intentar("T1", "un restaurante conserva precio del menu y capacidades", () -> {
            Local l = (Local) idaYVuelta(rest);
            if (!(l instanceof Restaurante)) {
                return false;
            }
            Restaurante r = (Restaurante) l;
            return r.equals(rest) && r.getPrecioMenu() == 18.5 && r.getCapacidadMesa() == 4
                    && r.getCapacidadTotal() == 60;
        });

        Pub pub = new Pub("La Noche", new Direccion("Tudela", "Navarra", "Ribera", 8),
                LocalTime.of(3, 0), LocalTime.of(22, 30));
        intentar("T2", "un pub conserva su horario", () -> {
            Local l = (Local) idaYVuelta(pub);
            return l instanceof Pub && l.equals(pub)
                    && ((Pub) l).getHoraApertura().equals(LocalTime.of(22, 30));
        });
    }

    /**
     * Clientes y duenos (con su conjunto de locales) se reconstruyen.
     */
    private static void probarUsuarios() {
        Cliente eva = new Cliente("evaSer", "clave", LocalDate.of(1999, 9, 9));
        intentar("U1", "un cliente conserva id, nick, fecha de nacimiento y perfil", () -> {
            Cliente c = (Cliente) idaYVuelta(eva);
            return c.getId() == eva.getId() && c.equals(eva)
                    && c.getFechaNacimiento().equals(eva.getFechaNacimiento())
                    && c.getPerfil() == Usuario.Perfil.CLIENTE;
        });

        Bar b1 = new Bar("Uno", new Direccion("Pamplona", "Navarra", "Calle A", 1));
        Bar b2 = new Bar("Dos", new Direccion("Pamplona", "Navarra", "Calle B", 2));
        Dueno pepe = new Dueno("pepeSer", "clave", LocalDate.of(1980, 1, 1));
        pepe.anadirLocal(b1);
        pepe.anadirLocal(b2);
        intentar("U2", "un dueno conserva su conjunto de locales", () -> {
            Dueno d = (Dueno) idaYVuelta(pepe);
            return d.equals(pepe) && d.getPerfil() == Usuario.Perfil.PROPIETARIO
                    && d.getLocales().size() == 2
                    && d.getLocales().contains(b1) && d.getLocales().contains(b2);
        });
    }

    /**
     * Una contestacion arrastra su review y su autor; si la review viaja en el
     * mismo flujo, se mantiene como un unico objeto compartido.
     */
    private static void probarContestacion() {
        Bar bar = new Bar("Contestado", new Direccion("Burlada", "Navarra", "Real", 4));
        Cliente cli = new Cliente("cliSer", "clave", LocalDate.of(2001, 1, 1));
        Dueno duo = new Dueno("duoSer", "clave", LocalDate.of(1975, 3, 3));
        duo.anadirLocal(bar);
        Review r = new Review(cli.getId(), bar.getId(), 3, "Normal", LocalDate.now().minusDays(2));
        Contestacion c = new Contestacion("Gracias por venir", r, duo);

        intentar("C1", "una contestacion conserva comentario, fecha, review y autor", () -> {
            Contestacion x = (Contestacion) idaYVuelta(c);
            return x.equals(c) && x.getComentario().equals(c.getComentario())
                    && x.getFecha().equals(c.getFecha())
                    && mismaReview(x.getReview(), r) && x.getAutor().equals(duo)
                    && x.getAutor().getLocales().contains(bar);
        });
        intentar("C2", "review y contestacion enviadas juntas comparten la misma review", () -> {
            Object[] par = (Object[]) idaYVuelta(new Object[]{r, c});
            return ((Contestacion) par[1]).getReview() == par[0];
        });
    }

    /**
     * Una reserva conserva su fecha, descuento, cliente y local.
     */
    private static void probarReserva() {
        Restaurante rest = new Restaurante(20, 4, 40, "Reservas",
                new Direccion("Pamplona", "Navarra", "Sancho", 7));
        Cliente cli = new Cliente("resSer", "clave", LocalDate.of(1990, 4, 4));
        LocalDateTime cuando = LocalDateTime.now().plusDays(5).withNano(0);
        Reserva res = new Reserva(cuando, 10.0, cli, rest);
        intentar("R1", "una reserva conserva id, fecha, descuento, cliente y local", () -> {
            Reserva x = (Reserva) idaYVuelta(res);
            return x.getId() == res.getId() && x.equals(res) && x.getFecha().equals(cuando)
                    && x.getDescuento().equals(10.0) && x.getCliente().equals(cli)
                    && x.getLocal() instanceof Restaurante && x.getLocal().equals(rest);
        });
    }

    /**
     * Los metodos remotos devuelven arrays de locales (y huecos a null).
     */
    private static void probarArrayDeLocales() {
        Local[] locales = {
            new Bar("Array1", new Direccion("Tafalla", "Navarra", "Plaza", 1)),
            new Restaurante(15, 2, 20, "Array2", new Direccion("Tafalla", "Navarra", "Plaza", 2)),
            null
        };
        intentar("A1", "un array de locales con huecos a null llega igual", () -> {
            Local[] x = (Local[]) idaYVuelta(locales);
            return x.length == 3 && x[0] instanceof Bar && x[0].equals(locales[0])
                    && x[1] instanceof Restaurante && x[1].equals(locales[1]) && x[2] == null;
        });
    }

    /**
     * Lo que llega por RMI es una copia: modificarla no altera el original.
     */
    private static void probarCopiaIndependiente() {
        Bar bar = new Bar("Original", new Direccion("Zizur", "Navarra", "Nueva", 5));
        intentar("I1", "modificar la copia deserializada no cambia el original", () -> {
            Bar copia = (Bar) idaYVuelta(bar);
            copia.setDescripcion("Cambiada en la copia");
            copia.anadirReview(new Review(1, copia.getId(), 5, "", LocalDate.now()));
            return !"Cambiada en la copia".equals(bar.getDescripcion()) && bar.getReviews().isEmpty();
        });
    }

    /**
     * En una JVM nueva (generadores a 1), deserializar objetos con un id alto
     * debe adelantar los generadores para que los objetos creados despues no
     * repitan identificadores.
     */
    private static void probarIdsTrasDeserializar() {
        intentar("D1", "tras deserializar, los nuevos objetos no repiten identificadores", () -> {
            Path f = Files.createTempFile("P04ids", ".ser");
            try {
                Bar bar = new Bar(ID_ALTO, "IdAlto", new Direccion("Olite", "Navarra", "Castillo", 1));
                Cliente cli = new Cliente(ID_ALTO, "idAltoSer", "clave", LocalDate.of(2000, 1, 1));
                Review r = new Review(ID_ALTO, cli.getId(), bar.getId(), 4, "", LocalDate.now(),
                        LocalDate.now());
                Reserva res = new Reserva(ID_ALTO, LocalDateTime.now().plusDays(1), 0.0, cli,
                        new Restaurante(ID_ALTO + 1, 10, 2, 10, "R", new Direccion("Olite", "Navarra", "Rua", 2)));
                bar.anadirReview(r);
                try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(f))) {
                    out.writeObject(bar);
                    out.writeObject(cli);
                    out.writeObject(res);
                }
                ProcessBuilder pb = new ProcessBuilder(
                        Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                        "-cp", System.getProperty("java.class.path"),
                        P04SerializacionTester.class.getName(), "--hijo", f.toString());
                pb.redirectErrorStream(true);
                Process p = pb.start();
                String salida = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                if (!p.waitFor(30, TimeUnit.SECONDS)) {
                    p.destroyForcibly();
                    return false;
                }
                System.out.println("        (segunda JVM) " + salida);
                return p.exitValue() == 0;
            } finally {
                Files.deleteIfExists(f);
            }
        });
    }

    /**
     * Parte de la prueba D1 que se ejecuta en la segunda JVM.
     *
     * @return 0 si los identificadores nuevos son mayores que los leidos
     */
    private static int comprobarIdsEnHijo(File f) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(f.toPath()))) {
            in.readObject();
            in.readObject();
            in.readObject();
        }
        Direccion d = new Direccion("Olite", "Navarra", "Otra", 9);
        int local = new Bar("Nuevo", d).getId();
        int usuario = new Cliente("nuevoSer", "clave", LocalDate.of(2000, 1, 1)).getId();
        int review = new Review(usuario, local, 3, "", LocalDate.now()).getId();
        int reserva = new Reserva(LocalDateTime.now().plusDays(2), 0.0, new Cliente("otroSer", "clave",
                LocalDate.of(2000, 1, 1)), new Restaurante(10, 2, 10, "N", d)).getId();
        System.out.println("ids nuevos: local=" + local + ", usuario=" + usuario + ", review=" + review
                + ", reserva=" + reserva + " (leidos hasta " + (ID_ALTO + 1) + ")");
        boolean ok = local > ID_ALTO + 1 && usuario > ID_ALTO && review > ID_ALTO && reserva > ID_ALTO;
        return ok ? 0 : 1;
    }

    // -------------------------------------------------------------- Utilidades
    /**
     * Serializa un objeto en memoria y devuelve la copia deserializada.
     */
    private static Object idaYVuelta(Object o) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(o);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return in.readObject();
        }
    }

    /**
     * Compara todos los campos de dos reviews (equals solo mira cliente, local
     * y fecha de visita).
     */
    private static boolean mismaReview(Review a, Review b) {
        return a.equals(b) && a.getId() == b.getId() && a.getValoracion() == b.getValoracion()
                && a.getComentario().equals(b.getComentario())
                && a.getFechaCreacion().equals(b.getFechaCreacion());
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
