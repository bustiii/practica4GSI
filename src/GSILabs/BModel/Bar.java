package GSILabs.BModel;

import GSILabs.serializable.XMLRepresentable;
import GSILabs.serializable.XMLUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Bar (C02): local reservable con una lista de especialidades almacenadas como
 * tags.
 *
 * @author Oihan
 */
public class Bar extends Local implements Reservable, XMLRepresentable {

    private List<String> especialidades;

    /**
     * Crea un bar sin especialidades.
     *
     * @param nombre nombre, no vacio
     * @param ubicacion direccion, no nula
     * @throws IllegalArgumentException si el nombre o la direccion no son
     * validos
     */
    public Bar(String nombre, Direccion ubicacion) {
        super(nombre, ubicacion);
        this.especialidades = new ArrayList<>();
    }

    /**
     * @return una copia de la lista de especialidades
     */
    public List<String> getEspecialidades() {
        return new ArrayList<>(especialidades);
    }

    /**
     * Sustituye las especialidades.
     *
     * @param especialidades nueva lista, no nula
     * @throws IllegalArgumentException si la lista es nula
     */
    public void setEspecialidades(List<String> especialidades) {
        if (especialidades == null) {
            throw new IllegalArgumentException("La lista de especialidades no puede ser nula.");
        }
        this.especialidades = new ArrayList<>(especialidades);
    }

    /**
     * Anade una especialidad si no es vacia y no estaba ya.
     *
     * @param tag especialidad a anadir
     */
    public void addEspecialidad(String tag) {
        if (tag != null && !tag.trim().isEmpty() && !especialidades.contains(tag)) {
            this.especialidades.add(tag);
        }
    }

    /**
     * @return nombre, direccion y especialidades del bar
     */
    @Override
    public String toString() {
        return "Bar: " + nombre + " | " + ubicacion + " | Especialidades: " + especialidades;
    }

    /**
     * @param obj objeto a comparar
     * @return cierto si es un bar en la misma direccion con las mismas
     * especialidades
     */
    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        return Objects.equals(this.especialidades, ((Bar) obj).especialidades);
    }

    /**
     * @return hash basado solo en la direccion (las especialidades son
     * mutables)
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Crea un bar sin especialidades con un identificador concreto
     * (reconstruccion desde XML).
     *
     * @param id identificador, positivo
     * @param nombre nombre del bar
     * @param ubicacion direccion del bar
     * @throws IllegalArgumentException si los datos no cumplen las reglas de
     * {@link Local}
     */
    public Bar(int id, String nombre, Direccion ubicacion) {
        super(id, nombre, ubicacion);
        this.especialidades = new ArrayList<>();
    }

    /**
     * @return el bar en XML, con sus especialidades
     */
    @Override
    public String toXML() {
        StringBuilder sb = new StringBuilder("<bar id=\"" + id + "\">");
        sb.append(camposXML()).append("<especialidades>");
        for (String e : especialidades) {
            sb.append(XMLUtils.elemento("especialidad", e));
        }
        return sb.append("</especialidades></bar>").toString();
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
