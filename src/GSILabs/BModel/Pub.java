package GSILabs.BModel;

import GSILabs.serializable.XMLRepresentable;
import GSILabs.serializable.XMLUtils;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Pub (C02): local con hora de apertura y de clausura.
 *
 *
 */
public class Pub extends Local implements XMLRepresentable {

    private LocalTime horaClausura;
    private LocalTime horaApertura;

    /**
     * Crea un pub.
     *
     * @param nombre nombre, no vacio
     * @param ubicacion direccion, no nula
     * @param horaClausura hora de cierre, no nula
     * @param horaApertura hora de apertura, no nula
     * @throws IllegalArgumentException si algun dato no es valido
     */
    public Pub(String nombre, Direccion ubicacion, LocalTime horaClausura, LocalTime horaApertura) {
        super(nombre, ubicacion);
        setHoraClausura(horaClausura);
        setHoraApertura(horaApertura);
    }

    /**
     * @return la hora de clausura
     */
    /**
     * Crea un pub con un identificador concreto (reconstruccion desde XML).
     *
     * @param id identificador, positivo
     * @param nombre nombre del pub
     * @param ubicacion direccion del pub
     * @param horaClausura hora de cierre
     * @param horaApertura hora de apertura
     * @throws IllegalArgumentException si algun dato no es valido
     */
    public Pub(int id, String nombre, Direccion ubicacion, LocalTime horaClausura, LocalTime horaApertura) {
        super(id, nombre, ubicacion);
        setHoraClausura(horaClausura);
        setHoraApertura(horaApertura);
    }

    /**
     * @return el pub en XML, con su horario
     */
    @Override
    public String toXML() {
        return "<pub id=\"" + id + "\">" + camposXML()
                + XMLUtils.elemento("horaApertura", horaApertura)
                + XMLUtils.elemento("horaClausura", horaClausura)
                + "</pub>";
    }

    public LocalTime getHoraClausura() {
        return horaClausura;
    }

    /**
     * @param horaClausura nueva hora de cierre, no nula
     * @throws IllegalArgumentException si es nula
     */
    public void setHoraClausura(LocalTime horaClausura) {
        if (horaClausura == null) {
            throw new IllegalArgumentException("La hora de clausura no puede ser nula.");
        }
        this.horaClausura = horaClausura;
    }

    /**
     * @return la hora de apertura
     */
    public LocalTime getHoraApertura() {
        return horaApertura;
    }

    /**
     * @param horaApertura nueva hora de apertura, no nula
     * @throws IllegalArgumentException si es nula
     */
    public void setHoraApertura(LocalTime horaApertura) {
        if (horaApertura == null) {
            throw new IllegalArgumentException("La hora de apertura no puede ser nula.");
        }
        this.horaApertura = horaApertura;
    }

    /**
     * @return nombre, direccion y horario del pub
     */
    @Override
    public String toString() {
        return "Pub: " + nombre + " | " + ubicacion + " | Horario: " + horaApertura + "-" + horaClausura;
    }

    /**
     * @param obj objeto a comparar
     * @return cierto si es un pub en la misma direccion con el mismo horario
     */
    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        Pub otro = (Pub) obj;
        return Objects.equals(this.horaApertura, otro.horaApertura)
                && Objects.equals(this.horaClausura, otro.horaClausura);
    }

    /**
     * @return hash basado solo en la direccion (el horario es mutable)
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
