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
import GSILabs.BSystem.BusinessSystem;
import GSILabs.persistence.XMLParser;
import GSILabs.persistence.XMLParsingException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Pruebas de la Practica 03: serializacion en XML de las clases del modelo y
 * del sistema, lectura desde XML (con {@link XMLParser}) y gestion de XML
 * inadecuados mediante {@link XMLParsingException}.
 * <p>
 * Los ficheros se crean en un directorio temporal, de modo que las pruebas no
 * dependen de rutas absolutas.
 */
public class P03Tester {

    private static int correctos = 0;
    private static int fallidos = 0;

    /**
     * Ejecuta todas las pruebas y muestra un resumen.
     *
     * @param args no se usan
     * @throws IOException si no se puede crear el directorio temporal
     */
    public static void main(String[] args) throws IOException {
        Path dir = Files.createTempDirectory("P03Tester");

        probarIdaYVuelta();
        probarFicheroIndividual(dir);
        probarOrdenLibre();
        probarCaracteresEspeciales();
        probarXMLInadecuados();
        probarContestacionYReserva();
        probarSistemaCompleto(dir);
        probarSistemaInconsistente(dir);

        System.out.println();
        System.out.println("RESUMEN: " + correctos + " correctos, " + fallidos + " fallidos (de "
                + (correctos + fallidos) + ")");
    }

    // ---------------------------------------------------------------- Pruebas
    /**
     * Cada objeto se convierte a XML y se vuelve a crear desde ese XML.
     */
    private static void probarIdaYVuelta() {
        Direccion d = new Direccion("Pamplona", "Navarra", "Estafeta", 12);
        intentar("X1", "una direccion se reconstruye desde su XML",
                () -> XMLParser.parseDireccion(d.toXML()).equals(d));

        Cliente ana = new Cliente("ana", "secreta", LocalDate.of(2000, 5, 1));
        intentar("X2", "un cliente se reconstruye con el mismo id, nick y fecha", () -> {
            Cliente c = XMLParser.parseCliente(ana.toXML());
            return c.getId() == ana.getId() && c.equals(ana)
                    && c.getFechaNacimiento().equals(ana.getFechaNacimiento());
        });

        Bar bar = new Bar("El Rincon", new Direccion("Pamplona", "Navarra", "Mayor", 3));
        bar.setDescripcion("Pintxos y vermut");
        bar.addEspecialidad("pintxos");
        bar.addEspecialidad("vermut");
        intentar("X3", "un bar conserva id, descripcion y especialidades", () -> {
            Bar b = XMLParser.parseBar(bar.toXML());
            return b.getId() == bar.getId() && b.equals(bar)
                    && "Pintxos y vermut".equals(b.getDescripcion());
        });

        Pub pub = new Pub("La Noche", new Direccion("Tudela", "Navarra", "Ribera", 8),
                LocalTime.of(3, 0), LocalTime.of(22, 30));
        intentar("X4", "un pub conserva su horario", () -> XMLParser.parsePub(pub.toXML()).equals(pub));

        Restaurante rest = new Restaurante(18.5, 4, 60, "Casa Pepe",
                new Direccion("Estella", "Navarra", "Ruiz", 1));
        intentar("X5", "un restaurante conserva menu y capacidades",
                () -> XMLParser.parseRestaurante(rest.toXML()).equals(rest));

        intentar("X6", "parseLocal devuelve el subtipo indicado por la etiqueta",
                () -> XMLParser.parseLocal(pub.toXML()) instanceof Pub
                && XMLParser.parseLocal(rest.toXML()) instanceof Restaurante);

        Review r = new Review(ana.getId(), bar.getId(), 4, "Muy bueno", LocalDate.now().minusDays(3));
        intentar("X7", "una review conserva id, referencias y fechas", () -> {
            Review p = XMLParser.parseReview(r.toXML());
            return p.getId() == r.getId() && p.equals(r) && p.getValoracion() == 4
                    && p.getFechaCreacion().equals(r.getFechaCreacion());
        });

        Dueno pepe = new Dueno("pepe", "clave", LocalDate.of(1980, 1, 1));
        pepe.anadirLocal(bar);
        intentar("X8", "el XML del dueno referencia sus locales por id", () -> {
            List<Integer> refs = XMLParser.referenciasLocales(XMLParser.raiz(pepe.toXML()));
            Usuario u = XMLParser.parseUsuario(pepe.toXML());
            return refs.equals(List.of(bar.getId())) && u instanceof Dueno && u.equals(pepe);
        });
    }

