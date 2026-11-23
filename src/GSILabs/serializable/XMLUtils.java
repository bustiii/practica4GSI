package GSILabs.serializable;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * Utilidades comunes para generar representaciones XML de los objetos del
 * sistema. Centraliza el escapado de caracteres para que todas las clases
 * generen XML bien formado.
 */
public final class XMLUtils {

    private XMLUtils() {
    }

    /**
     * Escapa los caracteres reservados de XML.
     *
     * @param texto texto a escapar (puede ser nulo)
     * @return el texto escapado, o cadena vacia si era nulo
     */
    public static String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(texto.length());
        for (char c : texto.toCharArray()) {
            switch (c) {
                case '<' ->
                    sb.append("&lt;");
                case '>' ->
                    sb.append("&gt;");
                case '&' ->
                    sb.append("&amp;");
                case '"' ->
                    sb.append("&quot;");
                case '\'' ->
                    sb.append("&apos;");
                default ->
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Construye un elemento simple con contenido textual.
     *
     * @param etiqueta nombre del elemento
     * @param valor valor del elemento (se escapa); si es nulo no se genera el
     * elemento
     * @return el elemento XML, o cadena vacia si el valor es nulo
     */
    public static String elemento(String etiqueta, Object valor) {
        if (valor == null) {
            return "";
        }
        return "<" + etiqueta + ">" + escapar(valor.toString()) + "</" + etiqueta + ">";
    }

    /**
     * Guarda un XML en un fichero, en UTF-8, con declaracion e indentado para
     * que sea legible y editable a mano.
     *
     * @param xml XML bien formado a guardar
     * @param f fichero de destino
     * @return cierto si se ha guardado correctamente
     */
    public static boolean guardar(String xml, File f) {
        try {
            Files.writeString(f.toPath(), formatear(xml), StandardCharsets.UTF_8);
            return true;
        } catch (IOException | TransformerException e) {
            System.err.println("Error al guardar el XML en " + f + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Indenta un XML bien formado y le anade la declaracion XML.
     *
     * @param xml XML bien formado
     * @return el mismo XML, indentado
     * @throws TransformerException si el XML no esta bien formado
     */
    public static String formatear(String xml) throws TransformerException {
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        StringWriter out = new StringWriter();
        t.transform(new StreamSource(new StringReader(xml)), new StreamResult(out));
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + out;
    }
}
