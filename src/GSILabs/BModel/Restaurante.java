package GSILabs.BModel;

import GSILabs.serializable.XMLRepresentable;
import GSILabs.serializable.XMLUtils;

/**
 * Restaurante (C02): local reservable con precio estimado de menu y capacidad
 * maxima total y por mesa.
 *
 *
 */
public class Restaurante extends Local implements Reservable, XMLRepresentable {

    private double precioMenu;
    private int capacidadMesa;
    private int capacidadTotal;

    /**
     * Crea un restaurante.
     *
     * @param precioMenu precio estimado del menu, no negativo
     * @param capacidadMesa comensales por mesa, positivo
     * @param capacidadTotal comensales totales, no menor que la capacidad por
     * mesa
     * @param nombre nombre, no vacio
     * @param ubicacion direccion, no nula
     * @throws IllegalArgumentException si algun dato no es valido
     */
    public Restaurante(double precioMenu, int capacidadMesa, int capacidadTotal, String nombre, Direccion ubicacion) {
        super(nombre, ubicacion);
        validarCapacidades(capacidadMesa, capacidadTotal);
        setPrecioMenu(precioMenu);
        this.capacidadMesa = capacidadMesa;
        this.capacidadTotal = capacidadTotal;
    }

    /**
     * Crea un restaurante con un identificador concreto (reconstruccion desde
     * XML).
     *
     * @param id identificador, positivo
     * @param precioMenu precio del menu, no negativo
     * @param capacidadMesa comensales por mesa, positivo
     * @param capacidadTotal capacidad total, no menor que la de una mesa
     * @param nombre nombre del restaurante
     * @param ubicacion direccion del restaurante
     * @throws IllegalArgumentException si algun dato no es valido
     */
    public Restaurante(int id, double precioMenu, int capacidadMesa, int capacidadTotal, String nombre, Direccion ubicacion) {
        super(id, nombre, ubicacion);
        validarCapacidades(capacidadMesa, capacidadTotal);
        setPrecioMenu(precioMenu);
        this.capacidadMesa = capacidadMesa;
        this.capacidadTotal = capacidadTotal;
    }

    /**
     * @return el restaurante en XML, con su menu y capacidades
     */
    @Override
    public String toXML() {
        return "<restaurante id=\"" + id + "\">" + camposXML()
                + XMLUtils.elemento("precioMenu", precioMenu)
                + XMLUtils.elemento("capacidadMesa", capacidadMesa)
                + XMLUtils.elemento("capacidadTotal", capacidadTotal)
                + "</restaurante>";
    }

    private static void validarCapacidades(int mesa, int total) {
        if (mesa <= 0 || total < mesa) {
            throw new IllegalArgumentException("Capacidades invalidas: por mesa debe ser positiva y no mayor que la total.");
        }
    }

    /**
     * @return el precio estimado del menu
     */
    public double getPrecioMenu() {
        return precioMenu;
    }

    /**
     * @param precioMenu nuevo precio, no negativo
     * @throws IllegalArgumentException si es negativo
     */
    public void setPrecioMenu(double precioMenu) {
        if (precioMenu < 0) {
            throw new IllegalArgumentException("El precio del menu no puede ser negativo.");
        }
        this.precioMenu = precioMenu;
    }

    /**
     * @return comensales por mesa
     */
    public int getCapacidadMesa() {
        return capacidadMesa;
    }

    /**
     * @param capacidadMesa nueva capacidad por mesa, positiva y no mayor que la
     * total
     * @throws IllegalArgumentException si no es valida
     */
    public void setCapacidadMesa(int capacidadMesa) {
        validarCapacidades(capacidadMesa, capacidadTotal);
        this.capacidadMesa = capacidadMesa;
    }

    /**
     * @return comensales totales
     */
    public int getCapacidadTotal() {
        return capacidadTotal;
    }

    /**
     * @param capacidadTotal nueva capacidad total, no menor que la de mesa
     * @throws IllegalArgumentException si no es valida
     */
    public void setCapacidadTotal(int capacidadTotal) {
        validarCapacidades(capacidadMesa, capacidadTotal);
        this.capacidadTotal = capacidadTotal;
    }

    /**
     * @return nombre, direccion, menu y capacidad del restaurante
     */
    @Override
    public String toString() {
        return "Restaurante: " + nombre + " | " + ubicacion + " | Menu: " + precioMenu
                + " euros | Capacidad: " + capacidadTotal + " (mesa: " + capacidadMesa + ")";
    }

    /**
     * @param obj objeto a comparar
     * @return cierto si es un restaurante en la misma direccion con los mismos
     * datos
     */
    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        Restaurante otro = (Restaurante) obj;
        return Double.compare(otro.precioMenu, this.precioMenu) == 0
                && this.capacidadMesa == otro.capacidadMesa
                && this.capacidadTotal == otro.capacidadTotal;
    }

    /**
     * @return hash basado solo en la direccion (el resto de campos son
     * mutables)
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
