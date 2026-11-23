package GSILabs.connect;

import GSILabs.BModel.Bar;
import GSILabs.BModel.Cliente;
import GSILabs.BModel.Local;
import GSILabs.BModel.Restaurante;
import GSILabs.BModel.Review;
import GSILabs.BSystem.BusinessSystem;

/**
 * Sistema de negocio accesible de forma remota (Practica 4, opcion b del
 * Ejercicio 1). Extiende {@link BusinessSystem}, de modo que conserva toda la
 * logica y la persistencia de las practicas anteriores, y anade la
 * implementacion de las interfaces remotas {@link LocalFinder},
 * {@link ClientGateway} y {@link AdminGateway}.
 * <p>
 * Un mismo objeto se publica en el registro RMI bajo dos identificadores (uno
 * por pasarela); cada cliente solo ve los metodos de la interfaz con la que lo
 * localiza. La clase no extiende UnicastRemoteObject: el servidor la exporta
 * con {@code UnicastRemoteObject.exportObject} (issue #5).
 * <p>
 * Los metodos se ejecutan en el servidor y no lanzan RemoteException; esa
 * excepcion la genera RMI en el lado cliente si falla la comunicacion.
 */
public class PublicBusinessSystem extends BusinessSystem implements ClientGateway, AdminGateway {

    /**
     * Crea un sistema vacio.
     */
    public PublicBusinessSystem() {
        super();
    }

    // ---------------------------------------------------------- LocalFinder
    /**
     * Devuelve el local cuyo nombre coincide exactamente con el indicado, sin
     * distinguir mayusculas ni espacios.
     *
     * @param name nombre del local
     * @return el local, o null si no existe
     */
    @Override
    public Local getLocal(String name) {
        // TODO issue #2: implementar la consulta
        return null;
    }

    /**
     * Devuelve los locales cuyo nombre contiene, total o parcialmente, el
     * texto indicado, sin distinguir mayusculas ni espacios.
     *
     * @param name nombre completo o parcial
     * @return los locales encontrados (array vacio si no hay ninguno)
     */
    @Override
    public Local[] getLocals(String name) {
        // TODO issue #2: implementar la consulta
        return null;
    }

    // -------------------------------------------------------- ClientGateway
    /**
     * Incorpora una review si su cliente y su local son correctos y no hay otra
     * del mismo cliente para el mismo local en la misma fecha.
     *
     * @param r review a introducir
     * @return true si y solo si se introdujo
     */
    @Override
    public boolean insertaReview(Review r) {
        // TODO issue #3: implementar la insercion
        return false;
    }

    /**
     * Elimina una review si existe y no tiene contestacion asociada.
     *
     * @param r review a eliminar
     * @return true si y solo si se elimino
     */
    @Override
    public boolean quitaReview(Review r) {
        // TODO issue #3: implementar el borrado (equivale a BusinessSystem.eliminaReview)
        return false;
    }

    /**
     * Devuelve el bar con mejor puntuacion media de una ciudad.
     *
     * @param ciudad nombre de la ciudad
     * @return el mejor bar, o null si la ciudad no tiene bares
     */
    @Override
    public Bar mejorBar(String ciudad) {
        // TODO issue #2: implementar la consulta
        return null;
    }

    /**
     * Devuelve los {@code num} restaurantes de una ciudad con mejor puntuacion
     * media, ordenados de mejor a peor.
     *
     * @param ciudad nombre de la ciudad
     * @param num numero de restaurantes pedidos
     * @return array de tamano {@code num}, con null en las posiciones que no
     * se puedan cubrir
     */
    @Override
    public Restaurante[] mejoresRestaurantes(String ciudad, Integer num) {
        // TODO issue #2: implementar la consulta
        return null;
    }

    // --------------------------------------------------------- AdminGateway
    /**
     * Elimina un local del sistema.
     *
     * @param l local a eliminar
     * @return true si y solo si se elimino
     */
    @Override
    public Boolean eliminaLocal(Local l) {
        // TODO issue #4: implementar el borrado
        return false;
    }

    /**
     * Elimina todas las reviews de un local y sus contestaciones.
     *
     * @param l local cuyas reviews se eliminan
     * @return true si y solo si se completo la tarea
     */
    @Override
    public Boolean eliminaReviewsDeLocal(Local l) {
        // TODO issue #4: implementar el borrado
        return false;
    }

    /**
     * Elimina una review como administrador, aunque tenga contestacion (que se
     * elimina con ella).
     * <p>
     * Sobrescribe {@link BusinessSystem#eliminaReview(Review)}, que rechaza
     * las reviews contestadas; ese comportamiento sigue disponible para los
     * clientes mediante {@link #quitaReview(Review)}.
     *
     * @param r review a eliminar
     * @return true si y solo si se elimino
     */
    @Override
    public boolean eliminaReview(Review r) {
        // TODO issue #4: implementar el borrado de administrador
        return false;
    }

    /**
     * Elimina todas las reviews de un cliente.
     *
     * @param c cliente cuyas reviews se eliminan
     * @return numero de reviews eliminadas, o -1 si el cliente no existe
     */
    @Override
    public Integer eliminaReviewsDeUsuario(Cliente c) {
        // TODO issue #4: implementar el borrado
        return -1;
    }

    /**
     * Crea un cliente falso valido y le anade una review con la puntuacion
     * indicada sobre el local dado.
     *
     * @param l local que recibe la review
     * @param puntuacion puntuacion de la review
     * @return true si y solo si se completo la tarea
     */
    @Override
    public Boolean insertaReviewFalsa(Local l, Integer puntuacion) {
        // TODO issue #3: implementar la insercion
        return false;
    }
}
