package GSILabs.BTesting.P01;

import GSILabs.BModel.Bar;
import GSILabs.BModel.Cliente;
import GSILabs.BModel.Contestacion;
import GSILabs.BModel.Direccion;
import GSILabs.BModel.Dueno;
import GSILabs.BModel.Restaurante;
import GSILabs.BModel.Review;
import GSILabs.BModel.Usuario;
import GSILabs.BSystem.BusinessSystem;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Clase ejecutable del Ejercicio 4. Crea una instancia de
 * {@link BusinessSystem}, la puebla con datos de prueba y comprueba, uno a uno,
 * los diez sucesos (S1-S10) pedidos en el guion de la Practica 01, mostrando el
 * resultado de cada comprobacion por linea de comandos.
 */
public class Tester {

    /**
     * Numero de comprobaciones que han dado el resultado esperado.
     */
    private static int aciertos = 0;
    /**
     * Numero de comprobaciones que no han dado el resultado esperado.
     */
    private static int fallos = 0;

    /**
     * Clase de utilidad: no se instancia.
     */
    private Tester() {
    }

    /**
     * Puebla un {@link BusinessSystem} con datos de prueba y ejecuta las
     * comprobaciones de los sucesos S1-S10.
     *
     * @param args no se usa
     */
    public static void main(String[] args) {
        BusinessSystem sistema = new BusinessSystem();

        Cliente ana = new Cliente("ana", "clave123", LocalDate.of(2000, 5, 10));
        Cliente bea = new Cliente("bea", "clave456", LocalDate.of(1995, 3, 20));
        Dueno duenio1 = new Dueno("duenio1", "p1", LocalDate.of(1980, 1, 1));
        Dueno duenio2 = new Dueno("duenio2", "p2", LocalDate.of(1981, 1, 1));
        Dueno duenio3 = new Dueno("duenio3", "p3", LocalDate.of(1982, 1, 1));
        Dueno duenio4 = new Dueno("duenio4", "p4", LocalDate.of(1983, 1, 1));
        System.out.println("Alta de usuarios: " + sistema.nuevoUsuario(ana) + " " + sistema.nuevoUsuario(bea)
                + " " + sistema.nuevoUsuario(duenio1) + " " + sistema.nuevoUsuario(duenio2)
                + " " + sistema.nuevoUsuario(duenio3) + " " + sistema.nuevoUsuario(duenio4));

        Direccion direccionBar = new Direccion("Pamplona", "Navarra", "Mayor", 10);
        Bar bar1 = new Bar("El Rincon", direccionBar);
        Restaurante restaurante1 = new Restaurante(15.0, 4, 40, "Restaurante Central",
                new Direccion("Tudela", "Navarra", "Ribera", 5));
        System.out.println("Alta de locales: " + sistema.nuevoLocal(bar1) + " " + sistema.nuevoLocal(restaurante1));

        System.out.println("Asociacion inicial de propietario: " + sistema.asociarLocal(bar1, duenio1));

        Review review1 = new Review(ana.getId(), bar1.getId(), 4, "Buen ambiente", LocalDate.now().minusDays(2));
        System.out.println("Alta de review: " + sistema.nuevaReview(review1));
        Contestacion contestacion1 = new Contestacion("Gracias por la visita", review1, duenio1);
        System.out.println("Alta de contestacion: " + sistema.nuevaContestacion(contestacion1, review1));
        System.out.println("Alta de reserva: "
                + sistema.nuevaReserva(bea, bar1, LocalDate.now().plusDays(3), LocalTime.of(21, 0)));

        comprobarS1(sistema, ana);
        comprobarS2(sistema);
        comprobarS3(sistema, direccionBar);

        Bar barNuevo = comprobarS4(sistema, bar1, direccionBar);

        comprobarS5();
        comprobarS6(sistema, ana);
        comprobarS7(sistema, ana, direccionBar);

        sistema.asociarLocal(barNuevo, duenio1);

        comprobarS8(sistema, ana, barNuevo, duenio1);
        comprobarS9(sistema, barNuevo, duenio1, duenio2, duenio3, duenio4);
        comprobarS10(sistema, bea, barNuevo);

        System.out.println();
        System.out.println("RESUMEN: " + aciertos + " correctos, " + fallos + " fallidos (de " + (aciertos + fallos) + ")");
    }

