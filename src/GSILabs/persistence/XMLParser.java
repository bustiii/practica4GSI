package GSILabs.persistence;

import GSILabs.BModel.Bar;
import GSILabs.BModel.Cliente;
import GSILabs.BModel.Contestacion;
import GSILabs.BModel.Direccion;
import GSILabs.BModel.Dueno;
import GSILabs.BModel.Local;
import GSILabs.BModel.Pub;
import GSILabs.BModel.Reserva;
import GSILabs.BModel.Reservable;
import GSILabs.BModel.Restaurante;
import GSILabs.BModel.Review;
import GSILabs.BModel.Usuario;
import GSILabs.BSystem.BusinessSystem;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Creacion de objetos del paquete {@code GSILabs.BModel} a partir de su
 * representacion XML (alternativa a) del Ejercicio 3).
 * <p>
 * Para cada clase instanciable hay un metodo que crea el objeto desde un
 * {@code String} y otro que lo hace desde un {@code File}. Ademas se ofrece una
 * version que recibe un {@link Element} ya analizado, usada al leer el sistema
 * completo.
 * <p>
 * Politicas de lectura:
 * <ul>
 * <li>Los campos se buscan por nombre, por lo que pueden aparecer en cualquier
 * orden.</li>
 * <li>Un campo obligatorio ausente o repetido, un valor con formato incorrecto
 * o un dato que viola las reglas de negocio provocan una
 * {@link XMLParsingException}.</li>
 * <li>Los elementos desconocidos se ignoran.</li>
 * <li>Las relaciones entre entidades independientes (review-cliente,
 * review-local, reserva-cliente...) se guardan como identificadores. Por eso
 * {@link Contestacion} y {@link Reserva} solo pueden crearse si se proporciona
 * el {@link BusinessSystem} donde resolver esas referencias, y los locales de
 * un {@link Dueno} se asocian al leer el sistema completo.</li>
 * </ul>
 */
public final class XMLParser {

    private XMLParser() {
    }

    // ------------------------------------------------------------ Usuarios
    /**
     * Crea un usuario (cliente o propietario) desde su XML.
     *
     * @param str XML de un elemento {@code usuario}, {@code cliente} o
     * {@code dueno}
     * @return el usuario creado, del tipo indicado por el elemento
     * @throws XMLParsingException si el XML no es adecuado
     */
    public static Usuario parseUsuario(String str) throws XMLParsingException {
        return parseUsuario(raiz(str));
    }

    /**
     * Crea un usuario (cliente o propietario) desde un fichero XML.
     *
     * @param f fichero con un elemento {@code usuario}, {@code cliente} o
     * {@code dueno}
     * @return el usuario creado, del tipo indicado por el elemento
     * @throws XMLParsingException si el fichero no se puede leer o no es
     * adecuado
     */
    public static Usuario parseUsuario(File f) throws XMLParsingException {
        return parseUsuario(raiz(f));
    }

    /**
     * Crea un usuario desde un elemento XML ya analizado.
     *
     * @param e elemento {@code usuario}, {@code cliente} o {@code dueno}
     * @return el usuario creado
     * @throws XMLParsingException si el elemento no es adecuado
     */
    public static Usuario parseUsuario(Element e) throws XMLParsingException {
        switch (e.getTagName()) {
            case "cliente":
                return parseCliente(e);
            case "dueno":
                return parseDueno(e);
            case "usuario":
                String perfil = atributo(e, "perfil");
                if (Usuario.Perfil.CLIENTE.name().equals(perfil)) {
                    return crearCliente(e);
                }
                if (Usuario.Perfil.PROPIETARIO.name().equals(perfil)) {
                    return crearDueno(e);
                }
                throw new XMLParsingException("Perfil de usuario desconocido: " + perfil);
            default:
                throw new XMLParsingException("Se esperaba un usuario y se encontro <" + e.getTagName() + ">.");
        }
    }