    /**
     * Un objeto se guarda en un fichero y se lee desde ese fichero.
     */
    private static void probarFicheroIndividual(Path dir) {
        Bar bar = new Bar("Fichero", new Direccion("Burlada", "Navarra", "Real", 2));
        File f = dir.resolve("bar.xml").toFile();
        intentar("F1", "saveToXML(File) + parseBar(File) reconstruye el bar",
                () -> bar.saveToXML(f) && XMLParser.parseBar(f).equals(bar));
        String ruta = dir.resolve("bar2.xml").toString();
        intentar("F2", "saveToXML(String) genera un fichero legible",
                () -> bar.saveToXML(ruta) && XMLParser.parseBar(new File(ruta)).equals(bar));
    }

    /**
     * Sin DTD no se garantiza el orden de los campos: deben aceptarse en
     * cualquier orden.
     */
    private static void probarOrdenLibre() {
        String desordenado = "<cliente id=\"900\">"
                + "<fechaNacimiento>1999-12-31</fechaNacimiento>"
                + "<contrasena>x</contrasena>"
                + "<nick>desordenado</nick>"
                + "</cliente>";
        intentar("O1", "los campos de un cliente se aceptan en cualquier orden",
                () -> XMLParser.parseCliente(desordenado).getNick().equals("desordenado"));

        String direccion = "<direccion><numero>5</numero><calle>Nueva</calle>"
                + "<provincia>Navarra</provincia><localidad>Olite</localidad></direccion>";
        String bar = "<bar id=\"901\"><especialidades><especialidad>cafe</especialidad></especialidades>"
                + direccion + "<nombre>Al reves</nombre></bar>";
        intentar("O2", "los campos de un bar se aceptan en cualquier orden",
                () -> XMLParser.parseBar(bar).getEspecialidades().contains("cafe"));

        String review = "<review local=\"3\" id=\"902\" cliente=\"7\"><fechaCreacion>2025-01-02</fechaCreacion>"
                + "<valoracion>2</valoracion><fechaVisita>2025-01-01</fechaVisita></review>";
        intentar("O3", "atributos en otro orden y comentario opcional ausente",
                () -> XMLParser.parseReview(review).getComentario().isEmpty());
    }

    /**
     * Los caracteres reservados de XML deben escaparse al escribir.
     */
    private static void probarCaracteresEspeciales() {
        Bar bar = new Bar("Tom & Jerry's <bar>", new Direccion("Pamplona", "Navarra", "Calle \"A\"", 1));
        intentar("E1", "los caracteres especiales se escapan y se recuperan",
                () -> XMLParser.parseBar(bar.toXML()).getNombre().equals("Tom & Jerry's <bar>"));
    }

