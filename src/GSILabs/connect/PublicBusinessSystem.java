package GSILabs.connect;

import GSILabs.BModel.Bar;
import GSILabs.BModel.Cliente;
import GSILabs.BModel.Local;
import GSILabs.BModel.Restaurante;
import GSILabs.BModel.Review;
import GSILabs.BSystem.BusinessSystem;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Sistema de negocio accesible de forma remota (Practica 4, opcion b del
 * Ejercicio 1). Extiende {@link BusinessSystem}, de modo que conserva toda la
 * logica y la persistencia de las practicas anteriores, y anade la
 * implementacion de las interfaces remotas {@link LocalFinder},
 * {@link ClientGateway} y {@link AdminGateway}.
 * <p>
 * Un mismo objeto se publica en el registro RMI bajo dos identificadores (uno
 * por pasarela); cada cliente solo ve los metodos de la interfaz con la que lo
 * localiza. La clase no extiende UnicastRemoteObject: el servidor la exporta
 * con {@code UnicastRemoteObject.exportObject} (issue #5).
 * <p>
 * Los metodos se ejecutan en el servidor y no lanzan RemoteException; esa
 * excepcion la genera RMI en el lado cliente si falla la comunicacion.
 * <p>
 * RMI puede atender a varios clientes a la vez en hilos distintos y las
 * colecciones de {@link BusinessSystem} no son seguras para hilos, por lo que
 * todos los metodos remotos son {@code synchronized}: las invocaciones sobre
 * un mismo sistema se ejecutan de una en una.
 */
public class PublicBusinessSystem extends BusinessSystem implements ClientGateway, AdminGateway {

    /**
     * Prefijo del nick de los clientes falsos creados por
     * {@link #insertaReviewFalsa(Local, Integer)}.
     */
    private static final String PREFIJO_FALSO = "usuario";
    /**
     * Edad con la que se crean los clientes falsos (cumple la edad minima).
     */
    private static final int EDAD_FALSO = 30;
    /**
     * Intentos maximos para encontrar un nick libre para un cliente falso.
     */
    private static final int INTENTOS_FALSO = 1000;

    /**
     * Ultimo numero usado en el nick de un cliente falso.
     */
    private int contadorFalsos = 0;

    /**
     * Crea un sistema vacio.
     */
    public PublicBusinessSystem() {
        super();
    }

    // ---------------------------------------------------------- LocalFinder
    /**
     * Devuelve el local cuyo nombre coincide exactamente con el indicado, sin
     * distinguir mayusculas ni espacios: "Casa  Pepe", "casapepe" y "CASA PEPE"
     * se consideran el mismo nombre. Si hay varios locales con ese nombre (en
     * direcciones distintas), devuelve el primero que se dio de alta.
     *
     * @param name nombre del local
     * @return el local, o null si no existe o el nombre es nulo
     */
    @Override
    public synchronized Local getLocal(String name) {
        if (name == null) {
            return null;
        }
        String buscado = normalizar(name);
        for (Local l : getLocalesRegistrados()) {
            if (normalizar(l.getNombre()).equals(buscado)) {
                return l;
            }
        }
        return null;
    }

    /**
     * Devuelve los locales cuyo nombre contiene, total o parcialmente, el
     * texto indicado, sin distinguir mayusculas ni espacios. Un texto vacio
     * (o solo con espacios) coincide con todos los locales.
     *
     * @param name nombre completo o parcial
     * @return los locales encontrados, en orden de alta (array vacio si no hay
     * ninguno o el texto es nulo)
     */
    @Override
    public synchronized Local[] getLocals(String name) {
        if (name == null) {
            return new Local[0];
        }
        String buscado = normalizar(name);
        List<Local> res = new ArrayList<>();
        for (Local l : getLocalesRegistrados()) {
            if (normalizar(l.getNombre()).contains(buscado)) {
                res.add(l);
            }
        }
        return res.toArray(new Local[0]);
    }

    // -------------------------------------------------------- ClientGateway
    /**
     * Incorpora una review si su cliente es un cliente registrado, su local
     * existe y no hay otra review del mismo cliente para el mismo local en la
     * misma fecha de visita.
     * <p>
     * La review llega deserializada desde el cliente, sin pasar por las
     * validaciones del constructor, asi que no se guarda tal cual: se crea una
     * copia con el constructor de {@link Review}, que rechaza valoraciones
     * fuera de rango, comentarios demasiado largos o fechas de visita futuras.
     * La fecha de creacion de la copia es la del servidor (el dia en que entra
     * en el sistema). Se conserva el identificador recibido salvo que ya lo
     * use otra review del servidor, en cuyo caso se asigna uno nuevo.
     *
     * @param r review a introducir
     * @return true si y solo si se introdujo
     */
    @Override
    public synchronized boolean insertaReview(Review r) {
        if (r == null) {
            return false;
        }
        Review copia;
        try {
            if (r.getId() > 0 && obtenerReview(r.getId()) == null) {
                copia = new Review(r.getId(), r.getCliente(), r.getLocal(), r.getValoracion(),
                        r.getComentario(), r.getFechaVisita(), LocalDate.now());
            } else {
                copia = new Review(r.getCliente(), r.getLocal(), r.getValoracion(), r.getComentario(),
                        r.getFechaVisita());
            }
        } catch (IllegalArgumentException e) {
            return false;
        }
        return nuevaReview(copia);
    }

    /**
     * Elimina una review si existe y no tiene contestacion asociada. La review
     * se busca por su cliente, su local y su fecha de visita (igualdad de
     * {@link Review}), asi que vale la copia que tiene el cliente aunque el
     * servidor le asignara otro identificador al insertarla.
     * <p>
     * Usa la regla original de {@link BusinessSystem#eliminaReview(Review)},
     * que esta clase sobrescribe con el borrado de administrador.
     *
     * @param r review a eliminar
     * @return true si y solo si se elimino
     */
    @Override
    public synchronized boolean quitaReview(Review r) {
        return super.eliminaReview(r);
    }

    /**
     * Devuelve el bar con mejor puntuacion media de una ciudad. La ciudad se
     * compara sin distinguir mayusculas ni espacios y en cualquier provincia.
     * Los bares sin reviews quedan por detras de los que tienen alguna; a
     * igualdad de puntuacion gana el que se dio de alta antes.
     *
     * @param ciudad nombre de la ciudad
     * @return el mejor bar, o null si la ciudad no tiene bares o es nula
     */
    @Override
    public synchronized Bar mejorBar(String ciudad) {
        List<Bar> bares = ordenadosPorPuntuacion(Bar.class, ciudad);
        return bares.isEmpty() ? null : bares.get(0);
    }

    /**
     * Devuelve los {@code num} restaurantes de una ciudad con mejor puntuacion
     * media, ordenados de mejor a peor con el mismo criterio que
     * {@link #mejorBar(String)}.
     *
     * @param ciudad nombre de la ciudad
     * @param num numero de restaurantes pedidos
     * @return array de tamano {@code num}, con null en las posiciones que no
     * se puedan cubrir; array vacio si {@code num} es nulo o no es positivo
     */
    @Override
    public synchronized Restaurante[] mejoresRestaurantes(String ciudad, Integer num) {
        if (num == null || num <= 0) {
            return new Restaurante[0];
        }
        Restaurante[] res = new Restaurante[num];
        List<Restaurante> ordenados = ordenadosPorPuntuacion(Restaurante.class, ciudad);
        for (int i = 0; i < res.length && i < ordenados.size(); i++) {
            res[i] = ordenados.get(i);
        }
        return res;
    }

    // --------------------------------------------------------- AdminGateway
    /**
     * Elimina un local del sistema.
     *
     * @param l local a eliminar
     * @return true si y solo si se elimino
     */
    @Override
    public synchronized Boolean eliminaLocal(Local l) {
        // TODO issue #4: implementar el borrado
        return false;
    }

    /**
     * Elimina todas las reviews de un local y sus contestaciones.
     *
     * @param l local cuyas reviews se eliminan
     * @return true si y solo si se completo la tarea
     */
    @Override
    public synchronized Boolean eliminaReviewsDeLocal(Local l) {
        // TODO issue #4: implementar el borrado
        return false;
    }

    /**
     * Elimina una review como administrador, aunque tenga contestacion (que se
     * elimina con ella).
     * <p>
     * Sobrescribe {@link BusinessSystem#eliminaReview(Review)}, que rechaza
     * las reviews contestadas; ese comportamiento sigue disponible para los
     * clientes mediante {@link #quitaReview(Review)}.
     *
     * @param r review a eliminar
     * @return true si y solo si se elimino
     */
    @Override
    public synchronized boolean eliminaReview(Review r) {
        // TODO issue #4: implementar el borrado de administrador
        return false;
    }

    /**
     * Elimina todas las reviews de un cliente.
     *
     * @param c cliente cuyas reviews se eliminan
     * @return numero de reviews eliminadas, o -1 si el cliente no existe
     */
    @Override
    public synchronized Integer eliminaReviewsDeUsuario(Cliente c) {
        // TODO issue #4: implementar el borrado
        return -1;
    }

    /**
     * Crea un cliente falso valido y le anade una review con la puntuacion
     * indicada sobre el local dado, con fecha de visita de hoy y sin
     * comentario.
     * <p>
     * El cliente falso tiene un nick libre de la forma {@code usuarioN}, una
     * contrasena aleatoria y {@value #EDAD_FALSO} anos. Si la review no se
     * puede insertar, el cliente falso se elimina para no dejar usuarios
     * sueltos.
     *
     * @param l local que recibe la review (debe estar registrado)
     * @param puntuacion puntuacion de la review, entre
     * {@value GSILabs.BModel.Review#VALORACION_MIN} y
     * {@value GSILabs.BModel.Review#VALORACION_MAX}
     * @return true si y solo si se creo el cliente falso y su review
     */
    @Override
    public synchronized Boolean insertaReviewFalsa(Local l, Integer puntuacion) {
        if (puntuacion == null || puntuacion < Review.VALORACION_MIN || puntuacion > Review.VALORACION_MAX) {
            return false;
        }
        Local registrado = localRegistrado(l);
        if (registrado == null) {
            return false;
        }
        Cliente falso = crearClienteFalso();
        if (falso == null) {
            return false;
        }
        Review r = new Review(falso.getId(), registrado.getId(), puntuacion, "", LocalDate.now());
        if (!nuevaReview(r)) {
            eliminaUsuario(falso);
            return false;
        }
        return true;
    }

    // ------------------------------------------------------------ Auxiliares
    /**
     * Normaliza un texto para compararlo sin distinguir mayusculas ni espacios:
     * elimina todos los espacios en blanco y lo pasa a minusculas.
     *
     * @param texto texto a normalizar
     * @return el texto normalizado
     */
    private static String normalizar(String texto) {
        return texto.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    /**
     * Busca en el sistema el local equivalente al recibido (mismo tipo y misma
     * direccion). El objeto recibido por RMI es una copia, asi que hay que
     * trabajar con el que guarda el servidor.
     *
     * @param l local recibido
     * @return el local registrado, o null si no existe o l es nulo
     */
    private Local localRegistrado(Local l) {
        if (l == null) {
            return null;
        }
        Local s = obtenerLocal(l.getUbicacion());
        return (s != null && s.equals(l)) ? s : null;
    }

    /**
     * Crea y da de alta un cliente falso con un nick libre.
     *
     * @return el cliente creado, o null si no se encontro un nick libre
     */
    private Cliente crearClienteFalso() {
        for (int i = 0; i < INTENTOS_FALSO; i++) {
            String nick = PREFIJO_FALSO + (++contadorFalsos);
            if (!existeNick(nick)) {
                Cliente c = new Cliente(nick, UUID.randomUUID().toString(),
                        LocalDate.now().minusYears(EDAD_FALSO));
                if (nuevoUsuario(c)) {
                    return c;
                }
            }
        }
        return null;
    }

    /**
     * Locales de un tipo situados en una ciudad, ordenados de mayor a menor
     * puntuacion media. Los que no tienen reviews van al final y, a igualdad,
     * se mantiene el orden de alta.
     *
     * @param <T> tipo de local
     * @param tipo clase del tipo de local buscado
     * @param ciudad ciudad (sin distinguir mayusculas ni espacios)
     * @return lista ordenada, vacia si no hay ninguno o la ciudad es nula
     */
    private <T extends Local> List<T> ordenadosPorPuntuacion(Class<T> tipo, String ciudad) {
        List<T> res = new ArrayList<>();
        if (ciudad == null) {
            return res;
        }
        String buscada = normalizar(ciudad);
        for (Local l : getLocalesRegistrados()) {
            if (tipo.isInstance(l) && normalizar(l.getUbicacion().getLocalidad()).equals(buscada)) {
                res.add(tipo.cast(l));
            }
        }
        res.sort(Comparator.comparing((T l) -> verReviews(l).length == 0)
                .thenComparing(Comparator.comparingDouble((T l) -> obtenerValoracionMedia(l)).reversed()));
        return res;
    }
}
