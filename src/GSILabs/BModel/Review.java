package GSILabs.BModel;

import GSILabs.serializable.XMLRepresentable;
import GSILabs.serializable.XMLUtils;
import java.io.File;
import java.time.LocalDate;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Opinion dejada por un {@link Cliente} sobre un {@link Local} (C05, C08). Un
 * cliente no puede valorar dos veces la misma visita, por lo que dos reviews
 * son iguales si coinciden cliente, local y fecha de visita.
 *
 *
 */
public class Review implements XMLRepresentable {

    /**
     * Valoracion minima.
     */
    public static final int VALORACION_MIN = 0;
    /**
     * Valoracion maxima.
     */
    public static final int VALORACION_MAX = 5;
    /**
     * Longitud maxima del comentario.
     */
    public static final int COMENTARIO_MAX = 500;

    private static final AtomicInteger SIGUIENTE_ID = new AtomicInteger(1);

    private final int id;
    private final int cliente;
    private final int local;
    private int valoracion;
    private String comentario;
    private final LocalDate fechaVisita;
    private final LocalDate fechaCreacion;

    /**
     * Crea una review validando las reglas de negocio. La fecha de creacion es
     * la actual.
     *
     * @param cliente autor, no nulo
     * @param local local valorado, no nulo
     * @param valoracion estrellas, de 0 a 5
     * @param comentario comentario de hasta 500 caracteres (null se guarda como
     * vacio)
     * @param fechaVisita fecha de la visita, no nula y no futura
     * @throws IllegalArgumentException si algun dato no cumple las reglas
     * anteriores
     */
    public Review(int cliente, int local, int valoracion, String comentario, LocalDate fechaVisita) {
        this(SIGUIENTE_ID.get(), cliente, local, valoracion, comentario, fechaVisita, LocalDate.now());
    }

    /**
     * Crea una review con identificador y fecha de creacion concretos. Se usa
     * al reconstruir reviews desde XML, conservando todos sus datos.
     *
     * @param id identificador, positivo
     * @param cliente identificador del cliente autor
     * @param local identificador del local valorado
     * @param valoracion estrellas, de 0 a 5
     * @param comentario comentario de hasta 500 caracteres
     * @param fechaVisita fecha de la visita, no futura
     * @param fechaCreacion fecha de creacion, no anterior a la visita
     * @throws IllegalArgumentException si algun dato no es valido
     */
    public Review(int id, int cliente, int local, int valoracion, String comentario,
            LocalDate fechaVisita, LocalDate fechaCreacion) {
        if (id <= 0) {
            throw new IllegalArgumentException("El identificador debe ser positivo.");
        }
        if (fechaCreacion == null || (fechaVisita != null && fechaCreacion.isBefore(fechaVisita))) {
            throw new IllegalArgumentException("La fecha de creacion no puede ser nula ni anterior a la visita.");
        }
        validarValoracion(valoracion);
        validarComentario(comentario);
        if (fechaVisita == null) {
            throw new IllegalArgumentException("La fecha de visita no puede ser nula.");
        }
        if (fechaVisita.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de visita no puede ser en el futuro.");
        }
        SIGUIENTE_ID.accumulateAndGet(id + 1, Math::max);
        this.id = id;
        this.cliente = cliente;
        this.local = local;
        this.valoracion = valoracion;
        this.comentario = (comentario == null) ? "" : comentario;
        this.fechaVisita = fechaVisita;
        this.fechaCreacion = fechaCreacion;
    }

    private static void validarValoracion(int valoracion) {
        if (valoracion < VALORACION_MIN || valoracion > VALORACION_MAX) {
            throw new IllegalArgumentException("La valoración debe estar entre 0 y 5 estrellas.");
        }
    }

    private static void validarComentario(String comentario) {
        if (comentario != null && comentario.length() > COMENTARIO_MAX) {
            throw new IllegalArgumentException("El comentario no puede superar los 500 caracteres.");
        }
    }

    /**
     * @return el identificador unico de la review
     */
    public int getId() {
        return id;
    }

    /**
     * @return el cliente autor
     */
    public int getCliente() {
        return cliente;
    }

    /**
     * @return el local valorado
     */
    public int getLocal() {
        return local;
    }

    /**
     * @return las estrellas, de 0 a 5
     */
    public int getValoracion() {
        return valoracion;
    }

    /**
     * @param valoracion nueva valoracion, de 0 a 5
     * @throws IllegalArgumentException si esta fuera de rango
     */
    public void setValoracion(int valoracion) {
        validarValoracion(valoracion);
        this.valoracion = valoracion;
    }

    /**
     * @return el comentario (nunca null)
     */
    public String getComentario() {
        return comentario;
    }

    /**
     * @param comentario nuevo comentario de hasta 500 caracteres (null se
     * guarda como vacio)
     * @throws IllegalArgumentException si excede los 500 caracteres
     */
    public void setComentario(String comentario) {
        validarComentario(comentario);
        this.comentario = (comentario == null) ? "" : comentario;
    }

    /**
     * @return la fecha de la visita
     */
    public LocalDate getFechaVisita() {
        return fechaVisita;
    }

    /**
     * @return la fecha en que se creo la review
     */
    public LocalDate getFechaCreacion() {
        return fechaCreacion;
    }

    /**
     * @param o objeto a comparar
     * @return cierto si es una review del mismo cliente, local y fecha de
     * visita
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Review review = (Review) o;
        return Objects.equals(cliente, review.cliente)
                && Objects.equals(local, review.local)
                && Objects.equals(fechaVisita, review.fechaVisita);
    }

    /**
     * @return hash coherente con {@link #equals(Object)}
     */
    @Override
    public int hashCode() {
        return Objects.hash(cliente, local, fechaVisita);
    }

    /**
     * @return descripcion de la review
     */
    @Override
    public String toString() {
        return "Review{id=" + id + ", cliente=" + cliente + ", local=" + local
                + ", valoracion=" + valoracion + ", fechaVisita=" + fechaVisita
                + ", fechaCreacion=" + fechaCreacion + '}';
    }

    /**
     * Representacion XML de la review. El cliente y el local no se incrustan:
     * se referencian por su identificador (atributos {@code cliente} y
     * {@code local}).
     *
     * @return la review en XML
     */
    @Override
    public String toXML() {
        return "<review id=\"" + id + "\" cliente=\"" + cliente + "\" local=\"" + local + "\">"
                + XMLUtils.elemento("valoracion", valoracion)
                + XMLUtils.elemento("comentario", comentario)
                + XMLUtils.elemento("fechaVisita", fechaVisita)
                + XMLUtils.elemento("fechaCreacion", fechaCreacion)
                + "</review>";
    }

    /**
     * Guarda la representacion XML en un fichero (UTF-8, indentado).
     *
     * @param f fichero de destino
     * @return cierto si se ha guardado correctamente
     */
    @Override
    public boolean saveToXML(File f) {
        return f != null && XMLUtils.guardar(this.toXML(), f);
    }

    /**
     * @param filePath ruta del fichero de destino
     * @return cierto si se ha guardado correctamente
     */
    @Override
    public boolean saveToXML(String filePath) {
        return filePath != null && saveToXML(new File(filePath));
    }
}
