package GSILabs.BSystem;

import GSILabs.BModel.*;
import GSILabs.persistence.XMLParser;
import GSILabs.persistence.XMLParsingException;
import GSILabs.serializable.XMLRepresentable;
import GSILabs.serializable.XMLUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.io.File;
import java.io.IOException;
import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;
import org.w3c.dom.Element;

/**
 * Implementacion en memoria (no persistente) de {@link LeisureOffice} y
 * {@link LookupService}. Mantiene la coherencia de la informacion: eliminar un
 * usuario o un local elimina en cascada sus reviews, contestaciones y reservas,
 * y modificar uno de ellos actualiza los objetos que lo referencian. Todas las
 * operaciones devuelven false / null ante argumentos nulos o inexistentes en
 * lugar de lanzar excepciones.
 *
 *
 */
public class BusinessSystem implements LeisureOffice, LookupService, XMLRepresentable {

    /**
     * Numero maximo de propietarios por local (C06).
     */
    public static final int MAX_PROPIETARIOS = 3;
    /**
     * Numero minimo de propietarios que debe conservar un local una vez tiene
     * alguno (C06). El interface {@link LeisureOffice#nuevoLocal(Local)} no
     * recibe un propietario inicial, asi que un local puede existir
     * momentaneamente sin dueño justo tras darse de alta; lo que este minimo
     * impide es que {@link #desasociarLocal(Local, Dueño)} deje a un local que
     * ya tenia propietario sin ninguno.
     */
    public static final int MIN_PROPIETARIOS = 1;
    /**
     * Longitud minima del nick, igual que {@link Usuario#NICK_MIN}.
     */
    private static final int MIN_NICK = 3;
    /**
     * Edad minima para registrarse, igual que {@link Usuario#EDAD_MIN}.
     */
    private static final int MIN_EDAD = 14;

    /**
     * Usuarios (Clientes y Dueños) dados de alta.
     */
    private final List<Usuario> usuarios = new ArrayList<>();
    /**
     * Locales dados de alta.
     */
    private final List<Local> locales = new ArrayList<>();
    /**
     * Reviews dadas de alta.
     */
    private final List<Review> reviews = new ArrayList<>();
    /**
     * Contestaciones dadas de alta.
     */
    private final List<Contestacion> contestaciones = new ArrayList<>();
    /**
     * Reservas dadas de alta.
     */
    private final List<Reserva> reservas = new ArrayList<>();

    /**
     * Crea un sistema vacio, sin usuarios, locales, reviews, contestaciones ni
     * reservas.
     */
    public BusinessSystem() {
    }

    // ------------------------------------------------------------ Usuarios
    /**
     * Da de alta un usuario si su nick es valido (al menos {@value #MIN_NICK}
     * caracteres), su edad cumple el minimo ({@value #MIN_EDAD} años) y aun no
     * existe otro usuario con el mismo nick.
     *
     * @param u usuario a dar de alta
     * @return true si se pudo añadir; false si es nulo, invalido o el nick ya
     * existe
     */
    @Override
    public boolean nuevoUsuario(Usuario u) {
        if (!usuarioValido(u) || existeNick(u.getNick())) {
            return false;
        }
        usuarios.add(u);
        return true;
    }

    /**
     * Elimina un usuario del sistema y, en cascada, sus reviews (junto con la
     * contestacion de cada una), sus reservas, y las contestaciones que hubiera
     * escrito como propietario.
     *
     * @param u usuario a eliminar
     * @return true si existia y se pudo eliminar
     */
    @Override
    public boolean eliminaUsuario(Usuario u) {
        Usuario s = (u == null) ? null : obtenerUsuario(u.getNick());
        if (s == null) {
            return false;
        }
        for (Review r : new ArrayList<>(reviews)) {
            if (r.getCliente() == s.getId()) {
                borrarReview(r);
            }
        }
        reservas.removeIf(r -> r.getCliente().equals(s));
        contestaciones.removeIf(c -> c.getAutor().equals(s));
        usuarios.remove(s);
        return true;
    }

    /**
     * Sustituye un usuario existente por una nueva version, siempre que esta
     * cumpla las mismas reglas que un alta y su nick no coincida con el de otro
     * usuario distinto. Las reviews, contestaciones y reservas que apuntaban al
     * usuario viejo se reconstruyen para apuntar al nuevo.
     *
     * @param u usuario que se quiere sustituir
     * @param nuevoU version nueva del usuario
     * @return true si el usuario existia, la version nueva es valida y se pudo
     * sustituir
     */
    @Override
    public boolean modificaUsuario(Usuario u, Usuario nuevoU) {
        Usuario s = (u == null) ? null : obtenerUsuario(u.getNick());
        if (s == null || !usuarioValido(nuevoU)) {
            return false;
        }
        if (!s.getNick().equals(nuevoU.getNick()) && existeNick(nuevoU.getNick())) {
            return false;
        }
        if (s instanceof Dueno && nuevoU instanceof Dueno) {
            for (Local l : ((Dueno) s).getLocales()) {
                ((Dueno) nuevoU).anadirLocal(l);
            }
        }
        reconstruir(s, nuevoU, null, null);
        usuarios.set(usuarios.indexOf(s), nuevoU);
        return true;
    }

