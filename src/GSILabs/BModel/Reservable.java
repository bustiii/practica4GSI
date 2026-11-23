package GSILabs.BModel;

import java.io.Serializable;

/**
 * Local en el que se pueden hacer reservas (C09): Bares y Restaurantes.
 * <p>
 * Es Serializable porque una {@link Reserva} guarda su local como
 * Reservable y las reservas pueden viajar por RMI.
 */
public interface Reservable extends Serializable {

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
