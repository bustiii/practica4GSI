package GSILabs.BModel;

import GSILabs.serializable.XMLRepresentable;
import GSILabs.serializable.XMLUtils;
import java.io.File;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Respuesta de un Dueño a una Review de uno de sus locales.
 */
public class Contestacion implements XMLRepresentable {

    private String comentario;
    private final LocalDate fecha;
    private final Review review;
    private final Dueno autor;

    /**
     * @param comentario Texto de la contestacion (maximo 500 caracteres).
     * @param review Review que se contesta.
     * @param autor Dueño que contesta.
     * @throws IllegalArgumentException si algun dato es nulo, el comentario
     * excede 500 caracteres o el autor no es propietario del local de la review
     * (C07).
     */
    public Contestacion(String comentario, Review review, Dueno autor) {
        this(comentario, review, autor, LocalDate.now());
    }

    /**
     * Crea una contestacion con una fecha concreta (reconstruccion desde XML).
     *
     * @param comentario texto de hasta 500 caracteres
     * @param review review contestada
     * @param autor propietario del local de la review
     * @param fecha fecha de la contestacion, no anterior a la de la review
     * @throws IllegalArgumentException si algun dato no es valido o el autor no
     * es propietario del local
     */
    public Contestacion(String comentario, Review review, Dueno autor, LocalDate fecha) {
        if (review == null || autor == null) {
            throw new IllegalArgumentException("La review y el autor no pueden ser nulos.");
        }
        validar(comentario);
        if (autor.getLocales().stream().noneMatch(l -> l.getId() == review.getLocal())) {
            throw new IllegalArgumentException("Solo el propietario del local puede contestar a sus reviews.");
        }
        this.comentario = comentario;
        this.review = review;
        this.autor = autor;
        if (fecha == null || fecha.isBefore(review.getFechaCreacion())) {
            throw new IllegalArgumentException("La fecha no puede ser nula ni anterior a la review.");
        }
        this.fecha = fecha;
    }

    private static void validar(String comentario) {
        if (comentario == null || comentario.length() > 500) {
            throw new IllegalArgumentException("La contestación no puede exceder los 500 caracteres.");
        }
    }

    /**
     * @param comentario nuevo comentario de hasta 500 caracteres
     * @throws IllegalArgumentException si es nulo o excede los 500 caracteres
     */
    public void setComentario(String comentario) {
        validar(comentario);
        this.comentario = comentario;
    }

    /**
     * @return el comentario
     */
    public String getComentario() {
        return comentario;
    }

    /**
     * @return la fecha en que se contesto
     */
    public LocalDate getFecha() {
        return fecha;
    }

    /**
     * @return la review contestada
     */
    public Review getReview() {
        return review;
    }

    /**
     * @return el propietario que contesta
     */
    public Dueno getAutor() {
        return autor;
    }

    /**
     * Una review solo puede tener una contestacion, asi que se identifica por
     * su review.
     */
    /**
     * @param obj objeto a comparar
     * @return cierto si contestan a la misma review
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return Objects.equals(review, ((Contestacion) obj).review);
    }

    /**
     * @return hash coherente con {@link #equals(Object)}
     */
    @Override
    public int hashCode() {
        return Objects.hash(review);
    }

    /**
     * @return descripcion de la contestacion
     */
    @Override
    public String toString() {
        return "Contestacion{autor=" + autor.getNick() + ", fecha=" + fecha + ", comentario='" + comentario + "'}";
    }

    /**
     * Representacion XML de la contestacion. La review y el autor se
     * referencian por su identificador.
     *
     * @return la contestacion en XML
     */
    @Override
    public String toXML() {
        return "<contestacion review=\"" + review.getId() + "\" autor=\"" + autor.getId() + "\">"
                + XMLUtils.elemento("comentario", comentario)
                + XMLUtils.elemento("fecha", fecha)
                + "</contestacion>";
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