    /**
     * Comprueba si existe algun usuario con el nick indicado.
     *
     * @param nick nick a buscar
     * @return true si hay un usuario con ese nick
     */
    @Override
    public boolean existeNick(String nick) {
        return obtenerUsuario(nick) != null;
    }

    /**
     * Busca un usuario por su nick.
     *
     * @param nick nick a buscar
     * @return el usuario con ese nick, o null si no existe
     */
    @Override
    public Usuario obtenerUsuario(String nick) {
        if (nick == null) {
            return null;
        }
        for (Usuario u : usuarios) {
            if (u.getNick().equals(nick)) {
                return u;
            }
        }
        return null;
    }

    /**
     * Busca un usuario registrado por su identificador.
     *
     * @param id identificador del usuario
     * @return el usuario, o null si no hay ninguno con ese identificador
     */
    public Usuario obtenerUsuario(int id) {
        for (Usuario u : usuarios) {
            if (u.getId() == id) {
                return u;
            }
        }
        return null;
    }

    // ------------------------------------------------------------- Reviews
    /**
     * Incorpora una review al sistema, siempre que su cliente y su local esten
     * registrados y no exista ya una review del mismo cliente para el mismo
     * local en la misma fecha de visita (C05).
     *
     * @param r review a incorporar
     * @return true si se pudo añadir
     */
    @Override
    public boolean nuevaReview(Review r) {
        if (r == null) {
            return false;
        }
        Usuario cliente = obtenerUsuario(r.getCliente());
        Local local = obtenerLocal(r.getLocal());
        if (!(cliente instanceof Cliente) || local == null) {
            return false;
        }
        if (existeRewiew(cliente, local, r.getFechaVisita())) {
            return false;
        }
        reviews.add(r);
        local.anadirReview(r);
        return true;
    }

    /**
     * Elimina una review del sistema, siempre que exista y no tenga ya una
     * contestacion asociada.
     *
     * @param r review a eliminar
     * @return true si existia, no tenia contestacion y se elimino
     */
    @Override
    public boolean eliminaReview(Review r) {
        Review s = reviewRegistrada(r);
        if (s == null || tieneContestacion(s)) {
            return false;
        }
        borrarReview(s);
        return true;
    }

