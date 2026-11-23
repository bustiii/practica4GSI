package Pruebas;

import GSILabs.BModel.Bar;
import GSILabs.BModel.Cliente;
import GSILabs.BModel.Direccion;
import GSILabs.BModel.Local;
import GSILabs.BModel.Pub;
import GSILabs.BModel.Restaurante;
import GSILabs.BModel.Review;
import GSILabs.connect.AdminGateway;
import GSILabs.connect.ClientGateway;
import GSILabs.connect.LocalFinder;
import GSILabs.connect.PublicBusinessSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;

/**
 * Pruebas de la Practica 04 (issue #2): consultas de
 * {@link PublicBusinessSystem} ({@code getLocal}, {@code getLocals},
 * {@code mejorBar} y {@code mejoresRestaurantes}), invocadas en local, sin
 * RMI.
 * <p>
 * Datos de prueba (puntuacion media entre parentesis):
 * <ul>
 * <li>Pamplona, bares: Bar Txoko (4), Txoko Berri (4,5), Bar Cero (0) y Bar
 * Vacio (sin reviews); pub: Pub Txoko (5).</li>
 * <li>Pamplona, restaurantes: Asador Ribera (3), La Cocina (5), Restaurante
 * Cero (0) y Casa Pepe (sin reviews).</li>
 * <li>Tudela: restaurante Meson Tudela (5), sin bares.</li>
 * <li>Burlada: Bar Solitario (sin reviews).</li>
 * </ul>
 */
public class P04ConsultasTester {

    private static int correctos = 0;
    private static int fallidos = 0;

    private static PublicBusinessSystem sis;
    private static Bar txoko, berri, cero, vacio, solitario;
    private static Pub pub;
    private static Restaurante asador, cocina, restCero, pepe, meson;

    /**
     * Ejecuta todas las pruebas y muestra un resumen.
     *
     * @param args no se usan
     */
    public static void main(String[] args) {
        crearSistema();

        probarGetLocal();
        probarGetLocals();
        probarMejorBar();
        probarMejoresRestaurantes();
        probarAccesoPorInterfaces();

        System.out.println();
        System.out.println("RESUMEN: " + correctos + " correctos, " + fallidos + " fallidos (de "
                + (correctos + fallidos) + ")");
        if (fallidos > 0) {
            System.exit(1);
        }
    }

    // ---------------------------------------------------------------- Pruebas
    private static void probarGetLocal() {
        intentar("G1", "getLocal encuentra un local por su nombre exacto",
                () -> sis.getLocal("Bar Txoko") == txoko);
        intentar("G2", "getLocal ignora mayusculas y espacios",
                () -> sis.getLocal("  bar   TXOKO ") == txoko && sis.getLocal("bartxoko") == txoko);
        intentar("G3", "getLocal no acepta coincidencias parciales",
                () -> sis.getLocal("Txoko") == null);
        intentar("G4", "getLocal devuelve null si no existe o el nombre es nulo",
                () -> sis.getLocal("No existe") == null && sis.getLocal(null) == null);
    }

    private static void probarGetLocals() {
        intentar("L1", "getLocals devuelve las coincidencias parciales en orden de alta",
                () -> Arrays.equals(sis.getLocals("txoko"), new Local[]{txoko, berri, pub}));
        intentar("L2", "getLocals ignora mayusculas y espacios",
                () -> Arrays.equals(sis.getLocals(" T X O K O "), new Local[]{txoko, berri, pub}));
        intentar("L3", "getLocals con coincidencia total devuelve ese local",
                () -> Arrays.equals(sis.getLocals("La Cocina"), new Local[]{cocina}));
        intentar("L4", "getLocals sin coincidencias o con nombre nulo devuelve un array vacio", () -> {
            Local[] a = sis.getLocals("zzz");
            Local[] b = sis.getLocals(null);
            return a != null && a.length == 0 && b != null && b.length == 0;
        });
        intentar("L5", "getLocals con texto vacio devuelve todos los locales",
                () -> sis.getLocals("").length == 11 && sis.getLocals("   ").length == 11);
    }

    private static void probarMejorBar() {
        intentar("M1", "mejorBar elige el bar con mejor media (un pub con mas nota no cuenta)",
                () -> sis.mejorBar("Pamplona") == berri);
        intentar("M2", "mejorBar ignora mayusculas y espacios en la ciudad",
                () -> sis.mejorBar("  pamPLONA ") == berri);
        intentar("M3", "mejorBar devuelve null si la ciudad no tiene bares o es nula",
                () -> sis.mejorBar("Tudela") == null && sis.mejorBar("Ninguna") == null
                && sis.mejorBar(null) == null);
        intentar("M4", "mejorBar devuelve un bar sin reviews si es el unico de la ciudad",
                () -> sis.mejorBar("Burlada") == solitario);
    }