    /**
     * @param str XML de un elemento {@code cliente}
     * @return el cliente creado
     * @throws XMLParsingException si el XML no es adecuado
     */
    public static Cliente parseCliente(String str) throws XMLParsingException {
        return parseCliente(raiz(str));
    }

    /**
     * @param f fichero con un elemento {@code cliente}
     * @return el cliente creado
     * @throws XMLParsingException si el fichero no se puede leer o no es
     * adecuado
     */
    public static Cliente parseCliente(File f) throws XMLParsingException {
        return parseCliente(raiz(f));
    }

    /**
     * @param e elemento {@code cliente}
     * @return el cliente creado
     * @throws XMLParsingException si el elemento no es adecuado
     */
    public static Cliente parseCliente(Element e) throws XMLParsingException {
        comprobarEtiqueta(e, "cliente");
        return crearCliente(e);
    }

    /**
     * Crea un propietario desde su XML. Los locales referenciados no se
     * asocian, ya que no existen fuera de un sistema; consulte
     * {@link #referenciasLocales(Element)}.
     *
     * @param str XML de un elemento {@code dueno}
     * @return el propietario creado, sin locales
     * @throws XMLParsingException si el XML no es adecuado
     */
    public static Dueno parseDueno(String str) throws XMLParsingException {
        return parseDueno(raiz(str));
    }

    /**
     * @param f fichero con un elemento {@code dueno}
     * @return el propietario creado, sin locales
     * @throws XMLParsingException si el fichero no se puede leer o no es
     * adecuado
     * @see #parseDueno(String)
     */
    public static Dueno parseDueno(File f) throws XMLParsingException {
        return parseDueno(raiz(f));
    }

    /**
     * @param e elemento {@code dueno}
     * @return el propietario creado, sin locales
     * @throws XMLParsingException si el elemento no es adecuado
     * @see #parseDueno(String)
     */
    public static Dueno parseDueno(Element e) throws XMLParsingException {
        comprobarEtiqueta(e, "dueno");
        referenciasLocales(e); // valida las referencias aunque no se resuelvan
        return crearDueno(e);
    }

    /**
     * Obtiene los identificadores de los locales que posee un propietario.
     *
     * @param dueno elemento {@code dueno}
     * @return lista de identificadores de local (vacia si no tiene)
     * @throws XMLParsingException si alguna referencia no es un entero
     */
    public static List<Integer> referenciasLocales(Element dueno) throws XMLParsingException {
        List<Integer> refs = new ArrayList<>();
        Element lista = hijoOpcional(dueno, "locales");
        if (lista != null) {
            for (Element local : hijos(lista, "local")) {
                refs.add(atributoEntero(local, "ref"));
            }
        }
        return refs;
    }

    private static Cliente crearCliente(Element e) throws XMLParsingException {
        int id = atributoEntero(e, "id");
        String nick = texto(e, "nick");
        String contrasena = texto(e, "contrasena");
        LocalDate nacimiento = fecha(e, "fechaNacimiento");
        return construir(e, () -> new Cliente(id, nick, contrasena, nacimiento));
    }

    private static Dueno crearDueno(Element e) throws XMLParsingException {
        int id = atributoEntero(e, "id");
        String nick = texto(e, "nick");
        String contrasena = texto(e, "contrasena");
        LocalDate nacimiento = fecha(e, "fechaNacimiento");
        return construir(e, () -> new Dueno(id, nick, contrasena, nacimiento));
    }

    // ------------------------------------------------------------ Direccion
    /**
     * @param str XML de un elemento {@code direccion}
     * @return la direccion creada
     * @throws XMLParsingException si el XML no es adecuado
     */
    public static Direccion parseDireccion(String str) throws XMLParsingException {
        return parseDireccion(raiz(str));
    }

    /**
     * @param f fichero con un elemento {@code direccion}
     * @return la direccion creada
     * @throws XMLParsingException si el fichero no se puede leer o no es
     * adecuado
     */
    public static Direccion parseDireccion(File f) throws XMLParsingException {
        return parseDireccion(raiz(f));
    }

