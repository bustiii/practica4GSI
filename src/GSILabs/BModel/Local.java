package GSILabs.BModel;

import GSILabs.serializable.XMLRepresentable;
import GSILabs.serializable.XMLUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Local de ocio (C01). Se identifica por su direccion: no puede haber dos
 * locales del mismo tipo en la misma. Ademas cada instancia recibe un
 * identificador unico.
 *
 *
 */
public class Local implements XMLRepresentable {

    /**
     * Longitud maxima de la descripcion.
     */
    public static final int DESCRIPCION_MAX = 300;

    private static final AtomicInteger SIGUIENTE_ID = new AtomicInteger(1);

    /**
     * Identificador unico e inmutable.
     */
    protected final int id;
    /**
     * Nombre del local.
     */
    protected String nombre;
    /**
     * Direccion del local.
     */
    protected Direccion ubicacion;
    /**
     * Descripcion breve (maximo 300 caracteres).
     */
    protected String descripcion;
    /**
     * Reviews recibidas.
     */
    protected ArrayList<Review> listaReviews;

    /**
     * Crea un local sin descripcion ni reviews.
     *
     * @param nombre nombre, no vacio
     * @param ubicacion direccion, no nula
     * @throws IllegalArgumentException si el nombre es nulo o vacio, o la
     * direccion es nula
     */
    public Local(String nombre, Direccion ubicacion) {
        this(SIGUIENTE_ID.get(), nombre, ubicacion);
    }

    /**
     * Crea un local con un identificador concreto. Se usa al reconstruir
     * locales desde XML para conservar las referencias por identificador.
     *
     * @param id identificador, positivo
     * @param nombre nombre, no vacio
     * @param ubicacion direccion, no nula
     * @throws IllegalArgumentException si el identificador no es positivo, el
     * nombre es nulo o vacio, o la direccion es nula
     */
    public Local(int id, String nombre, Direccion ubicacion) {
        if (id <= 0) {
            throw new IllegalArgumentException("El identificador debe ser positivo.");
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del local no puede ser nulo ni vacio.");
        }
        if (ubicacion == null) {
            throw new IllegalArgumentException("La direccion del local no puede ser nula.");
        }
        SIGUIENTE_ID.accumulateAndGet(id + 1, Math::max);
        this.id = id;
        this.nombre = nombre;
        this.ubicacion = ubicacion;
        this.listaReviews = new ArrayList<>();
    }

    /**
     * @return el identificador unico del local
     */
    public int getId() {
        return id;
    }

    /**
     * @return el nombre
     */
    public String getNombre() {
        return this.nombre;
    }

    /**
     * @return la direccion
     */
    public Direccion getUbicacion() {
        return ubicacion;
    }

    /**
     * @return la descripcion, o null si no tiene
     */
    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Establece la descripcion breve del local.
     *
     * @param descripcion descripcion de como maximo 300 caracteres, o null para
     * borrarla
     * @throws IllegalArgumentException si excede los 300 caracteres
     */
    public void setDescripcion(String descripcion) {
        if (descripcion != null && descripcion.length() > DESCRIPCION_MAX) {
            throw new IllegalArgumentException("La descripcion solo puede tener " + DESCRIPCION_MAX + " caracteres");
        }
        this.descripcion = descripcion;
    }

    /**
     * @return vista de solo lectura de las reviews del local
     */
    public List<Review> getReviews() {
        return Collections.unmodifiableList(listaReviews);
    }

    /**
     * Guarda una review de este local. Si ya estaba guardada no se duplica.
     *
     * @param review review a guardar
     * @throws IllegalArgumentException si es nula o pertenece a otro local
     */
    public void anadirReview(Review review) {
        if (review == null || review.getLocal() != this.id) {
            throw new IllegalArgumentException("La review debe ser de este local.");
        }
        if (!this.listaReviews.contains(review)) {
            this.listaReviews.add(review);
        }
    }

    /**
     * Retira una review de este local.
     *
     * @param review review a retirar (si no estaba no ocurre nada)
     */
    public void eliminarReview(Review review) {
        this.listaReviews.remove(review);
    }

    /**
     * @return texto con el nombre y el numero de reviews, para mostrar al
     * cliente
     */
    public String obtenerInformacion() {
        return "Local: " + this.nombre + " | Total de reviews: " + listaReviews.size();
    }

    /**
     * Dos locales son iguales si son del mismo tipo y estan en la misma
     * direccion.
     *
     * @param obj objeto a comparar
     * @return cierto si son iguales
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return Objects.equals(this.ubicacion, ((Local) obj).ubicacion);
    }

    /**
     * @return hash basado en la direccion; las subclases lo reutilizan para no
     * depender de campos mutables
     */
    @Override
    public int hashCode() {
        return Objects.hash(ubicacion);
    }

    /**
     * @return nombre y direccion del local
     */
    @Override
    public String toString() {
        return "Local: " + nombre + " | " + ubicacion;
    }

    /**
     * Representacion XML del local. La direccion se incrusta (es parte del
     * local); las reviews no, porque son entidades independientes que
     * referencian al local por su identificador.
     *
     * @return el local en XML
     */
    @Override
    public String toXML() {
        return "<local id=\"" + id + "\">" + camposXML() + "</local>";
    }

    /**
     * @return los campos comunes a todo local en formato XML, para que las
     * subclases los reutilicen
     */
    protected String camposXML() {
        return XMLUtils.elemento("nombre", nombre)
                + XMLUtils.elemento("descripcion", descripcion)
                + ubicacion.toXML();
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