    /**
     * Cualquier XML inadecuado debe provocar una XMLParsingException.
     */
    private static void probarXMLInadecuados() {
        esperarExcepcion("I1", "XML mal formado", () -> XMLParser.parseCliente("<cliente id=\"1\"><nick>ana"));
        esperarExcepcion("I2", "XML vacio", () -> XMLParser.parseCliente(""));
        esperarExcepcion("I3", "etiqueta raiz incorrecta",
                () -> XMLParser.parseBar(new Direccion("a", "b", "c", 1).toXML()));
        esperarExcepcion("I4", "falta un campo obligatorio",
                () -> XMLParser.parseCliente("<cliente id=\"1\"><nick>ana</nick><contrasena>x</contrasena></cliente>"));
        esperarExcepcion("I5", "falta el atributo id",
                () -> XMLParser.parseCliente("<cliente><nick>ana</nick><contrasena>x</contrasena>"
                        + "<fechaNacimiento>2000-01-01</fechaNacimiento></cliente>"));
        esperarExcepcion("I6", "fecha con formato incorrecto",
                () -> XMLParser.parseCliente("<cliente id=\"1\"><nick>ana</nick><contrasena>x</contrasena>"
                        + "<fechaNacimiento>01/01/2000</fechaNacimiento></cliente>"));
        esperarExcepcion("I7", "regla de negocio violada (usuario menor de 14 anios)",
                () -> XMLParser.parseCliente("<cliente id=\"1\"><nick>nino</nick><contrasena>x</contrasena>"
                        + "<fechaNacimiento>" + LocalDate.now().minusYears(5) + "</fechaNacimiento></cliente>"));
        esperarExcepcion("I8", "valoracion fuera de rango",
                () -> XMLParser.parseReview("<review id=\"1\" cliente=\"1\" local=\"1\"><valoracion>9</valoracion>"
                        + "<fechaVisita>2025-01-01</fechaVisita><fechaCreacion>2025-01-01</fechaCreacion></review>"));
        esperarExcepcion("I9", "campo repetido",
                () -> XMLParser.parseDireccion("<direccion><localidad>a</localidad><localidad>b</localidad>"
                        + "<provincia>p</provincia><calle>c</calle><numero>1</numero></direccion>"));
        esperarExcepcion("I10", "fichero inexistente",
                () -> XMLParser.parseBar(new File("no_existe_" + System.nanoTime() + ".xml")));
        esperarExcepcion("I11", "DTD prohibido (posibles entidades externas)",
                () -> XMLParser.parseCliente("<!DOCTYPE cliente [<!ENTITY x \"y\">]><cliente id=\"1\"/>"));
    }

    /**
     * Contestaciones y reservas referencian objetos que solo existen dentro de
     * un sistema, por lo que se crean resolviendo las referencias en el.
     */
    private static void probarContestacionYReserva() {
        BusinessSystem sis = new BusinessSystem();
        Cliente c = new Cliente("cliCR", "x", LocalDate.of(1995, 1, 1));
        Dueno d = new Dueno("dueCR", "x", LocalDate.of(1975, 1, 1));
        Bar b = new Bar("Bar CR", new Direccion("Ayegui", "Navarra", "Plaza", 1));
        sis.nuevoUsuario(c);
        sis.nuevoUsuario(d);
        sis.nuevoLocal(b);
        sis.asociarLocal(b, d);
        Review r = new Review(c.getId(), b.getId(), 5, "Genial", LocalDate.now().minusDays(1));
        sis.nuevaReview(r);
        Contestacion con = new Contestacion("Gracias", r, d);
        sis.nuevaReserva(c, b, LocalDate.now().plusDays(2), LocalTime.of(14, 0));
        Reserva res = sis.obtenerReservas(c)[0];

        intentar("C1", "una contestacion se reconstruye resolviendo review y autor", () -> {
            Contestacion p = XMLParser.parseContestacion(con.toXML(), sis);
            return p.equals(con) && p.getAutor() == d && p.getComentario().equals("Gracias");
        });
        intentar("C2", "una reserva se reconstruye resolviendo cliente y local", () -> {
            Reserva p = XMLParser.parseReserva(res.toXML(), sis);
            return p.getId() == res.getId() && p.equals(res);
        });
        esperarExcepcion("C3", "contestacion sobre un sistema que no tiene la review",
                () -> XMLParser.parseContestacion(con.toXML(), new BusinessSystem()));
        esperarExcepcion("C4", "reserva sin sistema donde resolver referencias",
                () -> XMLParser.parseReserva(res.toXML(), null));
    }