    private static void probarMejoresRestaurantes() {
        intentar("R1", "mejoresRestaurantes devuelve los mejores en orden",
                () -> Arrays.equals(sis.mejoresRestaurantes("Pamplona", 2),
                        new Restaurante[]{cocina, asador}));
        intentar("R2", "rellena con null y pone los restaurantes sin reviews al final",
                () -> Arrays.equals(sis.mejoresRestaurantes("Pamplona", 6),
                        new Restaurante[]{cocina, asador, restCero, pepe, null, null}));
        intentar("R3", "solo cuenta los restaurantes de la ciudad pedida",
                () -> Arrays.equals(sis.mejoresRestaurantes("tudela", 2), new Restaurante[]{meson, null}));
        intentar("R4", "ciudad desconocida o nula: array de num posiciones a null",
                () -> Arrays.equals(sis.mejoresRestaurantes("Ninguna", 3), new Restaurante[3])
                && Arrays.equals(sis.mejoresRestaurantes(null, 2), new Restaurante[2]));
        intentar("R5", "num nulo, cero o negativo: array vacio",
                () -> sis.mejoresRestaurantes("Pamplona", null).length == 0
                && sis.mejoresRestaurantes("Pamplona", 0).length == 0
                && sis.mejoresRestaurantes("Pamplona", -4).length == 0);
    }

    /**
     * Las consultas se usan a traves de las interfaces remotas, como haran los
     * clientes, y sus resultados se pueden serializar.
     */
    private static void probarAccesoPorInterfaces() {
        LocalFinder finder = sis;
        ClientGateway cliente = sis;
        AdminGateway admin = sis;
        intentar("I1", "las consultas funcionan a traves de LocalFinder, ClientGateway y AdminGateway",
                () -> finder.getLocal("la cocina") == cocina && admin.getLocals("Cero").length == 2
                && cliente.mejorBar("Pamplona") == berri
                && cliente.mejoresRestaurantes("Pamplona", 1)[0] == cocina);
        intentar("I2", "los resultados (con huecos a null) se pueden serializar", () -> {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
                out.writeObject(sis.mejoresRestaurantes("Pamplona", 5));
                out.writeObject(sis.getLocals("txoko"));
            }
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                Restaurante[] r = (Restaurante[]) in.readObject();
                Local[] l = (Local[]) in.readObject();
                return r.length == 5 && r[0].equals(cocina) && r[4] == null
                        && l.length == 3 && l[1].equals(berri) && l[0].getReviews().size() == 2;
            }
        });
    }

    // ------------------------------------------------------------------ Datos
    private static void crearSistema() {
        sis = new PublicBusinessSystem();
        Cliente ana = new Cliente("anaCon", "pass", LocalDate.of(2000, 1, 1));
        Cliente iker = new Cliente("ikerCon", "pass", LocalDate.of(1998, 7, 2));
        Cliente eva = new Cliente("evaCon", "pass", LocalDate.of(1990, 3, 3));
        sis.nuevoUsuario(ana);
        sis.nuevoUsuario(iker);
        sis.nuevoUsuario(eva);

        txoko = new Bar("Bar Txoko", dir("Pamplona", "San Nicolas", 4));
        berri = new Bar("Txoko Berri", dir("Pamplona", "Estafeta", 20));
        cero = new Bar("Bar Cero", dir("Pamplona", "Mayor", 1));
        vacio = new Bar("Bar Vacio", dir("Pamplona", "Mayor", 2));
        pub = new Pub("Pub Txoko", dir("Pamplona", "Jarauta", 30), LocalTime.of(4, 0), LocalTime.of(23, 0));
        asador = new Restaurante(22.0, 6, 80, "Asador Ribera", dir("Pamplona", "Navarreria", 10));
        cocina = new Restaurante(30.0, 4, 40, "La Cocina", dir("Pamplona", "Chapitela", 5));
        restCero = new Restaurante(12.0, 4, 20, "Restaurante Cero", dir("Pamplona", "Zapateria", 7));
        pepe = new Restaurante(15.0, 4, 30, "Casa Pepe", dir("Pamplona", "Calderería", 3));
        meson = new Restaurante(25.0, 8, 60, "Meson Tudela", new Direccion("Tudela", "Navarra", "Herrerias", 2));
        solitario = new Bar("Bar Solitario", new Direccion("Burlada", "Navarra", "Real", 9));
        for (Local l : new Local[]{txoko, berri, cero, vacio, pub, asador, cocina, restCero, pepe, meson, solitario}) {
            if (!sis.nuevoLocal(l)) {
                throw new IllegalStateException("No se pudo dar de alta " + l.getNombre());
            }
        }

        resena(ana, txoko, 5);
        resena(iker, txoko, 3);
        resena(ana, berri, 5);
        resena(eva, berri, 4);
        resena(iker, cero, 0);
        resena(eva, pub, 5);
        resena(ana, asador, 3);
        resena(iker, cocina, 5);
        resena(eva, restCero, 0);
        resena(ana, meson, 5);
    }

    private static Direccion dir(String ciudad, String calle, int numero) {
        return new Direccion(ciudad, "Navarra", calle, numero);
    }

    private static void resena(Cliente c, Local l, int puntuacion) {
        Review r = new Review(c.getId(), l.getId(), puntuacion, "", LocalDate.now().minusDays(1));
        if (!sis.nuevaReview(r)) {
            throw new IllegalStateException("No se pudo crear la review de " + l.getNombre());
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