    /**
     * @param e elemento {@code direccion}
     * @return la direccion creada
     * @throws XMLParsingException si el elemento no es adecuado
     */
    public static Direccion parseDireccion(Element e) throws XMLParsingException {
        comprobarEtiqueta(e, "direccion");
        String localidad = texto(e, "localidad");
        String provincia = texto(e, "provincia");
        String calle = texto(e, "calle");
        int numero = entero(e, "numero");
        return construir(e, () -> new Direccion(localidad, provincia, calle, numero));
    }

    // -------------------------------------------------------------- Locales
    /**
     * Crea un local desde su XML.
     *
     * @param str XML de un elemento {@code local}, {@code bar}, {@code pub} o
     * {@code restaurante}
     * @return el local creado, del tipo indicado por el elemento
     * @throws XMLParsingException si el XML no es adecuado
     */
    public static Local parseLocal(String str) throws XMLParsingException {
        return parseLocal(raiz(str));
    }

    /**
     * @param f fichero con un elemento {@code local}, {@code bar}, {@code pub}
     * o {@code restaurante}
     * @return el local creado, del tipo indicado por el elemento
     * @throws XMLParsingException si el fichero no se puede leer o no es
     * adecuado
     */
    public static Local parseLocal(File f) throws XMLParsingException {
        return parseLocal(raiz(f));
    }

    /**
     * @param e elemento {@code local}, {@code bar}, {@code pub} o
     * {@code restaurante}
     * @return el local creado, del tipo indicado por el elemento
     * @throws XMLParsingException si el elemento no es adecuado
     */
    public static Local parseLocal(Element e) throws XMLParsingException {
        switch (e.getTagName()) {
            case "bar":
                return parseBar(e);
            case "pub":
                return parsePub(e);
            case "restaurante":
                return parseRestaurante(e);
            case "local":
                int id = atributoEntero(e, "id");
                String nombre = texto(e, "nombre");
                Direccion d = parseDireccion(hijo(e, "direccion"));
                return completarLocal(e, construir(e, () -> new Local(id, nombre, d)));
            default:
                throw new XMLParsingException("Se esperaba un local y se encontro <" + e.getTagName() + ">.");
        }
    }

    /**
     * @param str XML de un elemento {@code bar}
     * @return el bar creado
     * @throws XMLParsingException si el XML no es adecuado
     */
    public static Bar parseBar(String str) throws XMLParsingException {
        return parseBar(raiz(str));
    }

    /**
     * @param f fichero con un elemento {@code bar}
     * @return el bar creado
     * @throws XMLParsingException si el fichero no se puede leer o no es
     * adecuado
     */
    public static Bar parseBar(File f) throws XMLParsingException {
        return parseBar(raiz(f));
    }

    /**
     * @param e elemento {@code bar}
     * @return el bar creado
     * @throws XMLParsingException si el elemento no es adecuado
     */
    public static Bar parseBar(Element e) throws XMLParsingException {
        comprobarEtiqueta(e, "bar");
        int id = atributoEntero(e, "id");
        String nombre = texto(e, "nombre");
        Direccion d = parseDireccion(hijo(e, "direccion"));
        Bar bar = completarLocal(e, construir(e, () -> new Bar(id, nombre, d)));
        Element lista = hijoOpcional(e, "especialidades");
        if (lista != null) {
            for (Element esp : hijos(lista, "especialidad")) {
                bar.addEspecialidad(esp.getTextContent().trim());
            }
        }
        return bar;
    }

    /**
     * @param str XML de un elemento {@code pub}
     * @return el pub creado
     * @throws XMLParsingException si el XML no es adecuado
     */
    public static Pub parsePub(String str) throws XMLParsingException {
        return parsePub(raiz(str));
    }

