package GSILabs.BModel;

/**
 * Local en el que se pueden hacer reservas (C09): Bares y Restaurantes.
 */
public interface Reservable {

    /**
     * @return el nombre del local reservable
     */
    String getNombre();

    /**
     * @return la direccion del local reservable
     */
    Direccion getUbicacion();

    /**
     * @return el identificador unico del local reservable
     */
    int getId();
}