    /**
     * El sistema completo se guarda en un fichero y se vuelve a leer.
     */
    private static void probarSistemaCompleto(Path dir) {
        BusinessSystem sis = crearSistema();
        File f = dir.resolve("sistema.xml").toFile();

        intentar("S1", "saveToXML del sistema genera el fichero", () -> sis.saveToXML(f) && f.length() > 0);

        intentar("S2", "parseXMLFile reconstruye usuarios, locales y propietarios", () -> {
            BusinessSystem leido = BusinessSystem.parseXMLFile(f);
            Local bar = leido.obtenerLocal(new Direccion("Pamplona", "Navarra", "San Nicolas", 4));
            return leido.existeNick("lucia") && leido.obtenerUsuario("marta") instanceof Dueno
                    && bar instanceof Bar && leido.obtenerPropietarios(bar).length == 2;
        });

        intentar("S3", "parseXMLFile reconstruye reviews, contestaciones y reservas", () -> {
            BusinessSystem leido = BusinessSystem.parseXMLFile(f);
            Local bar = leido.obtenerLocal(new Direccion("Pamplona", "Navarra", "San Nicolas", 4));
            Review[] reviews = leido.verReviews(bar);
            Cliente lucia = (Cliente) leido.obtenerUsuario("lucia");
            return reviews.length == 2 && leido.tieneContestacion(reviews[0])
                    && leido.obtenerReservas(lucia).length == 1
                    && leido.obtenerValoracionMedia(bar) == sis.obtenerValoracionMedia(
                            sis.obtenerLocal(bar.getUbicacion()));
        });

        intentar("S4", "guardar lo leido produce exactamente el mismo XML", () -> {
            BusinessSystem leido = BusinessSystem.parseXMLFile(f);
            return leido.toXML().equals(sis.toXML());
        });

        BusinessSystem otro = new BusinessSystem();
        intentar("S5", "loadXMLFile carga el contenido en un sistema existente",
                () -> otro.loadXMLFile(f) && otro.existeNick("lucia") && otro.toXML().equals(sis.toXML()));

        File malo = dir.resolve("malo.xml").toFile();
        intentar("S6", "loadXMLFile devuelve false con un fichero invalido y no modifica el sistema", () -> {
            Files.writeString(malo.toPath(), "<businessSystem><usuarios>", StandardCharsets.UTF_8);
            String antes = otro.toXML();
            return !otro.loadXMLFile(malo) && otro.toXML().equals(antes);
        });

        intentar("S7", "un sistema vacio se guarda y se lee",
                () -> BusinessSystem.parseXML(new BusinessSystem().toXML()).toXML()
                        .equals(new BusinessSystem().toXML()));
    }

    /**
     * Un XML con referencias rotas o que viola reglas de negocio no debe
     * aceptarse como sistema.
     */
    private static void probarSistemaInconsistente(Path dir) {
        Cliente c = new Cliente("huerfano", "x", LocalDate.of(1990, 1, 1));
        String reviewHuerfana = "<businessSystem><usuarios>" + c.toXML() + "</usuarios>"
                + "<reviews><review id=\"5000\" cliente=\"" + c.getId() + "\" local=\"99999\">"
                + "<valoracion>3</valoracion><fechaVisita>2025-01-01</fechaVisita>"
                + "<fechaCreacion>2025-01-01</fechaCreacion></review></reviews></businessSystem>";
        esperarExcepcion("N1", "review que referencia un local inexistente",
                () -> BusinessSystem.parseXML(reviewHuerfana));

        String nickRepetido = "<businessSystem><usuarios>" + c.toXML()
                + c.toXML().replace("id=\"" + c.getId() + "\"", "id=\"7777\"") + "</usuarios></businessSystem>";
        esperarExcepcion("N2", "dos usuarios con el mismo nick", () -> BusinessSystem.parseXML(nickRepetido));

        Bar b = new Bar("Bar", new Direccion("Lodosa", "Navarra", "Mayor", 1));
        StringBuilder muchos = new StringBuilder("<businessSystem><usuarios>");
        for (int i = 0; i < 4; i++) {
            Dueno d = new Dueno("dueno" + i, "x", LocalDate.of(1970, 1, 1));
            d.anadirLocal(b);
            muchos.append(d.toXML());
        }
        muchos.append("</usuarios><locales>").append(b.toXML()).append("</locales></businessSystem>");
        esperarExcepcion("N3", "un local con mas de 3 propietarios",
                () -> BusinessSystem.parseXML(muchos.toString()));

        esperarExcepcion("N4", "raiz que no es un businessSystem",
                () -> BusinessSystem.parseXML(c.toXML()));
        esperarExcepcion("N5", "parseXMLFile con un fichero inexistente",
                () -> BusinessSystem.parseXMLFile(dir.resolve("no_existe.xml").toFile()));
    }