    /**
     * Comprueba si un cliente ya valoro un local en una fecha de visita dada.
     *
     * @param u cliente autor de la posible review
     * @param l local visitado
     * @param ld fecha de la visita
     * @return true si esa review existe
     */
    @Override
    public boolean existeRewiew(Usuario u, Local l, LocalDate ld) {
        if (u == null || l == null || ld == null) {
            return false;
        }
        for (Review r : reviews) {
            if (r.getCliente() == u.getId() && r.getLocal() == l.getId() && r.getFechaVisita().equals(ld)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------ Contestaciones
    /**
     * Añade una contestacion a una review, siempre que la review este
     * registrada, no tenga ya contestacion, y el autor sea propietario del
     * local de esa review (C07).
     *
     * @param c contestacion a añadir
     * @param r review que se contesta
     * @return true si se pudo añadir
     */
    @Override
    public boolean nuevaContestacion(Contestacion c, Review r) {
        Review s = reviewRegistrada(r);
        if (c == null || s == null || !c.getReview().equals(s) || tieneContestacion(s)) {
            return false;
        }
        Usuario autor = obtenerUsuario(c.getAutor().getNick());
        // C07: solo el propietario del local puede contestar
        if (!(autor instanceof Dueno) || !esPropietario((Dueno) autor, obtenerLocal(s.getLocal()))) {
            return false;
        }
        contestaciones.add(c);
        return true;
    }

    /**
     * Comprueba si una review tiene contestacion.
     *
     * @param r review a consultar
     * @return true si la review existe y tiene contestacion
     */
    @Override
    public boolean tieneContestacion(Review r) {
        return obtenerContestacion(r) != null;
    }

    /**
     * Recupera la contestacion de una review.
     *
     * @param r review a consultar
     * @return la contestacion, o null si la review no existe o no tiene
     * contestacion
     */
    @Override
    public Contestacion obtenerContestacion(Review r) {
        Review s = reviewRegistrada(r);
        if (s == null) {
            return null;
        }
        for (Contestacion c : contestaciones) {
            if (c.getReview().equals(s)) {
                return c;
            }
        }
        return null;
    }

    /**
     * Elimina una contestacion del sistema.
     *
     * @param c contestacion a eliminar
     * @return true si existia y se elimino
     */
    @Override
    public boolean eliminaContestacion(Contestacion c) {
        return c != null && contestaciones.remove(c);
    }

    /**
     * Elimina la contestacion asociada a una review, si la tiene.
     *
     * @param r review cuya contestacion se quiere eliminar
     * @return true si tenia contestacion y se elimino
     */
    @Override
    public boolean eliminaContestacion(Review r) {
        return eliminaContestacion(obtenerContestacion(r));
    }

    /**
     * Busca una review registrada por su identificador.
     *
     * @param id identificador de la review
     * @return la review, o null si no hay ninguna con ese identificador
     */
    public Review obtenerReview(int id) {
        for (Review r : reviews) {
            if (r.getId() == id) {
                return r;
            }
        }
        return null;
    }

    // -------------------------------------------------------------- Locales
    /**
     * Añade un local al sistema, siempre que no haya ya otro local en la misma
     * direccion (C01).
     *
     * @param l local a añadir
     * @return true si se pudo añadir
     */
    @Override
    public boolean nuevoLocal(Local l) {
        if (l == null || l.getUbicacion() == null || obtenerLocal(l.getUbicacion()) != null) {
            return false;
        }
        locales.add(l);
        return true;
    }

    /**
     * Elimina un local del sistema y, en cascada, sus reviews (junto con sus
     * contestaciones), sus reservas, y lo desvincula de todos sus propietarios.
     *
     * @param l local a eliminar
     * @return true si existia y se pudo eliminar
     */
    @Override
    public boolean eliminarLocal(Local l) {
        Local s = localRegistrado(l);
        if (s == null) {
            return false;
        }
        for (Review r : new ArrayList<>(reviews)) {
            if (r.getLocal() == s.getId()) {
                borrarReview(r);
            }
        }
        reservas.removeIf(r -> r.getLocal().equals(s));
        for (Usuario u : usuarios) {
            if (u instanceof Dueno) {
                ((Dueno) u).eliminarLocal(s);
            }
        }
        locales.remove(s);
        return true;
    }

    /**
     * Busca el local instalado en una direccion.
     *
     * @param d direccion a buscar
     * @return el local en esa direccion, o null si no existe
     */
    @Override
    public Local obtenerLocal(Direccion d) {
        if (d == null) {
            return null;
        }
        for (Local l : locales) {
            if (l.getUbicacion().equals(d)) {
                return l;
            }
        }
        return null;
    }

    /**
     * Asocia un local a un propietario, siempre que ambos existan, el
     * propietario no lo fuera ya, y el local no haya llegado al maximo de
     * {@value #MAX_PROPIETARIOS} propietarios (C06).
     *
     * @param l local existente en el sistema
     * @param p propietario existente en el sistema
     * @return true si se pudo asociar
     */
    /**
     * Busca un local registrado por su identificador.
     *
     * @param id identificador del local
     * @return el local, o null si no hay ninguno con ese identificador
     */
    public Local obtenerLocal(int id) {
        for (Local l : locales) {
            if (l.getId() == id) {
                return l;
            }
        }
        return null;
    }

    @Override
    public boolean asociarLocal(Local l, Dueno p) {
        Local sl = localRegistrado(l);
        Usuario sp = (p == null) ? null : obtenerUsuario(p.getNick());
        if (sl == null || !(sp instanceof Dueno)) {
            return false;
        }
        Dueno dueno = (Dueno) sp;
        if (esPropietario(dueno, sl) || contarPropietarios(sl) >= MAX_PROPIETARIOS) {
            return false;
        }
        dueno.anadirLocal(sl);
        return true;
    }

    /**
     * Desliga un local de un propietario, siempre que ambos existan, ya esten
     * relacionados, y el local vaya a conservar al menos
     * {@value #MIN_PROPIETARIOS} propietario tras la operacion (C06).
     *
     * @param l local existente en el sistema
     * @param p propietario existente en el sistema
     * @return true si se pudo desligar
     */
    @Override
    public boolean desasociarLocal(Local l, Dueno p) {
        Local sl = localRegistrado(l);
        Usuario sp = (p == null) ? null : obtenerUsuario(p.getNick());
        if (sl == null || !(sp instanceof Dueno) || !esPropietario((Dueno) sp, sl)) {
            return false;
        }
        // C06: un local no puede quedarse sin propietarios (minimo 1) una vez ya tenia alguno.
        if (contarPropietarios(sl) <= MIN_PROPIETARIOS) {
            return false;
        }
        ((Dueno) sp).eliminarLocal(sl);
        return true;
    }

    /**
     * Reemplaza un local por una nueva version, siempre que la nueva direccion
     * no este ocupada por otro local distinto. Los propietarios, reviews,
     * contestaciones y reservas que apuntaban al local viejo se reconstruyen
     * para apuntar al nuevo.
     *
     * @param viejoL local que se quiere sustituir
     * @param nuevoL version nueva del local
     * @return true si el local existia y se pudo sustituir
     */
    @Override
    public boolean actualizarLocal(Local viejoL, Local nuevoL) {
        Local s = localRegistrado(viejoL);
        if (s == null || nuevoL == null || nuevoL.getUbicacion() == null) {
            return false;
        }
        Local enDestino = obtenerLocal(nuevoL.getUbicacion());
        if (enDestino != null && enDestino != s) {
            return false;
        }
        for (Usuario u : usuarios) {
            if (u instanceof Dueno && esPropietario((Dueno) u, s)) {
                ((Dueno) u).eliminarLocal(s);
                ((Dueno) u).anadirLocal(nuevoL);
            }
        }
        reconstruir(null, null, s, nuevoL);
        locales.set(locales.indexOf(s), nuevoL);
        return true;
    }

    /**
     * Obtiene las reviews de un local.
     *
     * @param l local a consultar
     * @return las reviews de ese local, o null si el local no existe
     */
    @Override
    public Review[] verReviews(Local l) {
        Local s = localRegistrado(l);
        if (s == null) {
            return null;
        }
        List<Review> res = new ArrayList<>();
        for (Review r : reviews) {
            if (r.getLocal() == s.getId()) {
                res.add(r);
            }
        }
        return res.toArray(new Review[0]);
    }

    // -------------------------------------------------------------- Reservas
    /**
     * Anota una reserva de un cliente en un local reservable, siempre que ambos
     * existan, la fecha y hora sean futuras, y el cliente no tenga ya otra
     * reserva para ese local en esa fecha.
     *
     * @param c cliente que reserva
     * @param r local reservable donde se efectua la reserva
     * @param ld fecha de la reserva
     * @param lt hora de la reserva
     * @return true si se pudo anotar
     */
    @Override
    public boolean nuevaReserva(Cliente c, Reservable r, LocalDate ld, LocalTime lt) {
        if (c == null || r == null || ld == null || lt == null) {
            return false;
        }
        Usuario cliente = obtenerUsuario(c.getNick());
        Local local = (r instanceof Local) ? localRegistrado((Local) r) : null;
        if (!(cliente instanceof Cliente) || !(local instanceof Reservable)) {
            return false;
        }
        LocalDateTime fecha = LocalDateTime.of(ld, lt);
        if (!fecha.isAfter(LocalDateTime.now())) {
            return false;
        }
        for (Reserva o : reservas) {
            if (o.getCliente().equals(cliente) && o.getLocal().equals(local)
                    && o.getFecha().toLocalDate().equals(ld)) {
                return false;
            }
        }
        reservas.add(new Reserva(fecha, null, (Cliente) cliente, (Reservable) local));
        return true;
    }

    /**
     * Obtiene las reservas de un cliente.
     *
     * @param c cliente a consultar
     * @return las reservas de ese cliente, o null si el cliente no existe
     */
    @Override
    public Reserva[] obtenerReservas(Cliente c) {
        if (c == null || !(obtenerUsuario(c.getNick()) instanceof Cliente)) {
            return null;
        }
        List<Reserva> res = new ArrayList<>();
        for (Reserva r : reservas) {
            if (r.getCliente().equals(c)) {
                res.add(r);
            }
        }
        return res.toArray(new Reserva[0]);
    }

    /**
     * Obtiene las reservas de un local reservable.
     *
     * @param r local a consultar
     * @return las reservas de ese local, o null si el local no existe
     */
    @Override
    public Reserva[] obtenerReservas(Reservable r) {
        Local s = (r instanceof Local) ? localRegistrado((Local) r) : null;
        if (s == null) {
            return null;
        }
        List<Reserva> res = new ArrayList<>();
        for (Reserva o : reservas) {
            if (o.getLocal().equals(s)) {
                res.add(o);
            }
        }
        return res.toArray(new Reserva[0]);
    }

    /**
     * Obtiene las reservas de un dia concreto.
     *
     * @param ld fecha a consultar
     * @return las reservas de ese dia (lista vacia si no hay ninguna)
     */
    @Override
    public Reserva[] obtenerReservas(LocalDate ld) {
        List<Reserva> res = new ArrayList<>();
        for (Reserva r : reservas) {
            if (r.getFecha().toLocalDate().equals(ld)) {
                res.add(r);
            }
        }
        return res.toArray(new Reserva[0]);
    }

    /**
     * Elimina una reserva del sistema.
     *
     * @param r reserva a eliminar
     * @return true si existia y se elimino
     */
    @Override
    public boolean eliminarReserva(Reserva r) {
        return r != null && reservas.remove(r);
    }

    // -------------------------------------------------------------- Listados
    /**
     * Lista los locales de una ciudad y provincia.
     *
     * @param ciudad ciudad de interes
     * @param provincia provincia de interes
     * @return los locales en esa ciudad y provincia (lista vacia si no hay
     * ninguno)
     */
    @Override
    public Local[] listarLocales(String ciudad, String provincia) {
        return filtrar(Local.class, ciudad, provincia).toArray(new Local[0]);
    }

    /**
     * Lista los bares de una ciudad y provincia.
     *
     * @param ciudad ciudad de interes
     * @param provincia provincia de interes
     * @return los bares en esa ciudad y provincia (lista vacia si no hay
     * ninguno)
     */
    @Override
    public Bar[] listarBares(String ciudad, String provincia) {
        return filtrar(Bar.class, ciudad, provincia).toArray(new Bar[0]);
    }

    /**
     * Lista los restaurantes de una ciudad y provincia.
     *
     * @param ciudad ciudad de interes
     * @param provincia provincia de interes
     * @return los restaurantes en esa ciudad y provincia (lista vacia si no hay
     * ninguno)
     */
    @Override
    public Restaurante[] listarRestaurantes(String ciudad, String provincia) {
        return filtrar(Restaurante.class, ciudad, provincia).toArray(new Restaurante[0]);
    }

    /**
     * Lista los pubs de una ciudad y provincia.
     *
     * @param ciudad ciudad de interes
     * @param provincia provincia de interes
     * @return los pubs en esa ciudad y provincia (lista vacia si no hay
     * ninguno)
     */
    @Override
    public Pub[] listarPubs(String ciudad, String provincia) {
        return filtrar(Pub.class, ciudad, provincia).toArray(new Pub[0]);
    }

    // --------------------------------------------------------- LookupService
    /**
     * Calcula la valoracion media de un local, contando todas sus reviews.
     *
     * @param l local a consultar
     * @return la valoracion media, 0 si no tiene reviews, o -1 si el local no
     * existe
     */
    @Override
    public float obtenerValoracionMedia(Local l) {
        Local s = localRegistrado(l);
        if (s == null) {
            return -1;
        }
        return mediaDe(reviewsDe(s), 0, Integer.MAX_VALUE);
    }

    /**
     * Calcula la valoracion media de todas las reviews de los locales de un
     * propietario.
     *
     * @param p propietario a consultar
     * @return la valoracion media, 0 si no tiene reviews, o -1 si el
     * propietario no existe
     */
    @Override
    public float obtenerValoracionMedia(Dueno p) {
        Usuario s = (p == null) ? null : obtenerUsuario(p.getNick());
        if (!(s instanceof Dueno)) {
            return -1;
        }
        List<Review> propias = new ArrayList<>();
        for (Review r : reviews) {
            if (esPropietario((Dueno) s, obtenerLocal(r.getLocal()))) {
                propias.add(r);
            }
        }
        return mediaDe(propias, 0, Integer.MAX_VALUE);
    }

    /**
     * Calcula la valoracion media de un local, contando solo las reviews de
     * clientes que tenian, en el momento de crear la review, una edad dentro
     * del rango indicado.
     *
     * @param l local a consultar
     * @param edadEntre edad minima del rango (incluida)
     * @param edadHasta edad maxima del rango (incluida)
     * @return la valoracion media del rango, 0 si no hay ninguna, o -1 si el
     * local no existe
     */
    @Override
    public float obtenerValoracionMedia(Local l, int edadEntre, int edadHasta) {
        Local s = localRegistrado(l);
        if (s == null) {
            return -1;
        }
        return mediaDe(reviewsDe(s), edadEntre, edadHasta);
    }

    /**
     * Lista los locales de una ciudad y provincia ordenados por valoracion
     * media descendente.
     *
     * @param ciudad ciudad de interes
     * @param provincia provincia de interes
     * @return los locales ordenados (lista vacia si no hay ninguno)
     */
    @Override
    public Local[] obtenerLocalesOrdenados(String ciudad, String provincia) {
        return ordenar(filtrar(Local.class, ciudad, provincia), Local[]::new);
    }

    /**
     * Lista los locales de una provincia ordenados por valoracion media
     * descendente.
     *
     * @param provincia provincia de interes
     * @return los locales ordenados (lista vacia si no hay ninguno)
     */
    @Override
    public Local[] obtenerLocalesOrdenados(String provincia) {
        List<Local> res = new ArrayList<>();
        for (Local l : locales) {
            if (l.getUbicacion().getProvincia().equalsIgnoreCase(provincia)) {
                res.add(l);
            }
        }
        return ordenar(res, Local[]::new);
    }

    /**
     * Lista los bares de una ciudad y provincia ordenados por valoracion media
     * descendente.
     *
     * @param ciudad ciudad de interes
     * @param provincia provincia de interes
     * @return los bares ordenados (lista vacia si no hay ninguno)
     */
    @Override
    public Bar[] obtenerBaresOrdenados(String ciudad, String provincia) {
        return ordenar(filtrar(Bar.class, ciudad, provincia), Bar[]::new);
    }

    /**
     * Lista los restaurantes de una ciudad y provincia ordenados por valoracion
     * media descendente.
     *
     * @param ciudad ciudad de interes
     * @param provincia provincia de interes
     * @return los restaurantes ordenados (lista vacia si no hay ninguno)
     */
    @Override
    public Restaurante[] obtenerRestaurantesOrdenados(String ciudad, String provincia) {
        return ordenar(filtrar(Restaurante.class, ciudad, provincia), Restaurante[]::new);
    }

    /**
     * Lista los pubs de una ciudad y provincia ordenados por valoracion media
     * descendente.
     *
     * @param ciudad ciudad de interes
     * @param provincia provincia de interes
     * @return los pubs ordenados (lista vacia si no hay ninguno)
     */
    @Override
    public Pub[] obtenerPubOrdenados(String ciudad, String provincia) {
        return ordenar(filtrar(Pub.class, ciudad, provincia), Pub[]::new);
    }

    // ------------------------------------------------------ Propietarios / ODS
    /**
     * Obtiene los propietarios de un local registrado.
     *
     * @param l local del que se quieren conocer los propietarios
     * @return los propietarios del local (vacio si no tiene ninguno), o null si
     * el local no esta registrado
     */
    public Dueno[] obtenerPropietarios(Local l) {
        Local s = localRegistrado(l);
        if (s == null) {
            return null;
        }
        List<Dueno> res = new ArrayList<>();
        for (Usuario u : usuarios) {
            if (u instanceof Dueno && esPropietario((Dueno) u, s)) {
                res.add((Dueno) u);
            }
        }
        return res.toArray(new Dueno[0]);
    }

    public int importaBares(File f) {
        if (f == null || !f.isFile()) {
            return 0;
        }
        int importados = 0;
        try {
            Sheet hoja = SpreadSheet.createFromFile(f).getSheet(0);
            for (int fila = 0; fila < hoja.getRowCount(); fila++) {
                String nombre = textoCelda(hoja, 0, fila);
                if (nombre.isEmpty()) {
                    break;
                }
                if (importaBar(hoja, fila, nombre)) {
                    importados++;
                }
            }
        } catch (IOException e) {
            // fichero ilegible: se devuelven los bares importados hasta el momento
        }
        return importados;
    }

    private boolean importaBar(Sheet hoja, int fila, String nombre) {
        try {
            String calleNumero = textoCelda(hoja, 1, fila);
            int sep = calleNumero.lastIndexOf(' ');
            if (sep < 0) {
                return false;
            }
            String calle = calleNumero.substring(0, sep).trim();
            int numero = Integer.parseInt(calleNumero.substring(sep + 1).trim());
            Direccion d = new Direccion(textoCelda(hoja, 2, fila), textoCelda(hoja, 3, fila), calle, numero);
            Bar bar = new Bar(nombre, d);
            for (int col = 5; col < hoja.getColumnCount(); col++) {
                String tag = textoCelda(hoja, col, fila);
                if (tag.isEmpty()) {
                    break;
                }
                bar.addEspecialidad(tag);
            }
            if (!nuevoLocal(bar)) {
                return false;
            }
            for (String nick : textoCelda(hoja, 4, fila).split(";")) {
                Usuario u = obtenerUsuario(nick.trim());
                if (u instanceof Dueno) {
                    asociarLocal(bar, (Dueno) u);
                }
            }
            return true;
        } catch (IllegalArgumentException e) {
            // incluye NumberFormatException y las validaciones de Direccion y Bar
            return false;
        }
    }

    private static String textoCelda(Sheet hoja, int col, int fila) {
        if (col >= hoja.getColumnCount()) {
            return "";
        }
        Object v = hoja.getValueAt(col, fila);
        return v == null ? "" : v.toString().trim();
    }

    // ------------------------------------------------------------------- XML
    /**
     * Representacion XML del sistema completo. Cada tipo de entidad se guarda
     * una sola vez, en su propia lista bajo la raiz; las relaciones entre
     * entidades se expresan mediante identificadores (ver informe del Ejercicio
     * 1).
     *
     * @return el sistema en XML
     */
    @Override
    public String toXML() {
        StringBuilder sb = new StringBuilder("<businessSystem>");
        seccionXML(sb, "usuarios", usuarios);
        seccionXML(sb, "locales", locales);
        seccionXML(sb, "reviews", reviews);
        seccionXML(sb, "contestaciones", contestaciones);
        seccionXML(sb, "reservas", reservas);
        return sb.append("</businessSystem>").toString();
    }

    private static void seccionXML(StringBuilder sb, String etiqueta, List<? extends XMLRepresentable> lista) {
        sb.append('<').append(etiqueta).append('>');
        for (XMLRepresentable x : lista) {
            sb.append(x.toXML());
        }
        sb.append("</").append(etiqueta).append('>');
    }

    /**
     * Guarda el sistema en un fichero XML (UTF-8, indentado para que sea
     * legible y editable).
     *
     * @param f fichero de destino
     * @return cierto si se ha guardado correctamente
     */
    @Override
    public boolean saveToXML(File f) {
        return f != null && XMLUtils.guardar(toXML(), f);
    }

    /**
     * @param filePath ruta del fichero de destino
     * @return cierto si se ha guardado correctamente
     * @see #saveToXML(File)
     */
    @Override
    public boolean saveToXML(String filePath) {
        return filePath != null && saveToXML(new File(filePath));
    }

    /**
     * Crea un sistema a partir de un fichero XML generado con
     * {@link #saveToXML(File)}.
     * <p>
     * Las entidades se registran mediante los metodos del propio sistema, de
     * modo que se aplican las mismas reglas de negocio que en el alta normal
     * (nicks unicos, una sola review por cliente, local y dia, como maximo
     * {@value #MAX_PROPIETARIOS} propietarios por local, etc.). Se leen en
     * orden de dependencia (usuarios, locales, propiedad, reviews,
     * contestaciones y reservas) para poder resolver las referencias por
     * identificador, independientemente del orden en el fichero.
     *
     * @param f fichero XML
     * @return el sistema leido
     * @throws XMLParsingException si el fichero no existe, no es XML bien
     * formado, le faltan datos, contiene referencias a entidades inexistentes o
     * viola alguna regla de negocio
     */
    public static BusinessSystem parseXMLFile(File f) throws XMLParsingException {
        return desdeXML(XMLParser.raiz(f));
    }

    /**
     * Crea un sistema a partir de su representacion XML en un texto.
     *
     * @param xml texto con la representacion generada por {@link #toXML()}
     * @return el sistema leido
     * @throws XMLParsingException en los mismos casos que
     * {@link #parseXMLFile(File)}
     */
    public static BusinessSystem parseXML(String xml) throws XMLParsingException {
        return desdeXML(XMLParser.raiz(xml));
    }

    /**
     * Sustituye el contenido de este sistema por el de un fichero XML. Si el
     * fichero no es valido, el sistema queda tal y como estaba.
     *
     * @param f fichero XML
     * @return cierto si se ha cargado, falso si el fichero no es valido
     */
    public boolean loadXMLFile(File f) {
        BusinessSystem leido;
        try {
            leido = parseXMLFile(f);
        } catch (XMLParsingException e) {
            System.err.println("No se pudo cargar el XML: " + e.getMessage());
            return false;
        }
        usuarios.clear();
        usuarios.addAll(leido.usuarios);
        locales.clear();
        locales.addAll(leido.locales);
        reviews.clear();
        reviews.addAll(leido.reviews);
        contestaciones.clear();
        contestaciones.addAll(leido.contestaciones);
        reservas.clear();
        reservas.addAll(leido.reservas);
        return true;
    }

    private static BusinessSystem desdeXML(Element raiz) throws XMLParsingException {
        XMLParser.comprobarEtiqueta(raiz, "businessSystem");
        BusinessSystem s = new BusinessSystem();

        Map<Dueno, List<Integer>> propiedad = new LinkedHashMap<>();
        for (Element e : seccion(raiz, "usuarios")) {
            Usuario u = XMLParser.parseUsuario(e);
            if (s.obtenerUsuario(u.getId()) != null || !s.nuevoUsuario(u)) {
                throw new XMLParsingException("Usuario repetido o no valido: " + u);
            }
            if (u instanceof Dueno) {
                propiedad.put((Dueno) u, XMLParser.referenciasLocales(e));
            }
        }
        for (Element e : seccion(raiz, "locales")) {
            Local l = XMLParser.parseLocal(e);
            if (s.obtenerLocal(l.getId()) != null || !s.nuevoLocal(l)) {
                throw new XMLParsingException("Local con identificador o direccion repetidos: " + l);
            }
        }
        for (Map.Entry<Dueno, List<Integer>> p : propiedad.entrySet()) {
            for (int ref : p.getValue()) {
                Local l = s.obtenerLocal(ref);
                if (l == null || !s.asociarLocal(l, p.getKey())) {
                    throw new XMLParsingException("No se puede asociar el local " + ref + " a "
                            + p.getKey().getNick() + " (inexistente, repetido o con demasiados propietarios).");
                }
            }
        }
        for (Element e : seccion(raiz, "reviews")) {
            Review r = XMLParser.parseReview(e);
            if (s.obtenerReview(r.getId()) != null || !s.nuevaReview(r)) {
                throw new XMLParsingException("Review repetida o con cliente/local inexistente: " + r);
            }
        }
        for (Element e : seccion(raiz, "contestaciones")) {
            Contestacion c = XMLParser.parseContestacion(e, s);
            if (!s.nuevaContestacion(c, c.getReview())) {
                throw new XMLParsingException("Contestacion no valida o repetida: " + c);
            }
        }
        for (Element e : seccion(raiz, "reservas")) {
            Reserva r = XMLParser.parseReserva(e, s);
            if (!s.registrarReserva(r)) {
                throw new XMLParsingException("Reserva repetida: " + r);
            }
        }
        return s;
    }

    private static List<Element> seccion(Element raiz, String etiqueta) throws XMLParsingException {
        Element e = XMLParser.hijoOpcional(raiz, etiqueta);
        return e == null ? List.of() : XMLParser.hijos(e);
    }

    /**
     * Registra una reserva ya existente (por ejemplo, leida de un XML). A
     * diferencia de {@link #nuevaReserva}, admite fechas pasadas, ya que una
     * reserva historica sigue siendo un dato valido.
     *
     * @param r reserva cuyo cliente y local ya estan registrados
     * @return cierto si se ha registrado; falso si se repite su identificador o
     * el cliente ya tenia reserva en ese local ese dia
     */
    private boolean registrarReserva(Reserva r) {
        for (Reserva o : reservas) {
            if (o.getId() == r.getId() || (o.getCliente().equals(r.getCliente())
                    && o.getLocal().equals(r.getLocal())
                    && o.getFecha().toLocalDate().equals(r.getFecha().toLocalDate()))) {
                return false;
            }
        }
        reservas.add(r);
        return true;
    }

    // ------------------------------------------------------------ Subclases
    /**
     * Devuelve todos los locales dados de alta, en orden de alta. Lo usan las
     * subclases que necesitan recorrer los locales sin filtrarlos por ciudad y
     * provincia (por ejemplo, el acceso remoto de la Practica 4).
     *
     * @return vista de solo lectura de los locales registrados
     */
    protected List<Local> getLocalesRegistrados() {
        return Collections.unmodifiableList(locales);
    }

    // -------------------------------------------------------------- Privados
    /**
     * Reviews de un local.
     */
    private List<Review> reviewsDe(Local l) {
        List<Review> res = new ArrayList<>();
        for (Review r : reviews) {
            if (r.getLocal() == l.getId()) {
                res.add(r);
            }
        }
        return res;
    }

    /**
     * Media de las valoraciones cuyo autor tenia entre min y max años al
     * crearse la review; 0 si no hay ninguna.
     */
    private float mediaDe(List<Review> lista, int min, int max) {
        int suma = 0, n = 0;
        for (Review r : lista) {
            Usuario autor = obtenerUsuario(r.getCliente());
            if (autor == null) {
                continue;
            }
            int edad = autor.getEdad(r.getFechaCreacion());
            if (edad >= min && edad <= max) {
                suma += r.getValoracion();
                n++;
            }
        }
        return n == 0 ? 0 : (float) suma / n;
    }

    /**
     * Ordena por valoracion media descendente (a igualdad, se mantiene el orden
     * de alta).
     */
    private <T extends Local> T[] ordenar(List<T> lista, IntFunction<T[]> crear) {
        lista.sort(Comparator.comparingDouble((T l) -> mediaDe(reviewsDe(l), 0, Integer.MAX_VALUE)).reversed());
        return lista.toArray(crear.apply(0));
    }

    /**
     * Locales de un tipo concreto situados en una ciudad y provincia.
     */
    private <T extends Local> List<T> filtrar(Class<T> tipo, String ciudad, String provincia) {
        List<T> res = new ArrayList<>();
        for (Local l : locales) {
            Direccion d = l.getUbicacion();
            if (tipo.isInstance(l) && d.getLocalidad().equalsIgnoreCase(ciudad)
                    && d.getProvincia().equalsIgnoreCase(provincia)) {
                res.add(tipo.cast(l));
            }
        }
        return res;
    }

    /**
     * Un usuario es valido si su nick tiene al menos 3 caracteres y tiene 14
     * años o mas.
     */
    private boolean usuarioValido(Usuario u) {
        return u != null && u.getNick() != null && u.getNick().trim().length() >= MIN_NICK
                && u.getEdad() >= MIN_EDAD;
    }

    /**
     * Devuelve el local almacenado en el sistema que coincide con l (misma
     * direccion y tipo), o null.
     */
    private Local localRegistrado(Local l) {
        if (l == null || l.getUbicacion() == null) {
            return null;
        }
        Local s = obtenerLocal(l.getUbicacion());
        return (s != null && s.equals(l)) ? s : null;
    }

    /**
     * Devuelve la review almacenada en el sistema que coincide con r (mismo
     * cliente, local y fecha), o null.
     */
    private Review reviewRegistrada(Review r) {
        if (r == null) {
            return null;
        }
        int i = reviews.indexOf(r);
        return i < 0 ? null : reviews.get(i);
    }

    /**
     * Comprueba si un dueño es propietario de un local.
     */
    private boolean esPropietario(Dueno d, Local l) {
        return d.getLocales().contains(l);
    }

    /**
     * Cuenta cuantos usuarios registrados son propietarios de un local.
     */
    private int contarPropietarios(Local l) {
        int n = 0;
        for (Usuario u : usuarios) {
            if (u instanceof Dueno && esPropietario((Dueno) u, l)) {
                n++;
            }
        }
        return n;
    }

    /**
     * Elimina una review junto con su contestacion.
     */
    private void borrarReview(Review r) {
        contestaciones.removeIf(c -> c.getReview().equals(r));
        reviews.remove(r);
        Local l = obtenerLocal(r.getLocal());
        if (l != null) {
            l.eliminarReview(r);
        }
    }

    /**
     * Sustituye en reviews, contestaciones y reservas las referencias a un
     * usuario y/o local por sus nuevas versiones. Lo que no pueda mantenerse
     * (p. ej. las reviews de un Cliente reemplazado por un Dueño, o las
     * reservas de un local que deja de ser Reservable) se elimina.
     */
    private void reconstruir(Usuario uViejo, Usuario uNuevo, Local lViejo, Local lNuevo) {
        Map<Review, Review> mapa = new IdentityHashMap<>();
        List<Review> antiguas = new ArrayList<>(reviews);
        reviews.clear();
        for (Review r : antiguas) {
            boolean cambiaU = uViejo != null && r.getCliente() == uViejo.getId();
            boolean cambiaL = lViejo != null && r.getLocal() == lViejo.getId();
            if (!cambiaU && !cambiaL) {
                reviews.add(r);
                mapa.put(r, r);
                continue;
            }
            Local anterior = obtenerLocal(r.getLocal());
            if (anterior != null) {
                anterior.eliminarReview(r);
            }
            Usuario c = !cambiaU ? obtenerUsuario(r.getCliente()) : uNuevo;
            Local l = cambiaL ? lNuevo : anterior;
            if (!(c instanceof Cliente) || l == null) {
                continue;
            }
            Review nueva = new Review(r.getId(), c.getId(), l.getId(), r.getValoracion(), r.getComentario(),
                    r.getFechaVisita(), r.getFechaCreacion());
            reviews.add(nueva);
            l.anadirReview(nueva);
            mapa.put(r, nueva);
        }
        List<Contestacion> antiguasC = new ArrayList<>(contestaciones);
        contestaciones.clear();
        for (Contestacion c : antiguasC) {
            Review nr = mapa.get(c.getReview());
            Dueno autor = c.getAutor();
            if (uViejo != null && autor.equals(uViejo)) {
                autor = (uNuevo instanceof Dueno) ? (Dueno) uNuevo : null;
            }
            if (nr == null || autor == null) {
                continue;
            }
            contestaciones.add((nr == c.getReview() && autor == c.getAutor())
                    ? c : new Contestacion(c.getComentario(), nr, autor));
        }
        List<Reserva> antiguasR = new ArrayList<>(reservas);
        reservas.clear();
        for (Reserva r : antiguasR) {
            boolean cambiaU = uViejo != null && r.getCliente().equals(uViejo);
            boolean cambiaL = lViejo != null && r.getLocal().equals(lViejo);
            if (!cambiaU && !cambiaL) {
                reservas.add(r);
                continue;
            }
            Cliente c = !cambiaU ? r.getCliente() : (uNuevo instanceof Cliente ? (Cliente) uNuevo : null);
            Reservable l = cambiaL ? (lNuevo instanceof Reservable ? (Reservable) lNuevo : null) : r.getLocal();
            if (c != null && l != null) {
                reservas.add(new Reserva(r.getFecha(), r.getDescuento(), c, l));
            }
        }
    }
}
