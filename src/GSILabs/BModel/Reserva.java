package GSILabs.BModel;

import GSILabs.serializable.XMLRepresentable;
import GSILabs.serializable.XMLUtils;
import java.io.File;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reserva de un Cliente en un local Reservable (Bar o Restaurante).
 */
public class Reserva implements XMLRepresentable {

    private static final AtomicInteger SIGUIENTE_ID = new AtomicInteger(1);

    private final int id;
    /**
     * Fecha y hora de la reserva.
     */
    protected LocalDateTime fecha;
    /**
     * Descuento en porcentaje, o null.
     */
    protected Double descuento;
    private Cliente cliente;
    private Reservable local;

    /**
     * @param fechaYHora Fecha y hora de la reserva.
     * @param descuentoPorcentaje Descuento entre 0 y 100, o null si no hay.
     * @param cliente Cliente que reserva.
     * @param local Local reservado.
     * @throws IllegalArgumentException si la fecha, el cliente o el local son
     * nulos, o el descuento esta fuera de rango.
     */
    public Reserva(LocalDateTime fechaYHora, Double descuentoPorcentaje, Cliente cliente, Reservable local) {
        this(SIGUIENTE_ID.get(), fechaYHora, descuentoPorcentaje, cliente, local);
    }

    /**
     * Crea una reserva con un identificador concreto (reconstruccion desde
     * XML).
     *
     * @param id identificador, positivo
     * @param fechaYHora fecha y hora de la reserva
     * @param descuentoPorcentaje descuento entre 0 y 100, o null
     * @param cliente cliente que reserva
     * @param local local reservado
     * @throws IllegalArgumentException si algun dato no es valido
     */
    public Reserva(int id, LocalDateTime fechaYHora, Double descuentoPorcentaje, Cliente cliente, Reservable local) {
        if (id <= 0) {
            throw new IllegalArgumentException("El identificador debe ser positivo.");
        }
        if (fechaYHora == null || cliente == null || local == null) {
            throw new IllegalArgumentException("La fecha, el cliente y el local no pueden ser nulos.");
        }
        if (descuentoPorcentaje != null && (descuentoPorcentaje < 0.0 || descuentoPorcentaje > 100.0)) {
            throw new IllegalArgumentException("El descuento debe estar entre 0 y 100");
        }
        SIGUIENTE_ID.accumulateAndGet(id + 1, Math::max);
        this.id = id;
        this.fecha = fechaYHora;
        this.descuento = descuentoPorcentaje;
        this.cliente = cliente;
        this.local = local;
    }

    /**
     * @return el identificador unico de la reserva
     */
    public int getId() {
        return id;
    }

    /**
     * @return fecha y hora de la reserva
     */
    public LocalDateTime getFecha() {
        return fecha;
    }

    /**
     * @return el descuento en porcentaje, o null si no hay
     */
    public Double getDescuento() {
        return descuento;
    }

    /**
     * @return el cliente que reserva
     */
    public Cliente getCliente() {
        return cliente;
    }

    /**
     * @return el local reservado
     */
    public Reservable getLocal() {
        return local;
    }

    /**
     * @param obj objeto a comparar
     * @return cierto si es una reserva del mismo cliente, local y fecha/hora
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Reserva o = (Reserva) obj;
        return Objects.equals(fecha, o.fecha) && Objects.equals(cliente, o.cliente)
                && Objects.equals(local, o.local);
    }

    /**
     * @return hash coherente con {@link #equals(Object)}
     */
    @Override
    public int hashCode() {
        return Objects.hash(fecha, cliente, local);
    }

    /**
     * @return descripcion de la reserva
     */
    @Override
    public String toString() {
        return "Reserva{id=" + id + ", cliente=" + (cliente == null ? null : cliente.getNick()) + ", local=" + local
                + ", fecha=" + fecha + ", descuento=" + descuento + "}";
    }

    /**
     * Representacion XML de la reserva. El cliente y el local se referencian
     * por su identificador.
     *
     * @return la reserva en XML
     */
    @Override
    public String toXML() {
        return "<reserva id=\"" + id + "\" cliente=\"" + cliente.getId()
                + "\" local=\"" + local.getId() + "\">"
                + XMLUtils.elemento("fecha", fecha)
                + XMLUtils.elemento("descuento", descuento)
                + "</reserva>";
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