    /**
     * @param f fichero con un elemento {@code pub}
     * @return el pub creado
     * @throws XMLParsingException si el fichero no se puede leer o no es
     * adecuado
     */
    public static Pub parsePub(File f) throws XMLParsingException {
        return parsePub(raiz(f));
    }

    /**
     * @param e elemento {@code pub}
     * @return el pub creado
     * @throws XMLParsingException si el elemento no es adecuado
     */
    public static Pub parsePub(Element e) throws XMLParsingException {
        comprobarEtiqueta(e, "pub");
        int id = atributoEntero(e, "id");
        String nombre = texto(e, "nombre");
        Direccion d = parseDireccion(hijo(e, "direccion"));
        LocalTime apertura = convertir(e, "horaApertura", LocalTime::parse);
        LocalTime clausura = convertir(e, "horaClausura", LocalTime::parse);
        return completarLocal(e, construir(e, () -> new Pub(id, nombre, d, clausura, apertura)));
    }

    /**
     * @param str XML de un elemento {@code restaurante}
     * @return el restaurante creado
     * @throws XMLParsingException si el XML no es adecuado
     */
    public static Restaurante parseRestaurante(String str) throws XMLParsingException {
        return parseRestaurante(raiz(str));
    }

    /**
     * @param f fichero con un elemento {@code restaurante}
     * @return el restaurante creado
     * @throws XMLParsingException si el fichero no se puede leer o no es
     * adecuado
     */
    public static Restaurante parseRestaurante(File f) throws XMLParsingException {
        return parseRestaurante(raiz(f));
    }

    /**
     * @param e elemento {@code restaurante}
     * @return el restaurante creado
     * @throws XMLParsingException si el elemento no es adecuado
     */
    public static Restaurante parseRestaurante(Element e) throws XMLParsingException {
        comprobarEtiqueta(e, "restaurante");
        int id = atributoEntero(e, "id");
        String nombre = texto(e, "nombre");
        Direccion d = parseDireccion(hijo(e, "direccion"));
        double precio = convertir(e, "precioMenu", Double::parseDouble);
        int mesa = entero(e, "capacidadMesa");
        int total = entero(e, "capacidadTotal");
        return completarLocal(e, construir(e, () -> new Restaurante(id, precio, mesa, total, nombre, d)));
    }

    private static <T extends Local> T completarLocal(Element e, T local) throws XMLParsingException {
        Element descripcion = hijoOpcional(e, "descripcion");
        if (descripcion != null) {
            String texto = descripcion.getTextContent();
            construir(e, () -> {
                local.setDescripcion(texto);
                return local;
            });
        }
        return local;
    }

    // -------------------------------------------------------------- Reviews
    /**
     * Crea una review desde su XML. El cliente y el local quedan como
     * identificadores; su existencia se comprueba al registrarla en un sistema.
     *
     * @param str XML de un elemento {@code review}
     * @return la review creada
     * @throws XMLParsingException si el XML no es adecuado
     */
    public static Review parseReview(String str) throws XMLParsingException {
        return parseReview(raiz(str));
    }

    /**
     * @param f fichero con un elemento {@code review}
     * @return la review creada
     * @throws XMLParsingException si el fichero no se puede leer o no es
     * adecuado
     * @see #parseReview(String)
     */
    public static Review parseReview(File f) throws XMLParsingException {
        return parseReview(raiz(f));
    }

    /**
     * @param e elemento {@code review}
     * @return la review creada
     * @throws XMLParsingException si el elemento no es adecuado
     * @see #parseReview(String)
     */
    public static Review parseReview(Element e) throws XMLParsingException {
        comprobarEtiqueta(e, "review");
        int id = atributoEntero(e, "id");
        int cliente = atributoEntero(e, "cliente");
        int local = atributoEntero(e, "local");
        int valoracion = entero(e, "valoracion");
        Element nodoComentario = hijoOpcional(e, "comentario");
        String comentario = nodoComentario == null ? "" : nodoComentario.getTextContent();
        LocalDate visita = fecha(e, "fechaVisita");
        LocalDate creacion = fecha(e, "fechaCreacion");
        return construir(e, () -> new Review(id, cliente, local, valoracion, comentario, visita, creacion));
    }

