package GSILabs.persistence;

/**
 * Excepcion lanzada cuando una representacion XML no es adecuada para crear el
 * objeto pedido: XML mal formado, elementos o atributos obligatorios ausentes,
 * valores con formato incorrecto, datos que violan las reglas de negocio o
 * referencias a objetos inexistentes.
 */
public class XMLParsingException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepcion con un mensaje descriptivo.
     *
     * @param mensaje descripcion del problema encontrado
     */
    public XMLParsingException(String mensaje) {
        super(mensaje);
    }

    /**
     * Crea la excepcion con un mensaje y la causa original.
     *
     * @param mensaje descripcion del problema encontrado
     * @param causa excepcion que origino el problema
     */
    public XMLParsingException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
