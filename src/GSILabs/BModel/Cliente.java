package GSILabs.BModel;

import GSILabs.serializable.XMLRepresentable;
import GSILabs.serializable.XMLUtils;
import java.io.File;
import java.time.LocalDate;

/**
 * Usuario que puede consultar la informacion de los locales y dejar reviews
 * (C04).
 */
public class Cliente extends Usuario implements XMLRepresentable {

    /**
     * Version de la forma serializada (la clase viaja por RMI).
     */
    private static final long serialVersionUID = 1L;

    /**
     * Crea un cliente.
     *
     * @param nick nick unico de al menos 3 caracteres
     * @param contrasena contrasena
     * @param fechaNacimiento fecha de nacimiento (minimo 14 anios)
     * @throws IllegalArgumentException si los datos no cumplen las reglas de
     * {@link Usuario}
     */
    public Cliente(String nick, String contrasena, LocalDate fechaNacimiento) {
        super(nick, contrasena, fechaNacimiento, Perfil.CLIENTE);
    }

    /**
     * Muestra por consola la informacion de un local.
     *
     * @param local local a consultar
     * @throws IllegalArgumentException si el local es nulo
     */
    public void verInformacionLocal(Local local) {
        if (local == null) {
            throw new IllegalArgumentException("El local no puede ser nulo.");
        }
        System.out.println("El cliente " + this.nick + " esta viendo la informacion de");
        System.out.println(local.obtenerInformacion());
    }

    /**
     * Crea una review de un local y la anade a este.
     *
     * @param local local visitado
     * @param valoracion estrellas, de 0 a 5
     * @param comentario comentario de hasta 500 caracteres
     * @param fechaVisita fecha de la visita, no futura
     * @return la review creada
     * @throws IllegalArgumentException si los datos no cumplen las reglas de
     * {@link Review}
     */
    public Review hacerReview(Local local, int valoracion, String comentario, LocalDate fechaVisita) {
        Review nueva = new Review(this.getId(), local.getId(), valoracion, comentario, fechaVisita);
        local.anadirReview(nueva);
        return nueva;
    }

    /**
     * Crea un cliente con un identificador concreto (reconstruccion desde XML).
     *
     * @param id identificador, positivo
     * @param nick nick unico de al menos 3 caracteres
     * @param contrasena contrasena
     * @param fechaNacimiento fecha de nacimiento (minimo 14 anios)
     * @throws IllegalArgumentException si los datos no cumplen las reglas de
     * {@link Usuario}
     */
    public Cliente(int id, String nick, String contrasena, LocalDate fechaNacimiento) {
        super(id, nick, contrasena, fechaNacimiento, Perfil.CLIENTE);
    }

    /**
     * Representacion XML del cliente. Sus reviews y reservas no se incluyen:
     * son entidades independientes que lo referencian por identificador.
     *
     * @return el cliente en XML
     */
    @Override
    public String toXML() {
        return "<cliente id=\"" + id + "\">" + camposXML() + "</cliente>";
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
