package GSILabs.BModel;

import GSILabs.serializable.XMLRepresentable;
import GSILabs.serializable.XMLUtils;
import java.io.File;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Usuario con perfil de Propietario (C06): puede poseer cualquier numero de
 * locales y contestar a las reviews de estos (C07).
 *
 *
 */
public class Dueno extends Usuario implements XMLRepresentable {

    /**
     * Version de la forma serializada (la clase viaja por RMI).
     */
    private static final long serialVersionUID = 1L;

    private final HashSet<Local> locales;

    /**
     * Crea un propietario sin locales.
     *
     * @param nick nick unico de al menos 3 caracteres
     * @param password contrasena
     * @param fechaNacimiento fecha de nacimiento (minimo 14 anios)
     * @throws IllegalArgumentException si los datos no cumplen las reglas de
     * {@link Usuario}
     */
    public Dueno(String nick, String password, LocalDate fechaNacimiento) {
        super(nick, password, fechaNacimiento, Perfil.PROPIETARIO);
        this.locales = new HashSet<>();
    }

    /**
     * @return una copia del conjunto de locales que posee el dueño
     */
    public Set<Local> getLocales() {
        return new HashSet<>(this.locales);
    }

    /**
     * Asocia un local a este propietario.
     *
     * @param local local a asociar
     * @throws IllegalArgumentException si el local es nulo
     */
    public void anadirLocal(Local local) {
        if (local == null) {
            throw new IllegalArgumentException("El local no puede ser nulo.");
        }
        this.locales.add(local);
    }

    /**
     * Desvincula un local de este propietario.
     *
     * @param local local a desvincular (si no estaba asociado no ocurre nada)
     */
    public void eliminarLocal(Local local) {
        this.locales.remove(local);
    }

    /**
     * @return descripcion con id, nick y numero de locales
     */
    @Override
    public String toString() {
        return "PROPIETARIO{id=" + id + ", nick='" + nick + "', numLocales=" + locales.size() + '}';
    }

    /**
     * Crea un propietario sin locales con un identificador concreto
     * (reconstruccion desde XML).
     *
     * @param id identificador, positivo
     * @param nick nick unico de al menos 3 caracteres
     * @param password contrasena
     * @param fechaNacimiento fecha de nacimiento (minimo 14 anios)
     * @throws IllegalArgumentException si los datos no cumplen las reglas de
     * {@link Usuario}
     */
    public Dueno(int id, String nick, String password, LocalDate fechaNacimiento) {
        super(id, nick, password, fechaNacimiento, Perfil.PROPIETARIO);
        this.locales = new HashSet<>();
    }

    /**
     * Representacion XML del propietario. Los locales que posee no se
     * incrustan: se referencian por su identificador.
     *
     * @return el propietario en XML
     */
    @Override
    public String toXML() {
        StringBuilder sb = new StringBuilder("<dueno id=\"" + id + "\">");
        sb.append(camposXML()).append("<locales>");
        locales.stream().mapToInt(Local::getId).sorted()
                .forEach(ref -> sb.append("<local ref=\"").append(ref).append("\"/>"));
        return sb.append("</locales></dueno>").toString();
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