    // ------------------------------------------------------------ Auxiliares
    private static BusinessSystem crearSistema() {
        BusinessSystem sis = new BusinessSystem();
        Cliente lucia = new Cliente("lucia", "pass1", LocalDate.of(2001, 3, 15));
        Cliente iker = new Cliente("iker", "pass2", LocalDate.of(1998, 7, 2));
        Dueno marta = new Dueno("marta", "pass3", LocalDate.of(1975, 11, 20));
        Dueno jon = new Dueno("jon", "pass4", LocalDate.of(1982, 4, 9));
        sis.nuevoUsuario(lucia);
        sis.nuevoUsuario(iker);
        sis.nuevoUsuario(marta);
        sis.nuevoUsuario(jon);

        Bar bar = new Bar("Bar Txoko", new Direccion("Pamplona", "Navarra", "San Nicolas", 4));
        bar.addEspecialidad("pintxos");
        bar.setDescripcion("Bar de pintxos en el casco viejo");
        Restaurante rest = new Restaurante(22.0, 6, 80, "Asador Ribera",
                new Direccion("Tudela", "Navarra", "Herrerias", 10));
        Pub pub = new Pub("Pub Luna", new Direccion("Pamplona", "Navarra", "Jarauta", 30),
                LocalTime.of(4, 0), LocalTime.of(23, 0));
        sis.nuevoLocal(bar);
        sis.nuevoLocal(rest);
        sis.nuevoLocal(pub);
        sis.asociarLocal(bar, marta);
        sis.asociarLocal(bar, jon);
        sis.asociarLocal(rest, marta);
        sis.asociarLocal(pub, jon);

        Review r1 = new Review(lucia.getId(), bar.getId(), 5, "Los mejores pintxos", LocalDate.now().minusDays(4));
        Review r2 = new Review(iker.getId(), bar.getId(), 3, "Algo caro", LocalDate.now().minusDays(2));
        Review r3 = new Review(iker.getId(), rest.getId(), 4, "Buena carne", LocalDate.now().minusDays(10));
        sis.nuevaReview(r1);
        sis.nuevaReview(r2);
        sis.nuevaReview(r3);
        sis.nuevaContestacion(new Contestacion("Gracias, Lucia!", r1, marta), r1);

        sis.nuevaReserva(lucia, rest, LocalDate.now().plusDays(5), LocalTime.of(21, 30));
        sis.nuevaReserva(iker, bar, LocalDate.now().plusDays(1), LocalTime.of(13, 0));
        return sis;
    }

    /**
     * Prueba que puede lanzar excepciones comprobadas.
     */
    @FunctionalInterface
    private interface Prueba {

        boolean ejecutar() throws Exception;
    }

    /**
     * Accion que deberia fallar con una XMLParsingException; su resultado se
     * descarta.
     */
    @FunctionalInterface
    private interface Accion {

        Object ejecutar() throws Exception;
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

    private static void esperarExcepcion(String id, String descripcion, Accion prueba) {
        boolean ok;
        String detalle;
        try {
            prueba.ejecutar();
            ok = false;
            detalle = " -> no se lanzo XMLParsingException";
        } catch (XMLParsingException e) {
            ok = true;
            detalle = " (" + e.getMessage() + ")";
        } catch (Exception e) {
            ok = false;
            detalle = " -> se lanzo " + e.getClass().getSimpleName() + " en vez de XMLParsingException";
        }
        informar(id, ok, "rechaza " + descripcion + detalle);
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