    /**
     * Imprime el resultado de una comprobacion y actualiza los contadores
     * globales de aciertos y fallos.
     *
     * @param codigo suceso comprobado (S1, S2, ...)
     * @param cumplida true si el sistema se comporto como se esperaba
     * @param descripcion breve explicacion de lo comprobado
     */
    private static void informar(String codigo, boolean cumplida, String descripcion) {
        if (cumplida) {
            aciertos++;
            System.out.println("[OK]    " + codigo + " - " + descripcion);
        } else {
            fallos++;
            System.out.println("[FALLO] " + codigo + " - " + descripcion);
        }
    }

    /**
     * S1) Si introduce a un usuario, este puede ser luego localizado a partir
     * de su ID.
     *
     * @param sistema sistema ya poblado con el usuario a buscar
     * @param ana cliente dado de alta previamente, con el que se compara el
     * resultado
     */
    private static void comprobarS1(BusinessSystem sistema, Cliente ana) {
        Usuario encontrado = sistema.obtenerUsuario(ana.getNick());
        boolean cumplida = encontrado != null && encontrado.getId() == ana.getId();
        informar("S1", cumplida, "el usuario dado de alta se localiza y conserva su ID (" + ana.getId() + ")");
    }

    /**
     * S2) Si busca a un usuario que no existe con ObtenerUsuario, el resultado
     * es null.
     *
     * @param sistema sistema sobre el que se hace la busqueda
     */
    private static void comprobarS2(BusinessSystem sistema) {
        Usuario resultado = sistema.obtenerUsuario("nickQueNoExiste");
        informar("S2", resultado == null, "obtenerUsuario con un nick inexistente devuelve null");
    }

    /**
     * S3) No se pueden introducir Locales en la misma direccion.
     *
     * @param sistema sistema con un local ya dado de alta en direccionBar
     * @param direccionBar direccion ya ocupada por un local existente
     */
    private static void comprobarS3(BusinessSystem sistema, Direccion direccionBar) {
        Bar barDuplicado = new Bar("Bar Duplicado", direccionBar);
        boolean rechazado = !sistema.nuevoLocal(barDuplicado);
        informar("S3", rechazado, "no se admite un segundo local en una direccion ya ocupada");
    }

    /**
     * S4) Si se anade un local, y se elimina posteriormente, se puede
     * introducir un bar en la misma direccion.
     *
     * @param sistema sistema sobre el que se elimina y se vuelve a dar de alta
     * @param barViejo local que se va a eliminar
     * @param direccionBar direccion del local eliminado, que debe quedar libre
     * @return el bar nuevo dado de alta en la direccion liberada, para que las
     * comprobaciones siguientes puedan seguir usando un local real
     */
    private static Bar comprobarS4(BusinessSystem sistema, Bar barViejo, Direccion direccionBar) {
        boolean eliminado = sistema.eliminarLocal(barViejo);
        Bar barNuevo = new Bar("El Rincon (nuevo)", direccionBar);
        boolean readmitido = sistema.nuevoLocal(barNuevo);
        informar("S4", eliminado && readmitido,
                "tras eliminar un local, su direccion queda libre para un bar nuevo");
        return barNuevo;
    }

    /**
     * S5) No se puede introducir un usuario menor de edad.
     */
    private static void comprobarS5() {
        boolean rechazado;
        try {
            new Cliente("peque", "clave", LocalDate.now().minusYears(10));
            rechazado = false;
        } catch (IllegalArgumentException e) {
            rechazado = true;
        }
        informar("S5", rechazado, "un usuario de 10 años es rechazado con IllegalArgumentException");
    }

