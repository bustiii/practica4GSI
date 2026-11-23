package GSILabs.BModel;

import GSILabs.serializable.XMLRepresentable;
import GSILabs.serializable.XMLUtils;
import java.io.File;
import java.util.Objects;

/**
 * Localizacion concreta de un local: localidad, provincia, calle y numero
 * (C01). Dos direcciones son iguales si coinciden todos sus campos.
 *
 *
 */
public class Direccion implements XMLRepresentable {

    private String localidad;
    private String provincia;
    private String calle;
    private int numero;

    /**
     * Crea una direccion.
     *
     * @param localidad localidad, no vacia
     * @param provincia provincia, no vacia
     * @param calle calle, no vacia
     * @param numero numero de portal, positivo
     * @throws IllegalArgumentException si algun texto es nulo o vacio, o el
     * numero no es positivo
     */
    public Direccion(String localidad, String provincia, String calle, int numero) {
        setLocalidad(localidad);
        setProvincia(provincia);
        setCalle(calle);
        setNumero(numero);
    }

    private static String texto(String valor, String campo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("La " + campo + " no puede ser nula ni vacia.");
        }
        return valor;
    }

    /**
     * @return la localidad
     */
    public String getLocalidad() {
        return localidad;
    }

    /**
     * @param localidad nueva localidad, no vacia
     * @throws IllegalArgumentException si es nula o vacia
     */
    public void setLocalidad(String localidad) {
        this.localidad = texto(localidad, "localidad");
    }

    /**
     * @return la provincia
     */
    public String getProvincia() {
        return provincia;
    }

    /**
     * @param provincia nueva provincia, no vacia
     * @throws IllegalArgumentException si es nula o vacia
     */
    public void setProvincia(String provincia) {
        this.provincia = texto(provincia, "provincia");
    }

    /**
     * @return la calle
     */
    public String getCalle() {
        return calle;
    }

    /**
     * @param calle nueva calle, no vacia
     * @throws IllegalArgumentException si es nula o vacia
     */
    public void setCalle(String calle) {
        this.calle = texto(calle, "calle");
    }

    /**
     * @return el numero de portal
     */
    public int getNumero() {
        return numero;
    }

    /**
     * @param numero nuevo numero, positivo
     * @throws IllegalArgumentException si no es positivo
     */
    public void setNumero(int numero) {
        if (numero <= 0) {
            throw new IllegalArgumentException("El numero debe ser positivo.");
        }
        this.numero = numero;
    }

    /**
     * @param obj objeto a comparar
     * @return cierto si es una direccion con los mismos cuatro campos
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Direccion otra = (Direccion) obj;
        return this.numero == otra.numero
                && Objects.equals(this.localidad, otra.localidad)
                && Objects.equals(this.provincia, otra.provincia)
                && Objects.equals(this.calle, otra.calle);
    }

    /**
     * @return hash coherente con {@link #equals(Object)}
     */
    @Override
    public int hashCode() {
        return Objects.hash(localidad, provincia, calle, numero);
    }

    /**
     * @return la direccion como "calle numero, localidad (provincia)"
     */
    @Override
    public String toString() {
        return calle + " " + numero + ", " + localidad + " (" + provincia + ")";
    }

    /**
     * Representacion XML de la direccion. Forma parte del local, por lo que se
     * incrusta dentro de este.
     *
     * @return la direccion en XML
     */
    @Override
    public String toXML() {
        return "<direccion>"
                + XMLUtils.elemento("localidad", localidad)
                + XMLUtils.elemento("provincia", provincia)
                + XMLUtils.elemento("calle", calle)
                + XMLUtils.elemento("numero", numero)
                + "</direccion>";
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
