package GSILabs.BModel;

import GSILabs.serializable.XMLRepresentable;
import GSILabs.serializable.XMLUtils;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Usuario del portal (C03). Puede ser {@link Cliente} o {@link Dueño}. El nick
 * es unico en el sistema, por lo que dos usuarios con el mismo nick se
 * consideran el mismo. Ademas cada instancia recibe un identificador numerico
 * unico generado automaticamente.
 *
 *
 */
public class Usuario implements XMLRepresentable, Serializable {

    /**
     * Version de la forma serializada (la clase viaja por RMI).
     */
    private static final long serialVersionUID = 1L;

    /**
     * Representacion XML del usuario. Contiene sus datos propios; no incluye
     * objetos relacionados.
     *
     * @return el usuario en XML
     */
    @Override
    public String toXML() {
        return "<usuario id=\"" + id + "\" perfil=\"" + perfil + "\">" + camposXML() + "</usuario>";
    }

    /**
     * @return los campos comunes a todo usuario en formato XML, para que las
     * subclases los reutilicen
     */
    protected String camposXML() {
        return XMLUtils.elemento("nick", nick)
                + XMLUtils.elemento("contrasena", contrasena)
                + XMLUtils.elemento("fechaNacimiento", fechaNacimiento);
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

    /**
     * Perfil que determina lo que puede hacer el usuario.
     */
    public enum Perfil {
        /**
         * Usuario que posee locales.
         */
        PROPIETARIO,
        /**
         * Usuario que consulta locales y escribe reviews.
         */
        CLIENTE
    }

    /**
     * Longitud minima del nick.
     */
    public static final int NICK_MIN = 3;
    /**
     * Edad minima para registrarse.
     */
    public static final int EDAD_MIN = 14;

    private static final AtomicInteger SIGUIENTE_ID = new AtomicInteger(1);

    /**
     * Identificador unico e inmutable.
     */
    protected final int id;
    /**
     * Nick unico del usuario.
     */
    protected String nick;
    /**
     * Contrasena del usuario.
     */
    protected String contrasena;
    /**
     * Fecha de nacimiento.
     */
    protected LocalDate fechaNacimiento;
    /**
     * Perfil del usuario.
     */
    protected Perfil perfil;

    /**
     * Crea un usuario validando las reglas de negocio.
     *
     * @param nick nick, de al menos {@value #NICK_MIN} caracteres
     * @param contrasena contrasena, no vacia
     * @param fechaNacimiento fecha de nacimiento, que implique al menos
     * {@value #EDAD_MIN} anios
     * @param perfil perfil del usuario
     * @throws IllegalArgumentException si algun dato es nulo o vacio, el nick
     * es demasiado corto o el usuario es menor de {@value #EDAD_MIN} anios
     */
    public Usuario(String nick, String contrasena, LocalDate fechaNacimiento, Perfil perfil) {
        this(SIGUIENTE_ID.get(), nick, contrasena, fechaNacimiento, perfil);
    }

    /**
     * Crea un usuario con un identificador concreto. Se usa al reconstruir
     * usuarios desde su representacion XML, para conservar las referencias por
     * identificador. El generador de identificadores se adelanta para no
     * repetir nunca el identificador dado.
     *
     * @param id identificador, positivo
     * @param nick nick, de al menos {@value #NICK_MIN} caracteres
     * @param contrasena contrasena, no vacia
     * @param fechaNacimiento fecha de nacimiento
     * @param perfil perfil del usuario
     * @throws IllegalArgumentException si el identificador no es positivo o
     * algun dato no cumple las reglas de negocio
     */
    protected Usuario(int id, String nick, String contrasena, LocalDate fechaNacimiento, Perfil perfil) {
        if (id <= 0) {
            throw new IllegalArgumentException("El identificador debe ser positivo.");
        }
        if (nick == null || nick.trim().length() < NICK_MIN) {
            throw new IllegalArgumentException("El nick debe tener al menos " + NICK_MIN + " caracteres.");
        }
        if (contrasena == null || contrasena.isEmpty()) {
            throw new IllegalArgumentException("La contrasena no puede estar vacia.");
        }
        if (fechaNacimiento == null) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser nula.");
        }
        if (perfil == null) {
            throw new IllegalArgumentException("El perfil no puede ser nulo.");
        }
        int edad = Period.between(fechaNacimiento, LocalDate.now()).getYears();
        if (edad < EDAD_MIN) {
            throw new IllegalArgumentException("El usuario " + nick + " tiene " + edad
                    + " anios; la edad minima es " + EDAD_MIN + ".");
        }
        SIGUIENTE_ID.accumulateAndGet(id + 1, Math::max);
        this.id = id;
        this.nick = nick;
        this.contrasena = contrasena;
        this.fechaNacimiento = fechaNacimiento;
        this.perfil = perfil;
    }

    /**
     * @return el identificador unico del usuario
     */
    public int getId() {
        return id;
    }

    /**
     * @return el nick del usuario
     */
    public String getNick() {
        return nick;
    }

    /**
     * @return el perfil del usuario
     */
    public Perfil getPerfil() {
        return perfil;
    }

    /**
     * @return la fecha de nacimiento
     */
    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    /**
     * @return la edad actual en anios
     */
    public int getEdad() {
        return getEdad(LocalDate.now());
    }

    /**
     * Calcula la edad del usuario en una fecha dada.
     *
     * @param en fecha de referencia
     * @return la edad en anios en esa fecha
     */
    public int getEdad(LocalDate en) {
        return Period.between(fechaNacimiento, en).getYears();
    }

    /**
     * Dos usuarios son el mismo si comparten nick.
     *
     * @param obj objeto a comparar
     * @return cierto si es un usuario con el mismo nick
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Usuario)) {
            return false;
        }
        return Objects.equals(nick, ((Usuario) obj).nick);
    }

    /**
     * @return hash coherente con {@link #equals(Object)} (basado en el nick)
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(nick);
    }

    /**
     * @return descripcion con perfil, id y nick
     */
    @Override
    public String toString() {
        return perfil + "{id=" + id + ", nick='" + nick + "'}";
    }

    /**
     * Reconstruye el objeto al deserializarlo (por ejemplo, al recibirlo por
     * RMI). La deserializacion no pasa por los constructores, asi que aqui se
     * adelanta el generador de identificadores para que los objetos que se
     * creen despues en esta maquina no repitan el identificador de este
     * usuario.
     *
     * @param in flujo del que se lee el objeto
     * @throws IOException si falla la lectura
     * @throws ClassNotFoundException si no se encuentra alguna clase
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        SIGUIENTE_ID.accumulateAndGet(id + 1, Math::max);
    }
}