    // -------------------------------------------------------- Contestaciones
    /**
     * Crea una contestacion desde su XML, resolviendo la review y el autor en
     * el sistema indicado.
     *
     * @param str XML de un elemento {@code contestacion}
     * @param sistema sistema donde estan registrados la review y el autor
     * @return la contestacion creada
     * @throws XMLParsingException si el XML no es adecuado o las referencias no
     * existen en el sistema
     */
    public static Contestacion parseContestacion(String str, BusinessSystem sistema) throws XMLParsingException {
        return parseContestacion(raiz(str), sistema);
    }

    /**
     * @param f fichero con un elemento {@code contestacion}
     * @param sistema sistema donde estan registrados la review y el autor
     * @return la contestacion creada
     * @throws XMLParsingException si el fichero no se puede leer, no es
     * adecuado o las referencias no existen en el sistema
     */
    public static Contestacion parseContestacion(File f, BusinessSystem sistema) throws XMLParsingException {
        return parseContestacion(raiz(f), sistema);
    }

    /**
     * @param e elemento {@code contestacion}
     * @param sistema sistema donde estan registrados la review y el autor
     * @return la contestacion creada
     * @throws XMLParsingException si el elemento no es adecuado o las
     * referencias no existen en el sistema
     */
    public static Contestacion parseContestacion(Element e, BusinessSystem sistema) throws XMLParsingException {
        comprobarEtiqueta(e, "contestacion");
        if (sistema == null) {
            throw new XMLParsingException("Hace falta un sistema para resolver las referencias de la contestacion.");
        }
        int idReview = atributoEntero(e, "review");
        int idAutor = atributoEntero(e, "autor");
        Review review = sistema.obtenerReview(idReview);
        if (review == null) {
            throw new XMLParsingException("La contestacion referencia una review inexistente: " + idReview);
        }
        Usuario autor = sistema.obtenerUsuario(idAutor);
        if (!(autor instanceof Dueno)) {
            throw new XMLParsingException("El autor de la contestacion no es un propietario registrado: " + idAutor);
        }
        String comentario = texto(e, "comentario");
        LocalDate fecha = fecha(e, "fecha");
        return construir(e, () -> new Contestacion(comentario, review, (Dueno) autor, fecha));
    }

    // -------------------------------------------------------------- Reservas
    /**
     * Crea una reserva desde su XML, resolviendo el cliente y el local en el
     * sistema indicado.
     *
     * @param str XML de un elemento {@code reserva}
     * @param sistema sistema donde estan registrados el cliente y el local
     * @return la reserva creada
     * @throws XMLParsingException si el XML no es adecuado o las referencias no
     * existen en el sistema
     */
    public static Reserva parseReserva(String str, BusinessSystem sistema) throws XMLParsingException {
        return parseReserva(raiz(str), sistema);
    }

    /**
     * @param f fichero con un elemento {@code reserva}
     * @param sistema sistema donde estan registrados el cliente y el local
     * @return la reserva creada
     * @throws XMLParsingException si el fichero no se puede leer, no es
     * adecuado o las referencias no existen en el sistema
     */
    public static Reserva parseReserva(File f, BusinessSystem sistema) throws XMLParsingException {
        return parseReserva(raiz(f), sistema);
    }