    /**
     * S6) No se pueden hacer reservas para un local inexistente.
     *
     * @param sistema sistema sobre el que se intenta la reserva
     * @param ana cliente que intenta reservar
     */
    private static void comprobarS6(BusinessSystem sistema, Cliente ana) {
        Bar barFantasma = new Bar("Bar Fantasma", new Direccion("Tudela", "Navarra", "Inventada", 99));
        boolean rechazada = !sistema.nuevaReserva(ana, barFantasma, LocalDate.now().plusDays(1), LocalTime.of(20, 0));
        informar("S6", rechazada, "no se puede reservar en un local que nunca se dio de alta");
    }

    /**
     * S7) No se pueden hacer reservas para un local inexistente, aunque este en
     * la misma direccion que otro existente.
     *
     * @param sistema sistema con un local real ya dado de alta en direccionBar
     * @param ana cliente que intenta reservar
     * @param direccionBar direccion del local real, reutilizada por el local
     * falso
     */
    private static void comprobarS7(BusinessSystem sistema, Cliente ana, Direccion direccionBar) {
        Restaurante restauranteFalso = new Restaurante(20.0, 4, 50, "Restaurante Impostor", direccionBar);
        boolean rechazada = !sistema.nuevaReserva(ana, restauranteFalso, LocalDate.now().plusDays(1), LocalTime.of(20, 0));
        informar("S7", rechazada, "no basta con compartir direccion con un local existente para poder reservar");
    }

    /**
     * S8) No se pueden añadir comentarios para Reviews que no existen.
     *
     * @param sistema sistema sobre el que se intenta la contestacion
     * @param ana cliente autor de la review suelta
     * @param barNuevo local sobre el que trata la review suelta
     * @param duenio1 propietario del local, autor de la contestacion
     */
    private static void comprobarS8(BusinessSystem sistema, Cliente ana, Bar barNuevo, Dueno duenio1) {
        Review reviewSuelta = new Review(ana.getId(), barNuevo.getId(), 3, "Comentario suelto", LocalDate.now().minusDays(5));
        Contestacion contestacionSuelta = new Contestacion("Respuesta", reviewSuelta, duenio1);
        boolean rechazada = !sistema.nuevaContestacion(contestacionSuelta, reviewSuelta);
        informar("S8", rechazada, "no se puede contestar una review que el sistema no tiene registrada");
    }

    /**
     * S9) No se pueden añadir cuatro dueños a un bar.
     *
     * @param sistema sistema sobre el que se asocian los propietarios
     * @param barNuevo local al que se asocian
     * @param d1 propietario ya asociado antes de esta comprobacion
     * @param d2 segundo propietario a asociar
     * @param d3 tercer propietario a asociar
     * @param d4 cuarto propietario, que debe ser rechazado
     */
    private static void comprobarS9(BusinessSystem sistema, Bar barNuevo, Dueno d1, Dueno d2, Dueno d3, Dueno d4) {
        boolean segundo = sistema.asociarLocal(barNuevo, d2);
        boolean tercero = sistema.asociarLocal(barNuevo, d3);
        boolean cuarto = sistema.asociarLocal(barNuevo, d4);
        informar("S9", segundo && tercero && !cuarto,
                "se admiten hasta 3 propietarios por bar, pero no un cuarto");
    }

    /**
     * S10) No se pueden añadir dos Reviews del mismo usuario, el mismo dia para
     * el mismo local.
     *
     * @param sistema sistema sobre el que se dan de alta las reviews
     * @param bea cliente autor de ambas reviews
     * @param barNuevo local valorado en ambas reviews
     */
    private static void comprobarS10(BusinessSystem sistema, Cliente bea, Bar barNuevo) {
        LocalDate fechaVisita = LocalDate.now().minusDays(1);
        Review primeraReview = new Review(bea.getId(), barNuevo.getId(), 5, "Genial", fechaVisita);
        boolean primeraAceptada = sistema.nuevaReview(primeraReview);

        Review segundaReview = new Review(bea.getId(), barNuevo.getId(), 2, "Cambio de opinion", fechaVisita);
        boolean segundaRechazada = !sistema.nuevaReview(segundaReview);

        informar("S10", primeraAceptada && segundaRechazada,
                "un mismo cliente no puede valorar dos veces el mismo local el mismo dia");
    }
}