    /**
     * @param e elemento {@code reserva}
     * @param sistema sistema donde estan registrados el cliente y el local
     * @return la reserva creada
     * @throws XMLParsingException si el elemento no es adecuado o las
     * referencias no existen en el sistema
     */
    public static Reserva parseReserva(Element e, BusinessSystem sistema) throws XMLParsingException {
        comprobarEtiqueta(e, "reserva");
        if (sistema == null) {
            throw new XMLParsingException("Hace falta un sistema para resolver las referencias de la reserva.");
        }
        int id = atributoEntero(e, "id");
        int idCliente = atributoEntero(e, "cliente");
        int idLocal = atributoEntero(e, "local");
        Usuario cliente = sistema.obtenerUsuario(idCliente);
        if (!(cliente instanceof Cliente)) {
            throw new XMLParsingException("La reserva referencia un cliente inexistente: " + idCliente);
        }
        Local local = sistema.obtenerLocal(idLocal);
        if (!(local instanceof Reservable)) {
            throw new XMLParsingException("La reserva referencia un local reservable inexistente: " + idLocal);
        }
        LocalDateTime fecha = convertir(e, "fecha", LocalDateTime::parse);
        Element nodoDescuento = hijoOpcional(e, "descuento");
        Double descuento = nodoDescuento == null ? null : convertir(e, "descuento", Double::valueOf);
        return construir(e, () -> new Reserva(id, fecha, descuento, (Cliente) cliente, (Reservable) local));
    }

    // ---------------------------------------------------- Utilidades de DOM
    /**
     * Analiza un texto XML y devuelve su elemento raiz.
     *
     * @param str texto XML
     * @return el elemento raiz
     * @throws XMLParsingException si el texto es nulo o no es XML bien formado
     */
    public static Element raiz(String str) throws XMLParsingException {
        if (str == null || str.isBlank()) {
            throw new XMLParsingException("El XML no puede ser nulo ni vacio.");
        }
        try {
            return documento().parse(new InputSource(new StringReader(str))).getDocumentElement();
        } catch (SAXException | IOException ex) {
            throw new XMLParsingException("XML mal formado: " + ex.getMessage(), ex);
        }
    }

    /**
     * Analiza un fichero XML y devuelve su elemento raiz.
     *
     * @param f fichero XML
     * @return el elemento raiz
     * @throws XMLParsingException si el fichero no existe, no se puede leer o
     * no es XML bien formado
     */
    public static Element raiz(File f) throws XMLParsingException {
        if (f == null || !f.isFile()) {
            throw new XMLParsingException("El fichero no existe: " + f);
        }
        try {
            Document doc = documento().parse(f);
            return doc.getDocumentElement();
        } catch (SAXException ex) {
            throw new XMLParsingException("XML mal formado en " + f.getName() + ": " + ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new XMLParsingException("No se pudo leer " + f.getName() + ": " + ex.getMessage(), ex);
        }
    }

    private static DocumentBuilder documento() throws XMLParsingException {
        try {
            DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
            // no se usan DTD; se prohiben para evitar entidades externas
            fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            fabrica.setExpandEntityReferences(false);
            DocumentBuilder builder = fabrica.newDocumentBuilder();
            builder.setErrorHandler(null); // los errores se notifican solo como excepcion
            return builder;
        } catch (ParserConfigurationException ex) {
            throw new XMLParsingException("No se pudo configurar el analizador XML.", ex);
        }
    }

    /**
     * Comprueba que un elemento tiene la etiqueta esperada.
     *
     * @param e elemento a comprobar
     * @param etiqueta etiqueta esperada
     * @throws XMLParsingException si el elemento es nulo o tiene otra etiqueta
     */
    public static void comprobarEtiqueta(Element e, String etiqueta) throws XMLParsingException {
        if (e == null) {
            throw new XMLParsingException("Falta el elemento <" + etiqueta + ">.");
        }
        if (!etiqueta.equals(e.getTagName())) {
            throw new XMLParsingException("Se esperaba <" + etiqueta + "> y se encontro <" + e.getTagName() + ">.");
        }
    }

    /**
     * Devuelve los hijos directos de un elemento con una etiqueta dada.
     *
     * @param padre elemento padre
     * @param etiqueta etiqueta buscada
     * @return lista de hijos con esa etiqueta, en orden de aparicion
     */
    public static List<Element> hijos(Element padre, String etiqueta) {
        List<Element> res = new ArrayList<>();
        NodeList nodos = padre.getChildNodes();
        for (int i = 0; i < nodos.getLength(); i++) {
            Node n = nodos.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && etiqueta.equals(n.getNodeName())) {
                res.add((Element) n);
            }
        }
        return res;
    }

    /**
     * Devuelve todos los hijos directos de tipo elemento.
     *
     * @param padre elemento padre
     * @return lista de elementos hijo, en orden de aparicion
     */
    public static List<Element> hijos(Element padre) {
        List<Element> res = new ArrayList<>();
        NodeList nodos = padre.getChildNodes();
        for (int i = 0; i < nodos.getLength(); i++) {
            if (nodos.item(i).getNodeType() == Node.ELEMENT_NODE) {
                res.add((Element) nodos.item(i));
            }
        }
        return res;
    }

    /**
     * Devuelve el unico hijo con la etiqueta dada, o null si no existe.
     *
     * @param padre elemento padre
     * @param etiqueta etiqueta buscada
     * @return el hijo, o null si no aparece
     * @throws XMLParsingException si aparece mas de una vez
     */
    public static Element hijoOpcional(Element padre, String etiqueta) throws XMLParsingException {
        List<Element> encontrados = hijos(padre, etiqueta);
        if (encontrados.size() > 1) {
            throw new XMLParsingException("<" + etiqueta + "> aparece mas de una vez en <" + padre.getTagName() + ">.");
        }
        return encontrados.isEmpty() ? null : encontrados.get(0);
    }

    private static Element hijo(Element padre, String etiqueta) throws XMLParsingException {
        Element e = hijoOpcional(padre, etiqueta);
        if (e == null) {
            throw new XMLParsingException("Falta <" + etiqueta + "> en <" + padre.getTagName() + ">.");
        }
        return e;
    }

    private static String texto(Element padre, String etiqueta) throws XMLParsingException {
        return hijo(padre, etiqueta).getTextContent();
    }

    private static String atributo(Element e, String nombre) throws XMLParsingException {
        if (!e.hasAttribute(nombre)) {
            throw new XMLParsingException("Falta el atributo '" + nombre + "' en <" + e.getTagName() + ">.");
        }
        return e.getAttribute(nombre);
    }

    private static int atributoEntero(Element e, String nombre) throws XMLParsingException {
        String valor = atributo(e, nombre);
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException ex) {
            throw new XMLParsingException("El atributo '" + nombre + "' de <" + e.getTagName()
                    + "> no es un entero: " + valor, ex);
        }
    }

    private static int entero(Element padre, String etiqueta) throws XMLParsingException {
        return convertir(padre, etiqueta, Integer::parseInt);
    }

    private static LocalDate fecha(Element padre, String etiqueta) throws XMLParsingException {
        return convertir(padre, etiqueta, LocalDate::parse);
    }

    /**
     * Conversor de texto a valor que puede fallar con una excepcion no
     * comprobada.
     */
    @FunctionalInterface
    private interface Conversor<T> {

        T convertir(String texto);
    }

    private static <T> T convertir(Element padre, String etiqueta, Conversor<T> conversor) throws XMLParsingException {
        String valor = texto(padre, etiqueta).trim();
        try {
            return conversor.convertir(valor);
        } catch (RuntimeException ex) {
            throw new XMLParsingException("Valor incorrecto en <" + etiqueta + "> de <" + padre.getTagName()
                    + ">: " + valor, ex);
        }
    }

    private static <T> T construir(Element e, Supplier<T> constructor) throws XMLParsingException {
        try {
            return constructor.get();
        } catch (IllegalArgumentException ex) {
            throw new XMLParsingException("Datos no validos en <" + e.getTagName() + ">: " + ex.getMessage(), ex);
        }
    }
}
